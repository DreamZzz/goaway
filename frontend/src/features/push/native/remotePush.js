import {
  NativeEventEmitter,
  NativeModules,
  Platform,
  TurboModuleRegistry,
} from 'react-native';

/** APNs 设备 token 就绪事件（payload: { token }）。 */
export const REMOTE_PUSH_TOKEN_EVENT = 'remotePushToken';
/** 远程通知被点击事件（payload: 通知自定义字段，如 { type:'taunt', trigger }）。 */
export const REMOTE_PUSH_TAP_EVENT = 'remotePushTap';

const nativeModule =
  Platform.OS === 'ios'
    ? TurboModuleRegistry.get('RemotePushManager') ||
      NativeModules.RemotePushManager ||
      null
    : null;

let eventEmitter = null;
const noopSubscription = { remove: () => {} };

const getEventEmitter = () => {
  if (!nativeModule) return null;
  if (!eventEmitter) eventEmitter = new NativeEventEmitter(nativeModule);
  return eventEmitter;
};

export const isRemotePushAvailable = () => Platform.OS === 'ios' && !!nativeModule;

const ensureNativeModule = () => {
  if (!isRemotePushAvailable()) {
    throw new Error('RemotePushManager native module is missing');
  }
  return nativeModule;
};

export const remotePush = {
  isAvailable: isRemotePushAvailable,

  /** 申请通知权限并向 APNs 注册远程通知；token 通过 REMOTE_PUSH_TOKEN_EVENT 异步回来。 */
  requestPermissionAndRegister: () => ensureNativeModule().requestPermissionAndRegister(),

  getPermissionStatus: () => ensureNativeModule().getPermissionStatus(),

  /** 冷启动时若由点击通知拉起，返回该通知 payload，否则 null。 */
  getInitialNotification: () => ensureNativeModule().getInitialNotification(),

  addTokenListener: (listener) => {
    const emitter = getEventEmitter();
    if (!emitter) return noopSubscription;
    return emitter.addListener(REMOTE_PUSH_TOKEN_EVENT, listener);
  },

  addTapListener: (listener) => {
    const emitter = getEventEmitter();
    if (!emitter) return noopSubscription;
    return emitter.addListener(REMOTE_PUSH_TAP_EVENT, listener);
  },
};
