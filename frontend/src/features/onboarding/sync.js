import { workProfileAPI } from '../profile/workProfileApi';
import { readOnboardingProfile, writeOnboardingProfile, setOnboarded } from './storage';
import { readCheckinSettings, writeCheckinSettings } from '../checkin/storage';

const hasProfileData = (p) =>
  !!(p && (p.nickname || p.industry || p.jobType || p.city || p.gender || p.hatedRelation || p.hatedTraits));

/**
 * 把本地画像（含工时/性别/最讨厌的人）整体上传到账号的 WorkProfile（服务器为事实源）。
 */
export const syncWorkProfileFromLocal = async (profileArg) => {
  try {
    const p = profileArg || (await readOnboardingProfile());
    const checkin = await readCheckinSettings();
    if (!hasProfileData(p)) return;
    await workProfileAPI.update({
      nickname: p.nickname || undefined,
      city: p.city || undefined,
      industry: p.industry || undefined,
      jobType: p.jobType || undefined,
      gender: p.gender || undefined,
      hatedRelation: p.hatedRelation || undefined,
      hatedNickname: p.hatedNickname || undefined,
      hatedTraits: p.hatedTraits || undefined,
      workStart: checkin.workStart || undefined,
      workEnd: checkin.workEnd || undefined,
    });
  } catch {
    // 未登录或网络异常：忽略
  }
};

/**
 * 登录后调用：若服务器已有该账号的画像，则拉回本地并标记已引导（避免重复填写）；
 * 若服务器为空但本地有数据，则把本地推上去。
 * @returns {Promise<boolean>} 服务器是否已有画像（true 表示已可跳过引导）
 */
export const hydrateWorkProfile = async () => {
  try {
    const r = await workProfileAPI.get();
    const s = r.data || {};
    if (hasProfileData(s)) {
      const local = await readOnboardingProfile();
      await writeOnboardingProfile({
        ...local,
        nickname: s.nickname || local.nickname || '',
        city: s.city || local.city || '',
        industry: s.industry || local.industry || '',
        jobType: s.jobType || local.jobType || '',
        gender: s.gender || local.gender || '',
        hatedRelation: s.hatedRelation || local.hatedRelation || '',
        hatedNickname: s.hatedNickname || local.hatedNickname || '',
        hatedTraits: s.hatedTraits || local.hatedTraits || '',
      });
      if (s.workStart || s.workEnd) {
        const checkin = await readCheckinSettings();
        await writeCheckinSettings({
          ...checkin,
          workStart: s.workStart || checkin.workStart,
          workEnd: s.workEnd || checkin.workEnd,
        });
      }
      await setOnboarded();
      return true;
    }
    // 服务器为空：把本地（若有）推上去
    await syncWorkProfileFromLocal();
    return false;
  } catch {
    return false;
  }
};
