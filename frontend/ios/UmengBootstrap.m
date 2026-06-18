#import "UmengBootstrap.h"
#import <UIKit/UIKit.h>
#import <TargetConditionals.h>

#if !TARGET_OS_SIMULATOR
#import <UMCommon/UMCommon.h>
#import <UMCommon/UMConfigure.h>
#import <UMCommon/MobClick.h>
#import <UMCommonLog/UMCommonLogHeaders.h>
#endif

@implementation UmengBootstrap

+ (void)load {
    [[NSNotificationCenter defaultCenter]
        addObserver:self
           selector:@selector(applicationDidFinishLaunching:)
               name:UIApplicationDidFinishLaunchingNotification
             object:nil];
}

+ (void)applicationDidFinishLaunching:(NSNotification *)notification {
#if TARGET_OS_SIMULATOR
    NSLog(@"[Umeng] Skipped native init on simulator; using JS fallback uploader");
    return;
#else
    NSBundle *bundle = [NSBundle mainBundle];
    NSString *appKey = [bundle objectForInfoDictionaryKey:@"UmengAppKey"];
    NSString *channel = [bundle objectForInfoDictionaryKey:@"UmengChannel"];

    if (appKey.length == 0) {
        NSLog(@"[Umeng] Skipped init: UmengAppKey missing in Info.plist");
        return;
    }
    if (channel.length == 0) {
        channel = @"AppStore";
    }

#if DEBUG
    [UMCommonLogManager setUpUMCommonLogManager];
    [UMConfigure setLogEnabled:YES];
#endif

    [UMConfigure initWithAppkey:appKey channel:channel];
    NSLog(@"[Umeng] Initialized with appKey=%@ channel=%@", appKey, channel);
#endif
}

@end
