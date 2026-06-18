import { Image } from 'react-native';
import { buildImageUrl } from './imageUrl';

export const resolveMediaUrl = (uri) => buildImageUrl(uri);

export const prefetchImage = (url) => {
  if (!url) return Promise.resolve();
  return Image.prefetch(url).catch(() => {});
};
