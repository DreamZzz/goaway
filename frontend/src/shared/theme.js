// Design system tokens — 狗啊喂 (goaway) · 二次元马卡龙
// 键名沿用旧版以便全局换肤；值改为马卡龙色系。

export const colors = {
  // Brand — 樱花粉主色
  brand50:  '#FFEFF4',
  brand100: '#FFDCE6',
  brand200: '#FFC4D6',
  brand300: '#FFA9C2',
  brand400: '#FF9BB8',
  brand500: '#FF8FB1',  // primary
  brand600: '#F26F97',
  brand700: '#D9587F',
  brand800: '#B5436A',

  // Surface — 奶白带粉 / 通透
  bg:      '#FFF7FB',
  bgElev:  '#FFFFFF',
  bgSoft:  '#F4EDF7',
  bgDeep:  '#EBE1F0',

  // Ink — 柔李灰（替代死黑）
  ink900: '#5A4E63',
  ink700: '#6E6379',
  ink500: '#9A90A6',
  ink400: '#B3AABF',
  ink300: '#C8C0D2',
  ink200: '#E4DBEC',
  ink100: '#F1E9F5',

  // Semantic（马卡龙化）
  success: '#5FC9A8',
  warn:    '#E5B454',
  danger:  '#FF7E8E',
  info:    '#6FB4F0',

  // Accent（马卡龙六色，旧 accent* 键保留兼容）
  sakura:     '#FF8FB1', sakuraSoft: '#FFD3E0',
  mint:       '#7FD8BE', mintSoft:   '#CFF3E7',
  lav:        '#B7A6F0', lavSoft:    '#E4DCFB',
  butter:     '#FFD98A', butterSoft: '#FFEFC6',
  peach:      '#FFB592', peachSoft:  '#FFE0CE',
  sky:        '#9AD0FF', skySoft:    '#D6ECFF',

  accentLeaf:  '#7FD8BE',
  accentGrain: '#FFD98A',
  accentMeat:  '#FF8FB1',
  accentSoup:  '#9AD0FF',
  accentSpice: '#B7A6F0',
  accentVeg:   '#7FD8BE',

  // 深色英雄卡（薪资/成就等）—— 深李紫，配奶油字
  heroDark: '#3A2F49',

  // 奶油强调（沿用 gold* 键）
  gold50:  '#FFF3DD',
  gold300: '#FFD98A',
  gold500: '#E0A93A',
  gold700: '#9A6F12',
};

export const radius = {
  xs:   10,
  sm:   13,
  md:   18,
  lg:   24,
  xl:   30,
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
  // Display/brand — 圆润粗体（接入站酷快乐体后更佳）
  displayLg: { fontSize: 34, fontWeight: '800', letterSpacing: -0.3, lineHeight: 40 },
  displayMd: { fontSize: 28, fontWeight: '800', letterSpacing: -0.2, lineHeight: 34 },
  displaySm: { fontSize: 22, fontWeight: '700', letterSpacing: -0.1, lineHeight: 28 },

  bodyLg:   { fontSize: 16, fontWeight: '400', lineHeight: 24 },
  bodyMd:   { fontSize: 15, fontWeight: '400', lineHeight: 22 },
  bodySm:   { fontSize: 13, fontWeight: '400', lineHeight: 20 },
  bodyXs:   { fontSize: 11, fontWeight: '400', lineHeight: 16 },

  labelLg:  { fontSize: 15, fontWeight: '700' },
  labelMd:  { fontSize: 13, fontWeight: '700' },
  labelSm:  { fontSize: 11, fontWeight: '700', letterSpacing: 0.5 },

  caption:  { fontSize: 12, fontWeight: '400', lineHeight: 18 },
};

// 弥散柔和的彩色软阴影
export const shadows = {
  sm: {
    shadowColor: '#9A7AB8',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.10,
    shadowRadius: 14,
    elevation: 2,
  },
  md: {
    shadowColor: '#9A7AB8',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.14,
    shadowRadius: 22,
    elevation: 4,
  },
  lg: {
    shadowColor: '#9A7AB8',
    shadowOffset: { width: 0, height: 16 },
    shadowOpacity: 0.18,
    shadowRadius: 32,
    elevation: 8,
  },
  pop: {
    shadowColor: '#FF8FB1',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.38,
    shadowRadius: 20,
    elevation: 8,
  },
};
