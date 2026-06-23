import { pushAPI } from './api';
import { remotePush } from './native/remotePush';

let started = false;
let tokenSub = null;
let tapSub = null;

/**
 * 启动远程推送：注册 token 上报 + 通知点击深链 + 申请权限并向 APNs 注册。
 * 仅在已登录后调用。重复调用安全（只装一次监听）。
 * @param onTap (payload) => void  通知点击回调，用于导航深链
 */
export async function setupRemotePush(onTap) {
  if (!remotePush.isAvailable()) return;

  if (!started) {
    started = true;
    tokenSub = remotePush.addTokenListener(async ({ token }) => {
      if (!token) return;
      try { await pushAPI.registerDevice(token, 'ios'); } catch (e) { /* 下次启动重试 */ }
    });
    tapSub = remotePush.addTapListener((payload) => {
      if (payload && typeof onTap === 'function') onTap(payload);
    });

    // 冷启动由点击通知拉起：处理初始通知
    try {
      const initial = await remotePush.getInitialNotification();
      if (initial && typeof onTap === 'function') onTap(initial);
    } catch (e) { /* noop */ }
  }

  try { await remotePush.requestPermissionAndRegister(); } catch (e) { /* 用户拒绝/无权限 */ }
}

export function teardownRemotePush() {
  if (tokenSub) { tokenSub.remove(); tokenSub = null; }
  if (tapSub) { tapSub.remove(); tapSub = null; }
  started = false;
}

/** 活跃心跳（App 回前台时调用），喂给不活跃召回判定。失败静默。 */
export async function reportActive() {
  try { await pushAPI.markActive(); } catch (e) { /* noop */ }
}
