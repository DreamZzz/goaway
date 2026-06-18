import runtimeConfig from '../../app/config/runtime';
import LogAdapter from './adapters/LogAdapter';
import NoopAdapter from './adapters/NoopAdapter';
import createUmengAdapter from './adapters/UmengAdapter';

/**
 * Select the analytics adapter based on runtime config.
 *
 * Resolution order:
 *   1. runtimeConfig.analyticsProvider explicit value
 *   2. __DEV__ → LogAdapter
 *   3. fall back to NoopAdapter
 */
export const selectAdapter = () => {
  const provider = (runtimeConfig.analyticsProvider || '').toLowerCase();

  if (provider === 'umeng') {
    return createUmengAdapter({
      appKey: runtimeConfig.umengAppKey,
      channel: runtimeConfig.umengChannel,
    });
  }
  if (provider === 'log') return LogAdapter;
  if (provider === 'noop') return NoopAdapter;

  if (typeof __DEV__ !== 'undefined' && __DEV__) return LogAdapter;
  return NoopAdapter;
};

export default selectAdapter;
