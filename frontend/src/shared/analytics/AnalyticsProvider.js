import React, { createContext, useContext, useEffect, useMemo, useRef } from 'react';
import { selectAdapter } from './selectAdapter';
import { EVENTS } from './events';

const AnalyticsContext = createContext({
  track: () => {},
  identify: () => {},
  reset: () => {},
});

/**
 * App-root provider that owns a single analytics adapter instance and exposes
 * `useAnalytics()` to feature code. All calls swallow exceptions internally.
 */
export const AnalyticsProvider = ({ children }) => {
  const adapterRef = useRef(null);
  if (!adapterRef.current) {
    adapterRef.current = selectAdapter();
  }

  useEffect(() => {
    try {
      adapterRef.current.init();
    } catch (error) {
       
      console.warn('[analytics] init failed:', error?.message || error);
    }
  }, []);

  const value = useMemo(() => {
    const wrap = (fn, label) => (...args) => {
      try {
        return adapterRef.current[fn](...args);
      } catch (error) {
         
        console.warn(`[analytics] ${label} failed:`, error?.message || error);
        return undefined;
      }
    };
    return {
      track: wrap('track', 'track'),
      identify: wrap('identify', 'identify'),
      reset: wrap('reset', 'reset'),
    };
  }, []);

  return (
    <AnalyticsContext.Provider value={value}>
      {children}
    </AnalyticsContext.Provider>
  );
};

export const useAnalytics = () => useContext(AnalyticsContext);

export { EVENTS };
export default AnalyticsProvider;
