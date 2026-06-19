import AsyncStorage from '@react-native-async-storage/async-storage';

const PROFILE_KEY = 'goaway.onboarding.profile.v1';
const DONE_KEY = 'goaway.onboarding.done.v1';

export const DEFAULT_ONBOARDING_PROFILE = {
  nickname: '',
  industry: '',
  jobType: '',
  gender: '',
  city: '',
  hatedRelation: '',
  hatedTraits: '',
  hatedNickname: '',
};

export const readOnboardingProfile = async () => {
  try {
    const raw = await AsyncStorage.getItem(PROFILE_KEY);
    if (!raw) return { ...DEFAULT_ONBOARDING_PROFILE };
    return { ...DEFAULT_ONBOARDING_PROFILE, ...JSON.parse(raw) };
  } catch {
    return { ...DEFAULT_ONBOARDING_PROFILE };
  }
};

export const writeOnboardingProfile = async (profile) => {
  try {
    await AsyncStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
  } catch {
    // 忽略持久化失败
  }
};

export const isOnboarded = async () => {
  try {
    return (await AsyncStorage.getItem(DONE_KEY)) === '1';
  } catch {
    return false;
  }
};

export const setOnboarded = async () => {
  try {
    await AsyncStorage.setItem(DONE_KEY, '1');
  } catch {
    // 忽略
  }
};

/**
 * 由「最讨厌的人」生成自定义对线角色描述（供 roleplay customPersona 使用）。
 * 没填则返回 null。
 */
export const buildHatedPersona = (profile) => {
  if (!profile || (!profile.hatedRelation && !profile.hatedTraits)) return null;
  const relation = profile.hatedRelation || '某个人';
  const name = profile.hatedNickname ? `（代号「${profile.hatedNickname}」）` : '';
  const traits = profile.hatedTraits ? `，特征：${profile.hatedTraits}` : '';
  return {
    nickname: profile.hatedNickname || `我最讨厌的${relation}`,
    description: `我最讨厌的${relation}${name}${traits}`,
  };
};
