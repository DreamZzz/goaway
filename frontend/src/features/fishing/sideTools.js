import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY = 'goaway.fishing.sidetools.v1';

const todayStr = () => {
  const d = new Date();
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
};

const EMPTY = { water: 0, smoke: 0, poopCount: 0, poopSeconds: 0 };

/** 读取今日子功能计数（跨天自动归零）。 */
export const readSideTools = async () => {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    if (!raw) return { ...EMPTY };
    const parsed = JSON.parse(raw);
    if (parsed.date !== todayStr()) return { ...EMPTY };
    return { ...EMPTY, ...parsed };
  } catch {
    return { ...EMPTY };
  }
};

const persist = async (data) => {
  try {
    await AsyncStorage.setItem(KEY, JSON.stringify({ ...data, date: todayStr() }));
  } catch {
    // 忽略持久化失败
  }
};

/** 计数 +1（water | smoke），返回新值对象。 */
export const bumpSideTool = async (key) => {
  const cur = await readSideTools();
  const next = { ...cur, [key]: (cur[key] || 0) + 1 };
  await persist(next);
  return next;
};

/** 记录一次带薪拉屎（累加次数与时长）。 */
export const addPoopSession = async (seconds) => {
  const cur = await readSideTools();
  const next = {
    ...cur,
    poopCount: (cur.poopCount || 0) + 1,
    poopSeconds: (cur.poopSeconds || 0) + Math.max(0, Math.floor(seconds)),
  };
  await persist(next);
  return next;
};
