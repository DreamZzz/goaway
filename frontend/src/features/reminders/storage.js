import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY = 'goaway.reminders.settings.v1';

export const DEFAULT_REMINDER_SETTINGS = {
  water: { enabled: false, intervalMin: 60 },
  sedentary: { enabled: false, intervalMin: 60 },
  offWork: { enabled: false, time: '18:00' },
  // 喝水 / 久坐提醒仅在工作时段内触发
  windowStart: '09:00',
  windowEnd: '18:00',
};

export const readReminderSettings = async () => {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    if (!raw) return { ...DEFAULT_REMINDER_SETTINGS };
    const parsed = JSON.parse(raw);
    return {
      ...DEFAULT_REMINDER_SETTINGS,
      ...parsed,
      water: { ...DEFAULT_REMINDER_SETTINGS.water, ...(parsed.water || {}) },
      sedentary: { ...DEFAULT_REMINDER_SETTINGS.sedentary, ...(parsed.sedentary || {}) },
      offWork: { ...DEFAULT_REMINDER_SETTINGS.offWork, ...(parsed.offWork || {}) },
    };
  } catch {
    return { ...DEFAULT_REMINDER_SETTINGS };
  }
};

export const writeReminderSettings = async (settings) => {
  try {
    await AsyncStorage.setItem(KEY, JSON.stringify(settings));
  } catch {
    // 本地持久化失败不应阻断使用
  }
};
