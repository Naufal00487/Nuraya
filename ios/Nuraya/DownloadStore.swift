import Foundation
import CryptoKit
import UIKit
import NurayaCore
@MainActor final class DownloadStore:ObservableObject {
    let directory:URL,content:ContentStore
    @Published var tasks=[String:Double](),error:String?
    private var active=[String:Task<URL,Error>]()
    init(directory:URL,content:ContentStore)throws {self.directory=directory;self.content=content;try FileManager.default.createDirectory(at:directory,withIntermediateDirectories:true);var d=directory;var values=URLResourceValues();values.isExcludedFromBackup=true;try d.setResourceValues(values)}
    func key(_ url:URL)->String {SHA256.hash(data:Data(url.absoluteString.utf8)).map {String(format:"%02x",$0)}.joined()}
    func cached(_ url:URL)->URL? {let p=directory.appendingPathComponent(key(url)+"."+url.pathExtension);return FileManager.default.fileExists(atPath:p.path) ? p:nil}
    func fetch(_ url:URL,sha256:String?=nil,bytes:Int?=nil) async throws -> URL {
        guard url.scheme=="https" else {throw URLError(.unsupportedURL)}
        if let file=cached(url) {if sha256==nil || verify(file,sha:sha256!,bytes:bytes) {return file}}
        let id=key(url);if let t=active[id] {return try await t.value}
        tasks[id]=0
        let target=directory.appendingPathComponent(id+"."+url.pathExtension)
        let task=Task<URL,Error> {
            let (temp,response)=try await URLSession.shared.download(from:url)
            defer {try? FileManager.default.removeItem(at:temp)}
            guard let response=response as? HTTPURLResponse,response.statusCode==200 else {throw URLError(.badServerResponse)}
            if let sha=sha256 {guard verify(temp,sha:sha,bytes:bytes) else {throw ContentError.invalidCorpus}}
            else {let size=try temp.resourceValues(forKeys:[.fileSizeKey]).fileSize ?? 0;guard size>0 else {throw URLError(.zeroByteResource)}}
            if FileManager.default.fileExists(atPath:target.path) {try FileManager.default.removeItem(at:target)}
            try FileManager.default.moveItem(at:temp,to:target);return target
        }
        active[id]=task
        defer {tasks[id]=nil;active[id]=nil}
        return try await task.value
    }
    private func verify(_ file:URL,sha:String,bytes:Int?)->Bool {
        guard let data=try? Data(contentsOf:file,options:.mappedIfSafe),bytes==nil || data.count==bytes else {return false}
        return SHA256.hash(data:data).map {String(format:"%02x",$0)}.joined()==sha
    }
    func page(_ edition:MushafEdition,_ number:Int) async throws -> UIImage {
        if let bundled=content.image(String(format:"printed_mushaf/%@/%03d.png",edition.id,number)) {return bundled}
        guard let meta=content.pageMetadata(edition,number),let url=URL(string:meta.url) else {throw ContentError.invalidReference}
        let p=try await fetch(url,sha256:meta.sha256,bytes:meta.bytes)
        guard let image=UIImage(contentsOfFile:p.path) else {throw ContentError.invalidCorpus};return image
    }
    func remove(_ url:URL)throws {if let p=cached(url) {try FileManager.default.removeItem(at:p)};objectWillChange.send()}
    func cancel(_ url:URL) {active[key(url)]?.cancel()}
    func size()->Int64 {let enumerator=FileManager.default.enumerator(at:directory,includingPropertiesForKeys:[.fileSizeKey]);return enumerator?.compactMap {$0 as? URL}.reduce(0) {$0+Int64((try? $1.resourceValues(forKeys:[.fileSizeKey]).fileSize) ?? 0)} ?? 0}
}
