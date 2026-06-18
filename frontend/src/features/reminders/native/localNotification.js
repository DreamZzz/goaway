import {
  NativeEventEmitter,
  NativeModules,
  Platform,
  TurboModuleRegistry,
} from 'react-native';

export const NOTIFICATION_TAP_EVENT = 'localNotificationTap';

const nativeModule =
  Platform.OS === 'ios'
    ? TurboModuleRegistry.get('LocalNotificationManager') ||
      NativeModules.LocalNotificationManager ||
      null
    : null;

let eventEmitter = null;

const noopSubscription = { remove: () => {} };

const getEventEmitter = () => {
  if (!nativeModule) return null;
  if (!eventEmitter) eventEmitter = new NativeEventEmitter(nativeModule);
  return eventEmitter;
};

export const isLocalNotificationAvailable = () =>
  Platform.OS === 'ios' && !!nativeModule;

const ensureNativeModule = () => {
  if (!isLocalNotificationAvailable()) {
    throw new Error('LocalNotificationManager native module is missing');
  }
  return nativeModule;
};

export const localNotification = {
  isAvailable: isLocalNotificationAvailable,
  addTapListener: (listener) => {
    const emitter = getEventEmitter();
    if (!emitter) return noopSubscription;
    return emitter.addListener(NOTIFICATION_TAP_EVENT, listener);
  },
  requestPermission: () => ensureNativeModule().requestPermission(),
  requestProvisionalPermission: () => ensureNativeModule().requestProvisionalPermission(),
  getPermissionStatus: () => ensureNativeModule().getPermissionStatus(),
  replaceAllScheduled: (specs) => ensureNativeModule().replaceAllScheduled(specs),
  cancelAllScheduled: () => ensureNativeModule().cancelAllScheduled(),
  getPendingIds: () => ensureNativeModule().getPendingIds(),
  getInitialNotification: () => ensureNativeModule().getInitialNotification(),
};
