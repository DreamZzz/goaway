import { buildReminderSpecs } from '../../src/features/reminders/scheduler';

const baseSettings = {
  water: { enabled: false, intervalMin: 60 },
  sedentary: { enabled: false, intervalMin: 60 },
  offWork: { enabled: false, time: '18:00' },
  windowStart: '09:00',
  windowEnd: '18:00',
};

describe('reminder scheduler', () => {
  test('全部关闭时不产生任何 spec', () => {
    const now = new Date(2026, 5, 18, 8, 0, 0);
    expect(buildReminderSpecs(baseSettings, now, 2)).toHaveLength(0);
  });

  test('下班提醒按未来 N 天各生成一条，且跳过已过去的时间', () => {
    const now = new Date(2026, 5, 18, 19, 0, 0); // 19:00，今天 18:00 已过
    const settings = { ...baseSettings, offWork: { enabled: true, time: '18:00' } };
    const specs = buildReminderSpecs(settings, now, 2);
    // 今天已过，只剩明天一条（days=2 窗口内）
    expect(specs.length).toBe(1);
    expect(specs[0].hour).toBe(18);
    expect(specs[0].payload.type).toBe('offWork');
  });

  test('喝水提醒按间隔在工作时段内生成多条', () => {
    const now = new Date(2026, 5, 18, 8, 0, 0); // 上班前
    const settings = { ...baseSettings, water: { enabled: true, intervalMin: 60 } };
    const specs = buildReminderSpecs(settings, now, 1).filter((s) => s.payload.type === 'water');
    // 09:00 起每 60 分钟到 18:00：首个偏移=intervalMin → 10:00,11:00,...,18:00
    expect(specs.length).toBeGreaterThan(3);
    specs.forEach((s) => {
      expect(s.month).toBe(6); // 1-based
      expect(s.hour).toBeGreaterThanOrEqual(9);
      expect(s.hour).toBeLessThanOrEqual(18);
    });
  });

  test('spec 总数不超过 60 上限', () => {
    const now = new Date(2026, 5, 18, 0, 0, 0);
    const settings = {
      ...baseSettings,
      water: { enabled: true, intervalMin: 5 },
      sedentary: { enabled: true, intervalMin: 5 },
      offWork: { enabled: true, time: '18:00' },
      windowStart: '00:00',
      windowEnd: '23:59',
    };
    expect(buildReminderSpecs(settings, now, 7).length).toBeLessThanOrEqual(60);
  });
});
