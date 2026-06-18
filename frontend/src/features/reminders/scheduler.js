import { parseHm } from '../checkin/utils';
import { localNotification } from './native/localNotification';

const MAX_SPECS = 60; // iOS 待发通知上限 64，留余量

const COPY = {
  water: { title: '💧 该喝水啦', body: '起来接杯水，对自己好一点。' },
  sedentary: { title: '🧍 久坐提醒', body: '起来动一动，别和椅子长在一起。' },
  offWork: { title: '🎉 下班时间到', body: '电脑一关，世界与我无关。' },
};

/**
 * 原生触发器是一次性日历触发，这里生成未来 days 天的具体时间点 spec 列表。
 * 应用在启动/回前台时重排，形成滚动窗口。
 */
export const buildReminderSpecs = (settings, now = new Date(), days = 2) => {
  const specs = [];
  const push = (prefix, copy, dt) => {
    if (dt.getTime() <= now.getTime()) return;
    specs.push({
      id: `${prefix}-${dt.getFullYear()}${dt.getMonth() + 1}${dt.getDate()}-${dt.getHours()}${dt.getMinutes()}`,
      title: copy.title,
      body: copy.body,
      year: dt.getFullYear(),
      month: dt.getMonth() + 1,
      day: dt.getDate(),
      hour: dt.getHours(),
      minute: dt.getMinutes(),
      payload: { type: prefix },
    });
  };

  const winStart = parseHm(settings.windowStart);
  const winEnd = parseHm(settings.windowEnd);

  const addInterval = (prefix, copy, intervalMin, offsetMin) => {
    if (!intervalMin || intervalMin < 5) return;
    for (let d = 0; d < days; d++) {
      for (let m = winStart + offsetMin; m <= winEnd; m += intervalMin) {
        const dt = new Date(now);
        dt.setDate(now.getDate() + d);
        dt.setHours(Math.floor(m / 60), m % 60, 0, 0);
        push(prefix, copy, dt);
      }
    }
  };

  if (settings.water?.enabled) {
    addInterval('water', COPY.water, settings.water.intervalMin, settings.water.intervalMin);
  }
  if (settings.sedentary?.enabled) {
    // 与喝水错开半个间隔，避免同一时刻重叠
    addInterval('sedentary', COPY.sedentary, settings.sedentary.intervalMin,
      Math.floor((settings.sedentary.intervalMin || 60) / 2));
  }
  if (settings.offWork?.enabled) {
    const off = parseHm(settings.offWork.time);
    for (let d = 0; d < days; d++) {
      const dt = new Date(now);
      dt.setDate(now.getDate() + d);
      dt.setHours(Math.floor(off / 60), off % 60, 0, 0);
      push('offWork', COPY.offWork, dt);
    }
  }

  // 按时间排序后截断到上限
  specs.sort((a, b) =>
    new Date(a.year, a.month - 1, a.day, a.hour, a.minute) -
    new Date(b.year, b.month - 1, b.day, b.hour, b.minute));
  return specs.slice(0, MAX_SPECS);
};

const anyEnabled = (settings) =>
  !!(settings.water?.enabled || settings.sedentary?.enabled || settings.offWork?.enabled);

/**
 * 按当前设置重排本地通知。返回 { scheduled }。
 */
export const applyReminders = async (settings) => {
  if (!localNotification.isAvailable()) return { scheduled: 0 };
  if (!anyEnabled(settings)) {
    try { await localNotification.cancelAllScheduled(); } catch (e) { /* noop */ }
    return { scheduled: 0 };
  }
  const specs = buildReminderSpecs(settings);
  try {
    return await localNotification.replaceAllScheduled(specs);
  } catch (e) {
    return { scheduled: 0 };
  }
};
