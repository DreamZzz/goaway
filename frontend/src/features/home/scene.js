import { parseHm } from '../checkin/utils';

/**
 * 根据系统时间 + 打卡工时设置判断当前场景。
 * 周末整天视为「下班后」。
 * @returns {'before'|'during'|'after'}
 */
export const computeScene = (settings, now = new Date()) => {
  const dow = now.getDay(); // 0=周日 6=周六
  if (dow === 0 || dow === 6) return 'after';

  const startMin = parseHm(settings?.workStart || '09:00');
  const endMin = parseHm(settings?.workEnd || '18:00');
  const nowMin = now.getHours() * 60 + now.getMinutes();

  if (nowMin < startMin) return 'before';
  if (nowMin >= endMin) return 'after';
  return 'during';
};

export const SCENES = [
  { key: 'before', label: '上班前' },
  { key: 'during', label: '上班中' },
  { key: 'after', label: '下班后' },
];
