#import <Foundation/Foundation.h>
NS_ASSUME_NONNULL_BEGIN
@interface NurayaWhisper : NSObject
- (nullable instancetype)initWithModelPath:(NSString *)path error:(NSError **)error;
- (nullable NSString *)transcribePCM:(NSData *)pcm error:(NSError **)error;
- (void)cancel;
@end
NS_ASSUME_NONNULL_END
