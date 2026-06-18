/**
 * 打卡场景的纯计算：上班进度、今日已赚薪资、距发薪/周末天数。
 * 全部本地计算，游客也可用。函数保持纯净以便测试。
 */

export const DEFAULT_CHECKIN_SETTINGS = {
  workStart: '09:00',
  workEnd: '18:00',
  monthlySalary: 10000,
  workDaysPerMonth: 22,
  paydayDay: 15,
};

export const parseHm = (hm) => {
  const [h, m] = String(hm || '0:0').split(':').map((x) => parseInt(x, 10) || 0);
  return h * 60 + m;
};

const pad2 = (n) => String(n).padStart(2, '0');

export const formatDuration = (totalSeconds) => {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  if (h > 0) return `${h}时${pad2(m)}分`;
  if (m > 0) return `${m}分${pad2(sec)}秒`;
  return `${sec}秒`;
};

export const formatMoney = (value) => {
  const v = Number(value) || 0;
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

/**
 * 计算上班状态与今日薪资进度。
 * @returns {{phase, phaseLabel, countdownSeconds, countdownLabel, dailySalary, perSecond, todayEarned, progressPct}}
 */
export const computeWorkDashboard = (settings, now = new Date()) => {
  const s = { ...DEFAULT_CHECKIN_SETTINGS, ...(settings || {}) };
  const startMin = parseHm(s.workStart);
  const endMin = parseHm(s.workEnd);
  const nowMin = now.getHours() * 60 + now.getMinutes() + now.getSeconds() / 60;
  const workMinutes = Math.max(1, endMin - startMin);

  const monthly = Number(s.monthlySalary) || 0;
  const workDays = Math.max(1, Number(s.workDaysPerMonth) || 1);
  const dailySalary = monthly / workDays;
  const perSecond = dailySalary / (workMinutes * 60);

  let phase;
  let phaseLabel;
  let countdownSeconds = 0;
  let countdownLabel = '';
  let workedMinutes;

  if (nowMin < startMin) {
    phase = 'before';
    phaseLabel = '距上班';
    countdownSeconds = Math.round((startMin - nowMin) * 60);
    countdownLabel = formatDuration(countdownSeconds);
    workedMinutes = 0;
  } else if (nowMin >= endMin) {
    phase = 'after';
    phaseLabel = '已下班';
    countdownLabel = '今天辛苦啦';
    workedMinutes = workMinutes;
  } else {
    phase = 'working';
    phaseLabel = '距下班';
    countdownSeconds = Math.round((endMin - nowMin) * 60);
    countdownLabel = formatDuration(countdownSeconds);
    workedMinutes = nowMin - startMin;
  }

  const progressPct = Math.max(0, Math.min(1, workedMinutes / workMinutes));
  const todayEarned = perSecond * workedMinutes * 60;

  return {
    phase,
    phaseLabel,
    countdownSeconds,
    countdownLabel,
    dailySalary,
    perSecond,
    todayEarned,
    progressPct,
  };
};

/** 距下一个发薪日的天数（含当天为 0）。 */
export const daysUntilPayday = (settings, now = new Date()) => {
  const s = { ...DEFAULT_CHECKIN_SETTINGS, ...(settings || {}) };
  const day = Math.min(28, Math.max(1, Number(s.paydayDay) || 1));
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  let next = new Date(now.getFullYear(), now.getMonth(), day);
  if (next < today) {
    next = new Date(now.getFullYear(), now.getMonth() + 1, day);
  }
  return Math.round((next - today) / 86400000);
};

/** 距周末（本周六）的天数，周六/周日返回 0。 */
export const daysUntilWeekend = (now = new Date()) => {
  const dow = now.getDay(); // 0=周日 .. 6=周六
  if (dow === 0 || dow === 6) return 0;
  return 6 - dow;
};
