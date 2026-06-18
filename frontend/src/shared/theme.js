// Design system tokens — 今天吃点啥 v2 redesign
// Source: design prototype, matches prototype.html CSS variables

export const colors = {
  // Brand — 暖橘主色
  brand50:  '#FFF4ED',
  brand100: '#FFE5D2',
  brand200: '#FFC8A1',
  brand300: '#FFA468',
  brand400: '#FF8534',
  brand500: '#F26419',  // primary
  brand600: '#D14E0C',
  brand700: '#A93D08',
  brand800: '#7C2D06',

  // Surface — 奶油/米白/暖灰
  bg:      '#FBF7F2',
  bgElev:  '#FFFFFF',
  bgSoft:  '#F4EEE5',
  bgDeep:  '#EBE2D4',

  // Ink — 暖墨
  ink900: '#1F1A14',
  ink700: '#3D352A',
  ink500: '#6B6256',
  ink400: '#8E8576',
  ink300: '#B8AE9D',
  ink200: '#DCD3C2',
  ink100: '#EDE6D8',

  // Semantic
  success: '#4F7A4A',
  warn:    '#D88A1A',
  danger:  '#C8442B',
  info:    '#4A6B8E',

  // Accent
  accentLeaf:  '#6BAA62',
  accentGrain: '#E8B848',
  accentMeat:  '#C8442B',
  accentSoup:  '#6FA8B8',
  accentSpice: '#B83A6E',
  accentVeg:   '#76A04E',

  // Premium/gold
  gold50:  '#FBF3DD',
  gold300: '#E5C46B',
  gold500: '#B8861C',
  gold700: '#6F500C',
};

export const radius = {
  xs:   8,
  sm:   12,
  md:   18,
  lg:   24,
  xl:   32,
  pill: 999,
};

export const spacing = {
  xs: 6,
  sm: 10,
  md: 16,
  lg: 22,
  xl: 32,
};

export const typography = {
  // Display/brand — serif feel
  displayLg: { fontSize: 34, fontWeight: '700', letterSpacing: -0.3, lineHeight: 40 },
  displayMd: { fontSize: 28, fontWeight: '700', letterSpacing: -0.2, lineHeight: 34 },
  displaySm: { fontSize: 22, fontWeight: '700', letterSpacing: -0.1, lineHeight: 28 },

  // Body
  bodyLg:   { fontSize: 16, fontWeight: '400', lineHeight: 24 },
  bodyMd:   { fontSize: 15, fontWeight: '400', lineHeight: 22 },
  bodySm:   { fontSize: 13, fontWeight: '400', lineHeight: 20 },
  bodyXs:   { fontSize: 11, fontWeight: '400', lineHeight: 16 },

  // Label
  labelLg:  { fontSize: 15, fontWeight: '600' },
  labelMd:  { fontSize: 13, fontWeight: '600' },
  labelSm:  { fontSize: 11, fontWeight: '600', letterSpacing: 0.5 },

  // Caption
  caption:  { fontSize: 12, fontWeight: '400', lineHeight: 18 },
};

export const shadows = {
  sm: {
    shadowColor: '#3C1E0A',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.04,
    shadowRadius: 4,
    elevation: 1,
  },
  md: {
    shadowColor: '#3C1E0A',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 12,
    elevation: 3,
  },
  lg: {
    shadowColor: '#3C1E0A',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.12,
    shadowRadius: 24,
    elevation: 6,
  },
  pop: {
    shadowColor: '#F26419',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.28,
    shadowRadius: 18,
    elevation: 8,
  },
};
