import Foundation
import Speech
import AVFoundation
import NurayaCore
@MainActor final class SpeechService:ObservableObject {
    @Published var text="";@Published var recording=false;@Published var error:String?
    private var generation=0
    private var engine:AVAudioEngine?,request:SFSpeechAudioBufferRecognitionRequest?,task:SFSpeechRecognitionTask?,timeout:Task<Void,Never>?
    func start(locale:String="id-ID",allowNetwork:Bool=false) async {
        stop();let token=generation;text="";error=nil
        let authorized=await withCheckedContinuation {c in SFSpeechRecognizer.requestAuthorization {c.resume(returning:$0 == .authorized)}}
        let microphone=await withCheckedContinuation {c in AVAudioSession.sharedInstance().requestRecordPermission {c.resume(returning:$0)}}
        guard generation==token else{return}
        guard authorized,microphone else {error="Izinkan mikrofon dan pengenalan ucapan melalui Pengaturan iPhone.";return}
        guard let recognizer=SFSpeechRecognizer(locale:Locale(identifier:locale)),recognizer.isAvailable else {error="Pengenal ucapan bahasa ini belum tersedia di iPhone.";return}
        guard recognizer.supportsOnDeviceRecognition || allowNetwork else {error="Model dikte lokal belum tersedia. Aktifkan izin layanan suara Apple atau gunakan ketikan.";return}
        do {
            let session=AVAudioSession.sharedInstance();try session.setCategory(.record,mode:.measurement,options:.duckOthers);try session.setActive(true)
            let e=AVAudioEngine(),r=SFSpeechAudioBufferRecognitionRequest();r.shouldReportPartialResults=true;r.requiresOnDeviceRecognition=recognizer.supportsOnDeviceRecognition
            let input=e.inputNode,format=input.outputFormat(forBus:0);guard format.sampleRate>0,format.channelCount>0 else {throw URLError(.cannotDecodeRawData)}
            input.installTap(onBus:0,bufferSize:1024,format:format){buffer,_ in r.append(buffer)}
            engine=e;request=r;task=recognizer.recognitionTask(with:r){[weak self] result,error in Task {@MainActor in
                guard self?.generation==token else{return}
                if let result=result {self?.text=result.bestTranscription.formattedString;if result.isFinal {self?.stop()}}
                if let error=error,self?.recording==true {self?.error=error.localizedDescription;self?.stop()}
            }}
            e.prepare();try e.start();recording=true
            timeout=Task {try? await Task.sleep(nanoseconds:30_000_000_000);if !Task.isCancelled {finish()}}
        }catch {self.error=error.localizedDescription;stop()}
    }
    func finish() {engine?.stop();engine?.inputNode.removeTap(onBus:0);engine=nil;request?.endAudio();recording=false;timeout?.cancel();timeout=Task {try? await Task.sleep(nanoseconds:15_000_000_000);if !Task.isCancelled {stop()}}}
    func stop() {generation+=1;timeout?.cancel();timeout=nil;engine?.stop();engine?.inputNode.removeTap(onBus:0);engine=nil;request?.endAudio();task?.cancel();task=nil;request=nil;recording=false;try? AVAudioSession.sharedInstance().setActive(false,options:.notifyOthersOnDeactivation)}
}
private final class PCMBuffer {
    private let lock=NSLock();private var samples=[Float]()
    func append(_ values:UnsafeBufferPointer<Float>){lock.lock();defer {lock.unlock()};samples.append(contentsOf:values.prefix(max(0,16000*90-samples.count)))}
    func snapshot()->[Float]{lock.lock();defer {lock.unlock()};return samples}
    func meter()->(seconds:Double,level:Double) {
        lock.lock();defer{lock.unlock()}
        let tail=samples.suffix(3200)
        let energy=tail.reduce(0.0){$0+Double($1*$1)}
        return (Double(samples.count)/16000,tail.isEmpty ? 0:sqrt(energy/Double(tail.count)))
    }
    func clear(){lock.lock();defer {lock.unlock()};samples.removeAll(keepingCapacity:false)}
}
struct QuranModelSpec:Decodable {let url:String,bytes:Int,sha256:String,fileName:String}
@MainActor final class QuranRecitationService:ObservableObject {
    @Published var ready=false;@Published var downloading=false;@Published var recording=false;@Published var processing=false;@Published var seconds=0.0;@Published var level=0.0;@Published var transcript="";@Published var error:String?;@Published var final=false;@Published var micOnly=false
    let state:AppState
    private let samples=PCMBuffer(),inference=DispatchQueue(label:"nuraya.whisper.inference",qos:.userInitiated)
    private var engine:AVAudioEngine?,native:NurayaWhisper?,pulse:Task<Void,Never>?,generation=0,lastPartial=0.0
    private var currentTask:Task<Void,Never>?
    init(state:AppState){self.state=state}
    private func spec()throws->QuranModelSpec {try state.content.decode("quran_model.json")}
    func inspect() {do {let s=try spec();ready=URL(string:s.url).flatMap {state.downloads.cached($0)} != nil}catch {self.error=error.localizedDescription}}
    func download() async {
        downloading=true;error=nil;defer {downloading=false}
        do {let s=try spec();guard let url=URL(string:s.url) else {throw ContentError.invalidModel};_=try await state.downloads.fetch(url,sha256:s.sha256,bytes:s.bytes);ready=true}catch {self.error="Model belum siap: \(error.localizedDescription)"}
    }
    func start(microphoneOnly:Bool=false) async {
        cancel();generation+=1;let startToken=generation;error=nil;transcript="";final=false;seconds=0;level=0;micOnly=microphoneOnly;samples.clear();lastPartial=0
        let permitted=await withCheckedContinuation {c in AVAudioSession.sharedInstance().requestRecordPermission {c.resume(returning:$0)}}
        guard generation==startToken else {return}
        guard permitted else {error="Izin mikrofon belum aktif. Buka Pengaturan iPhone.";return}
        state.audio.stop()
        do {
            if !microphoneOnly {
                let s=try spec();guard let u=URL(string:s.url) else {throw ContentError.invalidModel}
                // Fetch verifies an existing model too; no inference from an unchecked cache file.
                let file=try await state.downloads.fetch(u,sha256:s.sha256,bytes:s.bytes)
                if native==nil {processing=true;let engine:NurayaWhisper=try await withCheckedThrowingContinuation {c in inference.async {do {let n=try NurayaWhisper(modelPath:file.path);c.resume(returning:n)}catch {c.resume(throwing:error)}}};native=engine;processing=false}
            }
            guard generation==startToken else {processing=false;return}
            let session=AVAudioSession.sharedInstance();try session.setCategory(.record,mode:.measurement);try session.setActive(true)
            let e=AVAudioEngine(),input=e.inputNode,source=input.outputFormat(forBus:0)
            guard source.sampleRate>0,source.channelCount>0,let target=AVAudioFormat(commonFormat:.pcmFormatFloat32,sampleRate:16000,channels:1,interleaved:false),let converter=AVAudioConverter(from:source,to:target) else {throw ContentError.invalidModel}
            let buffer=samples
            input.installTap(onBus:0,bufferSize:2048,format:source){audio,_ in
                let capacity=AVAudioFrameCount(ceil(Double(audio.frameLength)*16000/source.sampleRate))+32
                guard let out=AVAudioPCMBuffer(pcmFormat:target,frameCapacity:capacity) else {return}
                var supplied=false;var conversionError:NSError?
                converter.convert(to:out,error:&conversionError){_,status in if supplied {status.pointee = .noDataNow;return nil};supplied=true;status.pointee = .haveData;return audio}
                if conversionError==nil,let p=out.floatChannelData?[0] {buffer.append(UnsafeBufferPointer(start:p,count:Int(out.frameLength)))}
            }
            engine=e;e.prepare();try e.start();recording=true
            pulse=Task {
                while !Task.isCancelled && recording {
                    try? await Task.sleep(nanoseconds:200_000_000);if Task.isCancelled {return}
                    let meter=samples.meter();seconds=meter.seconds;level=meter.level
                    if seconds >= (micOnly ? 5:90) {finish();return}
                    if !micOnly,seconds-lastPartial>=6,!processing {lastPartial=seconds;recognize(samples.snapshot(),final:false)}
                }
            }
        }catch {processing=false;self.error=error.localizedDescription;stopCapture()}
    }
    private func stopCapture(){pulse?.cancel();pulse=nil;engine?.stop();engine?.inputNode.removeTap(onBus:0);engine=nil;recording=false;try? AVAudioSession.sharedInstance().setActive(false,options:.notifyOthersOnDeactivation)}
    func finish(){stopCapture();if micOnly {final=true;transcript=level>0.001 ? "Mikrofon menerima suara. Cek ini bukan penilaian bacaan.":"Suara terlalu kecil. Periksa mikrofon dan coba lagi.";samples.clear();return};native?.cancel();recognize(samples.snapshot(),final:true)}
    private func recognize(_ pcm:[Float],final:Bool) {
        guard let native=native,pcm.count>=1600 else {if final {error="Rekaman terlalu pendek."};return}
        processing=true;let token=generation;let nativeToken=native.generation()
        let data=pcm.withUnsafeBufferPointer {Data(buffer:$0)}
        currentTask=Task {
            do {let text:String=try await withCheckedThrowingContinuation {c in inference.async {do {c.resume(returning:try native.transcribePCM(data,generation:nativeToken))}catch {c.resume(throwing:error)}}}
                guard generation==token,native.generation()==nativeToken else {return};transcript=text.trimmingCharacters(in:.whitespacesAndNewlines);self.final=final
                if final && transcript.isEmpty {error="Belum ada kata yang dikenali. Bacalah lebih dekat dan jelas."}
            }catch {if generation==token && native.generation()==nativeToken && final {self.error=error.localizedDescription}}
            if generation==token && native.generation()==nativeToken {processing=false;if final {samples.clear()}}
        }
    }
    func cancel(){generation+=1;native?.cancel();currentTask?.cancel();currentTask=nil;stopCapture();processing=false;samples.clear()}
}
