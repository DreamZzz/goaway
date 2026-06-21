import apiClient, { getStoredAuthContext } from '../../shared/api/client';
import { API_BASE_URL } from '../../app/config/api';

export const weeklyAPI = {
  list: () => apiClient.get('/weekly/reports'),
  get: (id) => apiClient.get(`/weekly/reports/${id}`),
};

/**
 * SSE 流式生成周报。RN 0.84 下 fetch+ReadableStream 不可靠，使用 XMLHttpRequest.onprogress
 * 增量解析 text/event-stream。
 * @returns {() => void} 取消函数
 */
export const streamWeeklyReport = ({ onDelta, onDone, onError } = {}) => {
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
        // 去掉 "data:" 前缀，保留其余原文（不 trim，避免吃掉有意义空白）
        dataLines.push(line.slice('data:'.length).replace(/^ /, ''));
      }
    }
    const data = dataLines.join('\n');
    if (event === 'delta') {
      onDelta && onDelta(data);
    } else if (event === 'done') {
      onDone && onDone(data);
    } else if (event === 'error') {
      onError && onError(data || '生成失败');
    }
  };

  const processBuffer = () => {
    let idx;
    // 事件以空行（\n\n）分隔
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const rawEvent = buffer.slice(0, idx);
      buffer = buffer.slice(idx + 2);
      if (rawEvent.trim()) {
        dispatch(rawEvent);
      }
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
      onError && onError(xhr.status === 401 ? '请先登录' : '生成失败');
    }
  };
  xhr.onerror = () => { if (!cancelled) onError && onError('网络异常'); };

  (async () => {
    const { token } = await getStoredAuthContext();
    if (cancelled) return;
    xhr.open('POST', `${API_BASE_URL}/weekly/reports/stream`);
    xhr.setRequestHeader('Content-Type', 'application/json');
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
    xhr.send(JSON.stringify({}));
  })();

  return () => {
    cancelled = true;
    try { xhr.abort(); } catch (e) { /* noop */ }
  };
};
