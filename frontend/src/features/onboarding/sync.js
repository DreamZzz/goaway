import { workProfileAPI } from '../profile/workProfileApi';
import { readOnboardingProfile } from './storage';

/**
 * 登录态下把本地画像（昵称/城市/行业/职业）同步到后端 WorkProfile（用于排行榜切片）。
 * 幂等，失败静默。
 */
export const syncWorkProfileFromLocal = async (profileArg) => {
  try {
    const p = profileArg || (await readOnboardingProfile());
    if (!p.nickname && !p.industry && !p.jobType && !p.city) return;
    await workProfileAPI.update({
      nickname: p.nickname || undefined,
      city: p.city || undefined,
      industry: p.industry || undefined,
      jobType: p.jobType || undefined,
    });
  } catch {
    // 未登录或网络异常：忽略，下次进入再同步
  }
};
