#import <Foundation/Foundation.h>
NS_ASSUME_NONNULL_BEGIN
@interface NurayaWhisper : NSObject
- (nullable instancetype)initWithModelPath:(NSString *)path error:(NSError **)error;
- (uint64_t)generation;
- (nullable NSString *)transcribePCM:(NSData *)pcm generation:(uint64_t)generation error:(NSError **)error;
- (void)cancel;
@end
NS_ASSUME_NONNULL_END
