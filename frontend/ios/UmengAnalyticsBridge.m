#import <React/RCTBridgeModule.h>
#import <TargetConditionals.h>

#if !TARGET_OS_SIMULATOR
#import <UMCommon/MobClick.h>
#endif

/**
 * RN 原生模块 UmengAnalyticsBridge，暴露给 JS 层的 Umeng 统计 API。
 *
 * 业务代码不应直接使用本模块，而应通过
 *   src/shared/analytics/adapters/UmengAdapter.js
 * 间接调用，以便 jest / 未集成 Umeng 的环境能走 fallback 通道。
 */
@interface UmengAnalyticsBridge : NSObject <RCTBridgeModule>
@end

@implementation UmengAnalyticsBridge

RCT_EXPORT_MODULE(UmengAnalyticsBridge)

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

RCT_EXPORT_METHOD(track:(NSString *)eventName
                  properties:(NSDictionary *)properties) {
    if (eventName.length == 0) {
        return;
    }
#if TARGET_OS_SIMULATOR
    return;
#else
    if (properties.count > 0) {
        [MobClick event:eventName attributes:properties];
    } else {
        [MobClick event:eventName];
    }
#endif
}

RCT_EXPORT_METHOD(onProfileSignIn:(NSString *)userId) {
    if (userId.length == 0) {
        return;
    }
#if TARGET_OS_SIMULATOR
    return;
#else
    [MobClick profileSignInWithPUID:userId];
#endif
}

RCT_EXPORT_METHOD(onProfileSignOff) {
#if TARGET_OS_SIMULATOR
    return;
#else
    [MobClick profileSignOff];
#endif
}

@end
