import Foundation
import UIKit
import UserNotifications
import React

/// 远程推送（APNs）的 token / 点击中转，跨 JS reload 存活。
/// token 在 AppDelegate didRegister 回调里到达，冷启动点击也先存这里，
/// 等 JS 就绪后通过事件 / getInitialNotification() 取走。
final class RemotePushStore: NSObject {
  static let shared = RemotePushStore()

  weak var emitter: RemotePushManager?
  private var pendingToken: String?
  private var pendingTap: [String: Any]?

  func deliverToken(_ token: String) {
    if let emitter = emitter, emitter.canEmit {
      emitter.emitToken(token)
    } else {
      pendingToken = token
    }
  }

  func deliverTap(_ payload: [AnyHashable: Any]) {
    let normalized = payload.reduce(into: [String: Any]()) { acc, kv in
      if let key = kv.key as? String { acc[key] = kv.value }
    }
    if let emitter = emitter, emitter.canEmit {
      emitter.emitTap(normalized)
    } else {
      pendingTap = normalized
    }
  }

  func flushPending() {
    if let token = pendingToken {
      emitter?.emitToken(token)
      pendingToken = nil
    }
  }

  func consumePendingTap() -> [String: Any]? {
    let tap = pendingTap
    pendingTap = nil
    return tap
  }
}

@objc(RemotePushManager)
final class RemotePushManager: RCTEventEmitter {
  private var hasListeners = false

  override init() {
    super.init()
    RemotePushStore.shared.emitter = self
  }

  @objc
  override static func requiresMainQueueSetup() -> Bool {
    true
  }

  override func supportedEvents() -> [String]! {
    ["remotePushToken", "remotePushTap"]
  }

  override func startObserving() {
    hasListeners = true
    RemotePushStore.shared.flushPending()
  }

  override func stopObserving() {
    hasListeners = false
  }

  var canEmit: Bool { hasListeners }

  func emitToken(_ token: String) {
    sendEvent(withName: "remotePushToken", body: ["token": token])
  }

  func emitTap(_ payload: [String: Any]) {
    sendEvent(withName: "remotePushTap", body: payload)
  }

  /// 申请通知权限，授权后向 APNs 注册远程通知（token 通过 remotePushToken 事件回来）。
  @objc(requestPermissionAndRegister:rejecter:)
  func requestPermissionAndRegister(_ resolve: @escaping RCTPromiseResolveBlock,
                                    rejecter reject: @escaping RCTPromiseRejectBlock) {
    UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
      if granted {
        DispatchQueue.main.async {
          UIApplication.shared.registerForRemoteNotifications()
        }
      }
      resolve(["granted": granted])
    }
  }

  @objc(getPermissionStatus:rejecter:)
  func getPermissionStatus(_ resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock) {
    UNUserNotificationCenter.current().getNotificationSettings { settings in
      let status: String
      switch settings.authorizationStatus {
      case .authorized, .ephemeral: status = "authorized"
      case .provisional: status = "provisional"
      case .denied: status = "denied"
      case .notDetermined: status = "notDetermined"
      @unknown default: status = "notDetermined"
      }
      resolve(["status": status])
    }
  }

  @objc(getInitialNotification:rejecter:)
  func getInitialNotification(_ resolve: @escaping RCTPromiseResolveBlock,
                              rejecter reject: @escaping RCTPromiseRejectBlock) {
    if let tap = RemotePushStore.shared.consumePendingTap() {
      resolve(tap)
    } else {
      resolve(NSNull())
    }
  }
}
