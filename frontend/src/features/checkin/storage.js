import AsyncStorage from '@react-native-async-storage/async-storage';
import { DEFAULT_CHECKIN_SETTINGS } from './utils';

const KEY = 'goaway.checkin.settings.v1';

export const readCheckinSettings = async () => {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    if (!raw) return { ...DEFAULT_CHECKIN_SETTINGS };
    return { ...DEFAULT_CHECKIN_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return { ...DEFAULT_CHECKIN_SETTINGS };
  }
};

export const writeCheckinSettings = async (settings) => {
  try {
    await AsyncStorage.setItem(KEY, JSON.stringify(settings));
  } catch {
    // 本地持久化失败不应阻断使用
  }
};
