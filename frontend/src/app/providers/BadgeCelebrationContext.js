import React, { createContext, useCallback, useContext, useState } from 'react';
import BadgeUnlockModal from '../../shared/components/BadgeUnlockModal';
import { markCelebrated, awardKey } from '../../features/badges/storage';

const CelebrationContext = createContext({ celebrate: () => {} });

export const useCelebration = () => useContext(CelebrationContext);

/**
 * 全局勋章中奖动画：celebrate(awards) 入队，逐个弹出。
 * 入队即标记「已弹」，避免进勋章页补未读时重复。
 */
export function BadgeCelebrationProvider({ children }) {
  const [queue, setQueue] = useState([]);

  const celebrate = useCallback((awards) => {
    if (!Array.isArray(awards) || awards.length === 0) return;
    setQueue((q) => [...q, ...awards]);
    markCelebrated(awards.map(awardKey));
  }, []);

  const close = useCallback(() => setQueue((q) => q.slice(1)), []);

  return (
    <CelebrationContext.Provider value={{ celebrate }}>
      {children}
      <BadgeUnlockModal award={queue[0] || null} onClose={close} />
    </CelebrationContext.Provider>
  );
}
