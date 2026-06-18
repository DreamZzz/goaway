#import <Foundation/Foundation.h>

/**
 * Umeng (友盟 U-App) 初始化入口。
 *
 * 通过 +load 自动注册 UIApplicationDidFinishLaunchingNotification，
 * 应用启动时读取 Info.plist 中的 UmengAppKey / UmengChannel 并调用
 * UMConfigure initWithAppkey:channel:。
 *
 * 这样设计是为了避免修改纯 Swift 的 AppDelegate（否则需要引入 Bridging Header）。
 */
@interface UmengBootstrap : NSObject
@end
