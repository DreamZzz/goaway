import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { totalUnread } from '../../features/roleplay/imStore';

const UnreadContext = createContext({ unread: 0, refreshUnread: () => {} });

export const useUnread = () => useContext(UnreadContext);

/**
 * 全局未读总数（AI 对线会话）。同步收件箱 / 进会话标记已读后调用 refreshUnread 刷新，
 * 驱动首页入口红点与会话列表角标。
 */
export const UnreadProvider = ({ children }) => {
  const [unread, setUnread] = useState(0);

  const refreshUnread = useCallback(async () => {
    try { setUnread(await totalUnread()); } catch { /* 忽略 */ }
  }, []);

  useEffect(() => { refreshUnread(); }, [refreshUnread]);

  return (
    <UnreadContext.Provider value={{ unread, refreshUnread }}>
      {children}
    </UnreadContext.Provider>
  );
};
