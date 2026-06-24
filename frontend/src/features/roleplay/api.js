import apiClient, { getStoredAuthContext } from '../../shared/api/client';
import { API_BASE_URL } from '../../app/config/api';

export const roleplayAPI = {
  personas: () => apiClient.get('/roleplay/personas'),
  // 毒舌收件箱增量同步（id 游标）
  tauntInbox: (sinceId = 0) => apiClient.get('/taunt/inbox', { params: { sinceId } }),
};

/**
 * 与 AI 角色多轮对话，流式返回回复。RN 0.84 用 XMLHttpRequest.onprogress 解析 SSE。
 * @param persona 角色 code
 * @param messages 历史消息 [{role:'user'|'assistant', content}]（含本轮用户最新消息）
 * @returns {() => void} 取消函数
 */
export const streamRoleplayReply = (persona, messages, { onDelta, onDone, onError, customPersona } = {}) => {
  let cancelled = false;
  const xhr = new XMLHttpRequest();
  let seenLength = 0;
  let buffer = '';

  const dispatch = (rawEvent) => {
    const lines = rawEvent.split('\n');
    let event = 'message';
    const dataLines = [];
    for (const line of lines) {
      if (line.startsWith('event:')) {
        event = line.slice('event:'.length).trim();
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice('data:'.length).replace(/^ /, ''));
      }
    }
    const data = dataLines.join('\n');
    if (event === 'delta') onDelta && onDelta(data);
    else if (event === 'done') onDone && onDone(data);
    else if (event === 'error') onError && onError(data || '对方没有回应');
  };

  const processBuffer = () => {
    let idx;
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const rawEvent = buffer.slice(0, idx);
      buffer = buffer.slice(idx + 2);
      if (rawEvent.trim()) dispatch(rawEvent);
    }
  };

  const onChunk = () => {
    if (cancelled) return;
    const text = xhr.responseText || '';
    if (text.length <= seenLength) return;
    buffer += text.slice(seenLength);
    seenLength = text.length;
    processBuffer();
  };

  xhr.onprogress = onChunk;
  xhr.onload = () => {
    if (cancelled) return;
    onChunk();
    if (xhr.status < 200 || xhr.status >= 300) {
      onError && onError(xhr.status === 401 ? '请先登录' : '对方没有回应');
    }
  };
  xhr.onerror = () => { if (!cancelled) onError && onError('网络异常'); };

  (async () => {
    const { token } = await getStoredAuthContext();
    if (cancelled) return;
    xhr.open('POST', `${API_BASE_URL}/roleplay/chat/stream`);
    xhr.setRequestHeader('Content-Type', 'application/json');
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    xhr.send(JSON.stringify({ persona, messages, customPersona }));
  })();

  return () => {
    cancelled = true;
    try { xhr.abort(); } catch (e) { /* noop */ }
  };
};
