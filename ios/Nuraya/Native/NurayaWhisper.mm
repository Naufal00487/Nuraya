#import "NurayaWhisper.h"
#include "whisper.h"
#include <atomic>
#include <mutex>
#include <vector>
#include <memory>
#include <cmath>
#include <chrono>
struct Session {whisper_context *context=nullptr;std::mutex mutex;std::atomic<uint64_t> generation{0};~Session(){if(context)whisper_free(context);}};
struct Request {Session *session;uint64_t generation;std::chrono::steady_clock::time_point start;};
static bool abortInference(void *ptr){auto*r=(Request*)ptr;return r->session->generation.load()!=r->generation||std::chrono::steady_clock::now()-r->start>std::chrono::seconds(120);}
static void quiet(enum ggml_log_level,const char*,void*){}
static NSError* failure(NSString*reason){return [NSError errorWithDomain:@"Nuraya.Whisper" code:1 userInfo:@{NSLocalizedDescriptionKey:reason}];}
@implementation NurayaWhisper {std::unique_ptr<Session> _session;}
- (instancetype)initWithModelPath:(NSString*)path error:(NSError**)error {
    self=[super init];if(!self)return nil;
    try {whisper_log_set(quiet,nullptr);auto p=whisper_context_default_params();p.use_gpu=false;
        _session=std::make_unique<Session>();_session->context=whisper_init_from_file_with_params(path.UTF8String,p);
        if(!_session->context){if(error)*error=failure(@"Model AI tidak bisa dibuka.");return nil;}
    }catch(...){if(error)*error=failure(@"Memori tidak cukup untuk membuka model AI.");return nil;}
    return self;
}
- (NSString*)transcribePCM:(NSData*)pcm error:(NSError**)error {
    if(!_session){if(error)*error=failure(@"Mesin bacaan belum siap.");return nil;}
    const auto generation=_session->generation.load();
    std::lock_guard<std::mutex> lock(_session->mutex);
    Request request{_session.get(),generation,std::chrono::steady_clock::now()};
    if(pcm.length%sizeof(float)||pcm.length<1600*sizeof(float)||pcm.length>16000*90*sizeof(float)){if(error)*error=failure(@"Durasi rekaman harus 0,1–90 detik.");return nil;}
    try {
        auto samples=(const float*)pcm.bytes;auto count=(int)(pcm.length/sizeof(float));double energy=0;float peak=0;
        for(int i=0;i<count;++i){auto v=samples[i];if(!std::isfinite(v)||std::abs(v)>1){if(error)*error=failure(@"Format suara tidak valid.");return nil;}energy+=v*v;peak=std::max(peak,std::abs(v));}
        if(peak<.01f&&std::sqrt(energy/count)<.001)return @"";
        auto p=whisper_full_default_params(WHISPER_SAMPLING_GREEDY);p.n_threads=std::min(4,(int)NSProcessInfo.processInfo.activeProcessorCount);p.language="ar";p.translate=false;p.detect_language=false;p.no_context=true;p.no_timestamps=true;p.print_special=false;p.print_progress=false;p.print_realtime=false;p.print_timestamps=false;p.suppress_nst=true;p.temperature=0;p.temperature_inc=0;p.greedy.best_of=1;p.initial_prompt=nullptr;p.prompt_tokens=nullptr;p.prompt_n_tokens=0;p.abort_callback=abortInference;p.abort_callback_user_data=&request;
        const int status=whisper_full(_session->context,p,samples,count);
        if(status!=0||abortInference(&request)){if(error)*error=failure(@"Pengenalan dihentikan atau melewati batas waktu. Coba ayat lebih pendek.");return nil;}
        NSMutableString* result=[NSMutableString string];for(int i=0;i<whisper_full_n_segments(_session->context);++i){const char*t=whisper_full_get_segment_text(_session->context,i);if(t)[result appendString:[NSString stringWithUTF8String:t]?:@""];}
        return result;
    }catch(...){if(error)*error=failure(@"Bacaan belum dapat diproses pada perangkat ini.");return nil;}
}
- (void)cancel {if(_session)_session->generation.fetch_add(1);}
@end
