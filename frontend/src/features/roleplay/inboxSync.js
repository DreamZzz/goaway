import { roleplayAPI } from './api';
import { readCursor, addIncomingTaunt } from './imStore';

/**
 * 拉取毒舌收件箱（id 游标增量），把新毒舌写入本地「最讨厌的人」会话作未读消息。
 * 在登录后启动、回前台、点毒舌通知时调用。失败静默（下次再同步）。
 * @returns 新增条数
 */
export async function syncTauntInbox() {
  try {
    const since = await readCursor();
    const res = await roleplayAPI.tauntInbox(since);
    const items = res.data || [];
    // 服务端按 id 升序返回；逐条写入并推进游标
    for (const item of items) {
      await addIncomingTaunt(item);
    }
    return items.length;
  } catch {
    return 0;
  }
}
