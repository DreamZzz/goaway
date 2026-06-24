import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * AI 对线 IM 的本地存储：会话索引 + 每会话消息历史 + 毒舌同步游标。
 * 纯本地（AsyncStorage），是会话历史的事实源；服务端只用于补发「最讨厌的人」会话的毒舌。
 */

const INDEX_KEY = 'goaway.im.index.v1';
const THREAD_KEY = (code) => `goaway.im.thread.${code}.v1`;
const CURSOR_KEY = 'goaway.im.tauntCursor.v1';
const MAX_PER_THREAD = 200;

/** 「最讨厌的人」会话固定 code，毒舌推送统一落到这里。 */
export const HATED_CODE = 'custom';

const genId = () => `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

const safeParse = (raw, fallback) => {
  if (!raw) return fallback;
  try { return JSON.parse(raw); } catch { return fallback; }
};

// ── 会话索引（每条：lastText / lastTs / unread）──

export const readIndex = async () => safeParse(await AsyncStorage.getItem(INDEX_KEY), {});

const writeIndex = async (index) => {
  try { await AsyncStorage.setItem(INDEX_KEY, JSON.stringify(index)); } catch { /* 忽略 */ }
};

export const totalUnread = async () => {
  const index = await readIndex();
  return Object.values(index).reduce((sum, c) => sum + (c?.unread || 0), 0);
};

// ── 单会话消息历史 ──

export const readThread = async (code) => safeParse(await AsyncStorage.getItem(THREAD_KEY(code)), []);

const writeThread = async (code, messages) => {
  const capped = messages.slice(-MAX_PER_THREAD);
  try { await AsyncStorage.setItem(THREAD_KEY(code), JSON.stringify(capped)); } catch { /* 忽略 */ }
  return capped;
};

/**
 * 追加一条消息。incrementUnread=true 时该会话未读 +1（用于用户不在该会话时收到的毒舌）。
 * 返回新消息对象。
 */
export const appendMessage = async (code, { role, content, ts, id }, { incrementUnread = false } = {}) => {
  const message = { id: id || genId(), role, content, ts: ts || Date.now() };
  const thread = await readThread(code);
  await writeThread(code, [...thread, message]);

  const index = await readIndex();
  const prev = index[code] || { unread: 0 };
  index[code] = {
    lastText: content,
    lastTs: message.ts,
    unread: incrementUnread ? (prev.unread || 0) + 1 : (prev.unread || 0),
  };
  await writeIndex(index);
  return message;
};

/** 进入会话时清未读。 */
export const markRead = async (code) => {
  const index = await readIndex();
  if (index[code]) {
    index[code] = { ...index[code], unread: 0 };
    await writeIndex(index);
  }
};

// ── 毒舌同步游标 ──

export const readCursor = async () => {
  const raw = await AsyncStorage.getItem(CURSOR_KEY);
  const n = parseInt(raw, 10);
  return Number.isNaN(n) ? 0 : n;
};

export const writeCursor = async (id) => {
  try { await AsyncStorage.setItem(CURSOR_KEY, String(id)); } catch { /* 忽略 */ }
};

/**
 * 把一条收件箱毒舌写入「最讨厌的人」会话（assistant 未读消息），并推进游标。
 * item: { id, content, sentAt }
 */
export const addIncomingTaunt = async (item) => {
  const ts = item.sentAt ? new Date(item.sentAt).getTime() : Date.now();
  await appendMessage(HATED_CODE, {
    id: `t${item.id}`,
    role: 'assistant',
    content: item.content,
    ts,
  }, { incrementUnread: true });
  await writeCursor(item.id);
};
