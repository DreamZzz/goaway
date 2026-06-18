import {
  computeWorkDashboard,
  daysUntilPayday,
  daysUntilWeekend,
  parseHm,
  formatDuration,
} from '../../src/features/checkin/utils';

describe('checkin utils', () => {
  const settings = {
    workStart: '09:00',
    workEnd: '18:00',
    monthlySalary: 22000,
    workDaysPerMonth: 22,
    paydayDay: 15,
  };

  test('parseHm 解析时分为分钟', () => {
    expect(parseHm('09:30')).toBe(570);
    expect(parseHm('00:00')).toBe(0);
  });

  test('上班前阶段已赚为 0', () => {
    const before = new Date(2026, 0, 5, 7, 0, 0); // 07:00
    const d = computeWorkDashboard(settings, before);
    expect(d.phase).toBe('before');
    expect(d.todayEarned).toBe(0);
    expect(d.progressPct).toBe(0);
  });

  test('下班后已赚为满勤日薪', () => {
    const after = new Date(2026, 0, 5, 20, 0, 0); // 20:00
    const d = computeWorkDashboard(settings, after);
    expect(d.phase).toBe('after');
    // 日薪 = 22000 / 22 = 1000
    expect(Math.round(d.dailySalary)).toBe(1000);
    expect(Math.round(d.todayEarned)).toBe(1000);
    expect(d.progressPct).toBe(1);
  });

  test('工作中进度约为一半', () => {
    const mid = new Date(2026, 0, 5, 13, 30, 0); // 13:30，工时 9h 的中点
    const d = computeWorkDashboard(settings, mid);
    expect(d.phase).toBe('working');
    expect(d.progressPct).toBeCloseTo(0.5, 1);
  });

  test('daysUntilPayday 当月未到发薪日', () => {
    const now = new Date(2026, 0, 10); // 10 号，发薪 15 号
    expect(daysUntilPayday(settings, now)).toBe(5);
  });

  test('daysUntilWeekend 周三返回 3', () => {
    const wed = new Date(2026, 0, 7); // 2026-01-07 是周三
    expect(daysUntilWeekend(wed)).toBe(3);
  });

  test('formatDuration 友好格式', () => {
    expect(formatDuration(65)).toBe('1分05秒');
    expect(formatDuration(3661)).toBe('1时01分');
  });
});
