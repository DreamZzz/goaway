import { colors } from '../../shared/theme';

// 5 档稀有度配色（colorKey 来自后端 BadgeTier.colorKey）
const TIER_STYLE = {
  gray: { color: colors.ink400, soft: colors.bgSoft, glow: 'rgba(154,144,166,0.35)' },     // 拉
  mint: { color: colors.mint, soft: colors.mintSoft, glow: 'rgba(127,216,190,0.45)' },      // NPC
  lav: { color: colors.lav, soft: colors.lavSoft, glow: 'rgba(183,166,240,0.5)' },          // 人上人
  gold: { color: colors.gold300, soft: colors.gold50, glow: 'rgba(255,217,138,0.6)' },      // 顶级
  sakura: { color: colors.brand500, soft: colors.sakuraSoft, glow: 'rgba(255,143,177,0.6)' }, // 夯
};

export const tierStyle = (colorKey) => TIER_STYLE[colorKey] || TIER_STYLE.gray;

// 时长/次数格式化（与排行榜一致口径）
export const formatMetric = (value, unit) => {
  if (unit === 'seconds') {
    const h = Math.floor(value / 3600);
    const m = Math.floor((value % 3600) / 60);
    if (h > 0) return m > 0 ? `${h}小时${m}分` : `${h}小时`;
    if (m > 0) return `${m}分`;
    return `${value}秒`;
  }
  return `${value}`;
};

export const formatDate = (iso) => {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
};
