import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY = 'goaway.badges.celebrated.v1';

// 已弹过中奖动画的徽章档位键集合（badgeKey 形如 water.NPC / 配置勋章 key）
export const readCelebrated = async () => {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    return new Set(raw ? JSON.parse(raw) : []);
  } catch {
    return new Set();
  }
};

export const markCelebrated = async (keys) => {
  try {
    const set = await readCelebrated();
    keys.forEach((k) => set.add(k));
    await AsyncStorage.setItem(KEY, JSON.stringify([...set]));
  } catch {
    // 忽略
  }
};

// award 的稳定键：系列档位 = seriesKey.tier；配置勋章 tier 为空时用 seriesKey
export const awardKey = (a) => (a.tier ? `${a.seriesKey}.${a.tier}` : a.seriesKey);
