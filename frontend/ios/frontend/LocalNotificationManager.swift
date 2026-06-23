import Foundation
import UserNotifications
import React

/// UNUserNotificationCenter delegate that survives JS reloads.
/// Cold-start taps arrive before the JS bundle is ready, so they are held
/// here until JS pulls them via getInitialNotification().
final class LocalNotificationTapStore: NSObject, UNUserNotificationCenterDelegate {
  static let shared = LocalNotificationTapStore()

  private var pendingTap: [String: Any]?
  weak var emitter: LocalNotificationManager?

  func userNotificationCenter(_ center: UNUserNotificationCenter,
                              didReceive response: UNNotificationResponse,
                              withCompletionHandler completionHandler: @escaping () -> Void) {
    let request = response.notification.request
    // 远程推送（APNs）的点击交给 RemotePushStore，本地通知走原逻辑。
    if request.trigger is UNPushNotificationTrigger {
      RemotePushStore.shared.deliverTap(request.content.userInfo)
      completionHandler()
      return
    }
    let tap: [String: Any] = [
      "id": request.identifier,
      "payload": request.content.userInfo,
    ]
    if let emitter = emitter, emitter.canEmit {
      emitter.emitTap(tap)
    } else {
      pendingTap = tap
    }
    completionHandler()
  }

  func userNotificationCenter(_ center: UNUserNotificationCenter,
                              willPresent notification: UNNotification,
                              withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
    // Meal reminders are redundant while the user is already in the app.
    completionHandler([])
  }

  func consumePendingTap() -> [String: Any]? {
    let tap = pendingTap
    pendingTap = nil
    return tap
  }
}

@objc(LocalNotificationManager)
final class LocalNotificationManager: RCTEventEmitter {
  private var hasListeners = false

  override init() {
    super.init()
    LocalNotificationTapStore.shared.emitter = self
  }

  @objc
  override static func requiresMainQueueSetup() -> Bool {
    true
  }

  override func supportedEvents() -> [String]! {
    ["localNotificationTap"]
  }

  override func startObserving() {
    hasListeners = true
  }

  override func stopObserving() {
    hasListeners = false
  }

  var canEmit: Bool {
    hasListeners
  }

  func emitTap(_ tap: [String: Any]) {
    sendEvent(withName: "localNotificationTap", body: tap)
  }

  @objc(requestPermission:rejecter:)
  func requestPermission(_ resolve: @escaping RCTPromiseResolveBlock,
                         rejecter reject: @escaping RCTPromiseRejectBlock) {
    UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
      resolve(["granted": granted])
    }
  }

  /// Provisional authorization is granted silently (no system dialog):
  /// notifications are delivered quietly to Notification Center, which lets
  /// reminders be on by default before the user ever sees a permission prompt.
  @objc(requestProvisionalPermission:rejecter:)
  func requestProvisionalPermission(_ resolve: @escaping RCTPromiseResolveBlock,
                                    rejecter reject: @escaping RCTPromiseRejectBlock) {
    UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge, .provisional]) { granted, _ in
      resolve(["granted": granted])
    }
  }

  @objc(getPermissionStatus:rejecter:)
  func getPermissionStatus(_ resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock) {
    UNUserNotificationCenter.current().getNotificationSettings { settings in
      let status: String
      switch settings.authorizationStatus {
      case .authorized, .ephemeral:
        status = "authorized"
      case .provisional:
        status = "provisional"
      case .denied:
        status = "denied"
      case .notDetermined:
        status = "notDetermined"
      @unknown default:
        status = "notDetermined"
      }
      resolve(["status": status])
    }
  }

  /// Replaces the full pending set: this app schedules only its own
  /// notifications, so a wholesale remove-then-add keeps JS reconciliation trivial.
  @objc(replaceAllScheduled:resolver:rejecter:)
  func replaceAllScheduled(_ specs: NSArray,
                           resolver resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock) {
    let center = UNUserNotificationCenter.current()
    center.removeAllPendingNotificationRequests()

    var scheduled = 0
    for case let spec as [String: Any] in specs {
      guard let id = spec["id"] as? String,
            let title = spec["title"] as? String,
            let body = spec["body"] as? String,
            let year = spec["year"] as? Int,
            let month = spec["month"] as? Int,
            let day = spec["day"] as? Int,
            let hour = spec["hour"] as? Int,
            let minute = spec["minute"] as? Int else {
        continue
      }

      let content = UNMutableNotificationContent()
      content.title = title
      content.body = body
      content.sound = .default
      if let payload = spec["payload"] as? [String: Any] {
        content.userInfo = payload
      }

      var components = DateComponents()
      components.year = year
      components.month = month
      components.day = day
      components.hour = hour
      components.minute = minute

      let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
      center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
      scheduled += 1
    }

    resolve(["scheduled": scheduled])
  }

  @objc(cancelAllScheduled:rejecter:)
  func cancelAllScheduled(_ resolve: @escaping RCTPromiseResolveBlock,
                          rejecter reject: @escaping RCTPromiseRejectBlock) {
    UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    resolve(["ok": true])
  }

  @objc(getPendingIds:rejecter:)
  func getPendingIds(_ resolve: @escaping RCTPromiseResolveBlock,
                     rejecter reject: @escaping RCTPromiseRejectBlock) {
    UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
      resolve(requests.map { $0.identifier })
    }
  }

  @objc(getInitialNotification:rejecter:)
  func getInitialNotification(_ resolve: @escaping RCTPromiseResolveBlock,
                              rejecter reject: @escaping RCTPromiseRejectBlock) {
    if let tap = LocalNotificationTapStore.shared.consumePendingTap() {
      resolve(tap)
    } else {
      resolve(NSNull())
    }
  }
}
