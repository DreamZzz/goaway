const noop = () => {};

/** Emergency kill-switch adapter. Used when no provider is configured. */
const NoopAdapter = {
  name: 'noop',
  init: noop,
  identify: noop,
  reset: noop,
  track: noop,
  flush: noop,
};

export default NoopAdapter;
