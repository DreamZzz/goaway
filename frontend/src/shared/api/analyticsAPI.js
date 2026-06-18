import apiClient from './client';

/**
 * Fallback uploader for analytics events. Used when the on-device SDK adapter
 * (e.g. Umeng) is unavailable or fails — never called directly by feature code.
 */
export const analyticsAPI = {
  postEvent: (eventName, properties) =>
    apiClient.post('/analytics/events', {
      eventName,
      properties: properties || {},
      occurredAt: new Date().toISOString(),
    }),

  postBatch: (events) =>
    apiClient.post('/analytics/events/batch', {
      events: events.map((e) => ({
        eventName: e.eventName,
        properties: e.properties || {},
        occurredAt: e.occurredAt || new Date().toISOString(),
      })),
    }),
};

export default analyticsAPI;
