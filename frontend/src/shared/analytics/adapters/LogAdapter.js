/**
 * Console logger adapter — default in __DEV__ so engineers can verify wiring
 * without a real SDK.
 */
const noop = () => {};

const LogAdapter = {
  name: 'log',
  init() {
     
    console.log('[analytics] LogAdapter initialized');
  },
  identify(userId) {
     
    console.log('[analytics] identify', userId);
  },
  reset() {
     
    console.log('[analytics] reset');
  },
  track(eventName, properties) {
     
    console.log('[analytics]', eventName, properties || {});
  },
  flush: noop,
};

export default LogAdapter;
