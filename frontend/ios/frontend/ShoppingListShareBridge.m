#import <React/RCTBridgeModule.h>
#import <React/RCTUtils.h>
#import <UIKit/UIKit.h>

@interface ShoppingListShareBridge : NSObject <RCTBridgeModule>
@end

@implementation ShoppingListShareBridge

RCT_EXPORT_MODULE(ShoppingListShareBridge)

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

RCT_REMAP_METHOD(shareTextFile,
                 shareTextFile:(NSString *)content
                 title:(NSString *)title
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    if (content.length == 0) {
        reject(@"empty_content", @"Shopping list share content is empty", nil);
        return;
    }

    NSString *baseName = title.length > 0 ? title : @"shopping-list";
    NSCharacterSet *invalidCharacters = [NSCharacterSet characterSetWithCharactersInString:@"/\\?%*|\"<>:"];
    NSArray<NSString *> *parts = [baseName componentsSeparatedByCharactersInSet:invalidCharacters];
    NSString *safeBaseName = [[parts componentsJoinedByString:@"-"] stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceAndNewlineCharacterSet];
    if (safeBaseName.length == 0) {
        safeBaseName = @"shopping-list";
    }

    NSString *fileName = [NSString stringWithFormat:@"%@.txt", safeBaseName];
    NSURL *fileURL = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:fileName]];

    NSError *writeError = nil;
    BOOL didWrite = [content writeToURL:fileURL atomically:YES encoding:NSUTF8StringEncoding error:&writeError];
    if (!didWrite || writeError != nil) {
        reject(@"write_failed", @"Unable to prepare shopping list share file", writeError);
        return;
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *controller = RCTPresentedViewController();
        if (controller == nil) {
            reject(@"missing_controller", @"Unable to find a view controller for sharing", nil);
            return;
        }

        UIActivityViewController *shareController = [[UIActivityViewController alloc] initWithActivityItems:@[ fileURL ]
                                                                                      applicationActivities:nil];
        if (title.length > 0) {
            [shareController setValue:title forKey:@"subject"];
        }

        UIPopoverPresentationController *popover = shareController.popoverPresentationController;
        if (popover != nil) {
            popover.sourceView = controller.view;
            popover.sourceRect = CGRectMake(CGRectGetMidX(controller.view.bounds), CGRectGetMidY(controller.view.bounds), 1, 1);
            popover.permittedArrowDirections = 0;
        }

        shareController.completionWithItemsHandler =
            ^(UIActivityType _Nullable activityType, BOOL completed, NSArray * _Nullable returnedItems, NSError * _Nullable activityError) {
                if (activityError != nil) {
                    reject(@"share_failed", activityError.localizedDescription ?: @"Shopping list share failed", activityError);
                    return;
                }

                resolve(@{
                    @"completed": @(completed),
                    @"activityType": activityType ?: [NSNull null],
                });
            };

        [controller presentViewController:shareController animated:YES completion:nil];
    });
}

@end
