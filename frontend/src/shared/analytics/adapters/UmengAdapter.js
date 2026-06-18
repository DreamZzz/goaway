/**
 * Umeng (友盟 U-App) RN SDK adapter.
 *
 * iOS: 原生模块 UmengAnalyticsBridge (UmengAnalyticsBridge.m)，SDK 初始化由
 *   UmengBootstrap.m 在应用启动时自动完成。
 * Android: 原生模块 UmengAnalyticsBridgeModule.kt，SDK 初始化由
 *   MainApplication.initUmeng() 完成。
 *
 * 使用 TurboModuleRegistry.get() 兼容 RN 新架构 bridgeless 模式；
 * 当原生模块不可用时（jest、未完成 pod install 等）退化到 fallback。
 */
import { NativeModules, TurboModuleRegistry } from 'react-native';
import analyticsAPI from '../../api/analyticsAPI';

const nativeModule =
  TurboModuleRegistry.get('UmengAnalyticsBridge') ||
  NativeModules.UmengAnalyticsBridge ||
  null;
const nativeAvailable = !!nativeModule;
const isTestEnvironment = typeof process !== 'undefined' && process.env?.NODE_ENV === 'test';

if (!nativeAvailable && !isTestEnvironment && typeof __DEV__ !== 'undefined' && __DEV__) {
  console.warn('[analytics] UmengAnalyticsBridge native module unavailable; using fallback uploader');
}

const safeCall = (fn, label) => {
  try {
    fn();
  } catch (error) {
     
    console.warn(`[analytics] Umeng ${label} failed:`, error?.message || error);
  }
};

const fallbackPost = (eventName, properties) => {
  analyticsAPI.postEvent(eventName, properties).catch(() => {
    /* swallow — analytics must never break business flow */
  });
};

const createUmengAdapter = ({ appKey: _appKey, channel: _channel }) => ({
  name: 'umeng',
  init() {
    // 初始化由原生侧 UmengBootstrap.m 在应用启动时自动完成，此处 no-op。
    // appKey / channel 参数保留以兼容 selectAdapter 签名，但实际来源是 Info.plist。
  },
  identify(userId) {
    if (!userId || !nativeAvailable) return;
    safeCall(() => nativeModule.onProfileSignIn(String(userId)), 'identify');
  },
  reset() {
    if (!nativeAvailable) return;
    safeCall(() => nativeModule.onProfileSignOff(), 'reset');
  },
  track(eventName, properties) {
    if (!nativeAvailable) {
      fallbackPost(eventName, properties);
      return;
    }
    safeCall(() => nativeModule.track(eventName, properties || {}), 'track');
  },
  flush() {
    // SDK handles its own flush queue
  },
});

export default createUmengAdapter;
