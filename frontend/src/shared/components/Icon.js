import React from 'react';
import Svg, { Path, Circle, Ellipse, Rect, G } from 'react-native-svg';

// 马卡龙图标色（与 theme 一致，react-native-svg 用字面色值）
const P = {
  sakura: '#FF8FB1', mint: '#7FD8BE', lav: '#B7A6F0', butter: '#FFD98A',
  peach: '#FFB592', sky: '#9AD0FF', ink: '#5A4E63', inkFaint: '#C8C0D2',
  blush: '#FFD3E0', w: '#FFFFFF',
};
// 边牧炭灰三色
const C = { a: '#4C4357', b: '#463E52', c: '#3F3749' };

const GLYPHS = {
  fish: (
    <>
      <Path d="M30 16c-2.4 3.2-3.8 4.6-6.2 4.6.9-2.9.9-6.3 0-9.2 2.4 0 3.8 1.4 6.2 4.6Z" fill={P.mint} />
      <Path d="M22.5 16c0 5-4.6 8.4-10.4 8.4C6.6 24.4 2 21 2 16s4.6-8.4 10.1-8.4C17.9 7.6 22.5 11 22.5 16Z" fill={P.mint} />
      <Circle cx={9} cy={14.4} r={2} fill={P.w} /><Circle cx={8.6} cy={14.4} r={1} fill={P.ink} />
      <Circle cx={14} cy={19} r={1.5} fill={P.w} opacity={0.6} />
    </>
  ),
  toilet: (
    <>
      <Path d="M6 6.5C6 5.1 7.1 4 8.5 4h3C12.9 4 14 5.1 14 6.5V14H6V6.5Z" fill={P.inkFaint} />
      <Path d="M5 13.5h22c0 5-3 8.6-7.4 9.7L19 28h-6l-.6-4.8C8 22.1 5 18.5 5 13.5Z" fill={P.peach} />
      <Ellipse cx={16} cy={14.5} rx={9} ry={2.4} fill={P.w} />
    </>
  ),
  water: (
    <>
      <Path d="M16 3c4 5 7 8.5 7 12.2A7 7 0 1 1 9 15.2C9 11.5 12 8 16 3Z" fill={P.sky} />
      <Path d="M12.5 15.5c0 2 1.6 3.5 3.5 3.5" stroke={P.w} strokeWidth={2.2} strokeLinecap="round" fill="none" opacity={0.85} />
    </>
  ),
  smoke: (
    <>
      <Rect x={3} y={17} width={20} height={7} rx={3.5} fill={P.lav} />
      <Rect x={18} y={17} width={5} height={7} rx={2.5} fill={P.w} opacity={0.7} />
      <Path d="M22 13c2-1.2 2-3.2 0-4.4M26 14c2-1.4 2-3.8 0-5.2" stroke={P.inkFaint} strokeWidth={2} strokeLinecap="round" fill="none" />
    </>
  ),
  trophy: (
    <>
      <Path d="M9 5h14v6a7 7 0 0 1-14 0V5Z" fill={P.butter} />
      <Path d="M9 7H5.5C5 7 5 10.5 9 11M23 7h3.5c.5 0 .5 3.5-3.5 4" stroke={P.peach} strokeWidth={2.2} fill="none" strokeLinecap="round" />
      <Rect x={13} y={17} width={6} height={6} rx={1.5} fill={P.peach} />
      <Rect x={9} y={23} width={14} height={4} rx={2} fill={P.peach} />
      <Circle cx={16} cy={9} r={2} fill={P.w} opacity={0.7} />
    </>
  ),
  soup: (
    <>
      <Path d="M4 14h24c0 6-5.4 11-12 11S4 20 4 14Z" fill={P.sakura} />
      <Ellipse cx={16} cy={14} rx={12} ry={2.6} fill={P.w} opacity={0.55} />
      <Path d="M13 9c-1.4-1.2-1.4-2.8 0-4M19 9c-1.4-1.2-1.4-2.8 0-4" stroke={P.w} strokeWidth={2} strokeLinecap="round" fill="none" opacity={0.8} />
    </>
  ),
  bell: (
    <>
      <Path d="M7 22c2-1.4 2-4 2-8a7 7 0 0 1 14 0c0 4 0 6.6 2 8H7Z" fill={P.butter} />
      <Path d="M13 24a3 3 0 0 0 6 0" fill={P.peach} />
      <Circle cx={16} cy={5} r={2} fill={P.peach} />
    </>
  ),
  doc: (
    <>
      <Path d="M7 5.5C7 4.7 7.7 4 8.5 4H19l6 6v16.5c0 .8-.7 1.5-1.5 1.5h-15C7.7 28 7 27.3 7 26.5v-21Z" fill={P.sky} />
      <Path d="M19 4l6 6h-4.5C19.7 10 19 9.3 19 8.5V4Z" fill={P.w} opacity={0.7} />
      <Path d="M11 15h10M11 19h10M11 23h6" stroke={P.w} strokeWidth={2} strokeLinecap="round" />
    </>
  ),
  roast: (
    <>
      <Path d="M4 8.5C4 7.1 5.1 6 6.5 6h19C26.9 6 28 7.1 28 8.5V19c0 1.4-1.1 2.5-2.5 2.5H12l-6 4.5V8.5Z" fill={P.sakura} />
      <Path d="M12 11l8 6M20 11l-8 6" stroke={P.w} strokeWidth={2.4} strokeLinecap="round" />
    </>
  ),
  fortune: (
    <>
      <Circle cx={16} cy={14} r={10} fill={P.lav} />
      <Path d="M8 25h16c0 1.7-3.6 3-8 3s-8-1.3-8-3Z" fill={P.peach} />
      <Path d="M12 10l1.2 2.6L16 14l-2.8 1.4L12 18l-1.2-2.6L8 14l2.8-1.4L12 10Z" fill={P.w} opacity={0.85} />
      <Circle cx={20} cy={17} r={1.6} fill={P.w} opacity={0.7} />
    </>
  ),
  coin: (
    <>
      <Circle cx={16} cy={16} r={12} fill={P.butter} />
      <Circle cx={16} cy={16} r={8.5} fill={P.w} opacity={0.55} />
      <Path d="M16 10v12M13 13h4.2a2 2 0 0 1 0 4H13h4.2a2 2 0 0 1 0 4H13" stroke={P.peach} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" fill="none" />
    </>
  ),
  gear: (
    <>
      <Path d="M16 3.5c1.1 0 2 .7 2.3 1.7l.3 1a10 10 0 0 1 2 1.1l1-.4c1-.4 2.2 0 2.8 1l.5.8c.6 1 .4 2.2-.4 2.9l-.8.7c.1.7.1 1.5 0 2.2l.8.7c.8.7 1 1.9.4 2.9l-.5.8c-.6 1-1.8 1.4-2.8 1l-1-.4a10 10 0 0 1-2 1.1l-.3 1c-.3 1-1.2 1.7-2.3 1.7s-2-.7-2.3-1.7l-.3-1a10 10 0 0 1-2-1.1l-1 .4c-1 .4-2.2 0-2.8-1l-.5-.8c-.6-1-.4-2.2.4-2.9l.8-.7a10 10 0 0 1 0-2.2l-.8-.7c-.8-.7-1-1.9-.4-2.9l.5-.8c.6-1 1.8-1.4 2.8-1l1 .4a10 10 0 0 1 2-1.1l.3-1C14 4.2 14.9 3.5 16 3.5Z" fill={P.lav} />
      <Circle cx={16} cy={16} r={4} fill={P.w} />
    </>
  ),
};

// 边牧吉祥物（独立 viewBox 120）
const Mascot = ({ size = 120, style }) => (
  <Svg width={size} height={size} viewBox="0 0 120 120" style={style}>
    <Ellipse cx={58} cy={107} rx={30} ry={6} fill="rgba(90,78,99,0.12)" />
    <G rotation={22} originX={60} originY={64}>
      <Path d="M35 43c-13-2-21 9-20 26 1 9 6 14 12 12 6-3 8-12 9-23 .6-9 .5-14-1-15Z" fill={C.b} />
      <Path d="M85 43c13-2 21 9 20 26-1 9-6 14-12 12-6-3-8-12-9-23-.6-9-.5-14 1-15Z" fill={C.a} />
      <Path d="M30 55C30 36 43 30 60 30s30 6 30 25c0 12-4 20-10 26-5 5-11 11-20 11s-15-6-20-11c-6-6-10-14-10-26Z" fill={P.w} />
      <Path d="M30 62l-6 1 5 3-6 2 6 2-4 3" fill="none" stroke={P.w} strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" />
      <Path d="M90 62l6 1-5 3 6 2-6 2 4 3" fill="none" stroke={P.w} strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" />
      <Path d="M30 55C30 36 43 30 60 30s30 6 30 25l-5-4-4 5-5-4-4 5-5-4-4 5-4-5-4 5-5-4-4 5-5-4-4 5Z" fill={C.a} />
      <Path d="M60 32c5 10 5 24 1 35-1 2-2 2-3 0-4-11-3-25 2-35Z" fill={P.w} />
      <Ellipse cx={60} cy={79} rx={5} ry={3.8} fill={C.c} />
      <Path d="M60 82.8v3.6M51 89q9 3.5 18 0" stroke={C.c} strokeWidth={2} strokeLinecap="round" fill="none" />
      <Path d="M47 80c-7 1-13 0-17-3M48 84c-7 2-13 3-18 1M73 80c7 1 13 0 17-3M72 84c7 2 13 3 18 1" stroke={P.inkFaint} strokeWidth={1.3} strokeLinecap="round" fill="none" />
      <Path d="M42 51l11 2" stroke={P.w} strokeWidth={2.6} strokeLinecap="round" />
      <Path d="M78 49l-11 3.5" stroke={P.w} strokeWidth={2.6} strokeLinecap="round" />
      <Ellipse cx={48} cy={62} rx={5.2} ry={4.9} fill={P.w} stroke={C.c} strokeWidth={1.3} />
      <Ellipse cx={70} cy={60} rx={5.2} ry={4.9} fill={P.w} stroke={C.c} strokeWidth={1.3} />
      <Path d="M43 60h10.4M65 58h10.4" stroke={C.c} strokeWidth={3} strokeLinecap="round" />
      <Circle cx={51.4} cy={60} r={2.4} fill={C.c} /><Circle cx={73.4} cy={58} r={2.4} fill={C.c} />
      <Ellipse cx={40} cy={71} rx={5} ry={3.2} fill={P.blush} /><Ellipse cx={80} cy={71} rx={5} ry={3.2} fill={P.blush} />
    </G>
  </Svg>
);

/**
 * 统一图标组件。name='mascot' 渲染边牧；name='home' 单色（用 color）；其余为马卡龙双色。
 */
const Icon = ({ name, size = 24, color = '#C8C0D2', style }) => {
  if (name === 'mascot') return <Mascot size={size} style={style} />;
  if (name === 'home') {
    return (
      <Svg width={size} height={size} viewBox="0 0 32 32" style={style}>
        <Path d="M5 15 16 5l11 10v11.5c0 .8-.7 1.5-1.5 1.5h-6V20h-7v8h-6C5.7 28 5 27.3 5 26.5V15Z" fill={color} />
      </Svg>
    );
  }
  const glyph = GLYPHS[name];
  if (!glyph) return null;
  return (
    <Svg width={size} height={size} viewBox="0 0 32 32" style={style}>{glyph}</Svg>
  );
};

export default Icon;
export { Mascot };
