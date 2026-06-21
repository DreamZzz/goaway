import React, { useEffect, useRef } from 'react';
import { Animated, Easing, Modal, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import Svg, { Line, Circle as SvgCircle } from 'react-native-svg';
import GIcon from './Icon';
import { colors, radius, shadows } from '../theme';
import { tierStyle, formatMetric, formatDate } from '../../features/badges/tiers';

const AnimatedView = Animated.View;

// 预计算 12 道光芒线（中心 50,50，内 r24 外 r48）
const RAYS = Array.from({ length: 12 }, (_, i) => {
  const a = (i * 30 * Math.PI) / 180;
  return {
    x1: 50 + 24 * Math.cos(a), y1: 50 + 24 * Math.sin(a),
    x2: 50 + 48 * Math.cos(a), y2: 50 + 48 * Math.sin(a),
  };
});

const BadgeUnlockModal = ({ award, onClose }) => {
  const backdrop = useRef(new Animated.Value(0)).current;
  const pop = useRef(new Animated.Value(0)).current;
  const spin = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    backdrop.setValue(0); pop.setValue(0); spin.setValue(0);
    Animated.parallel([
      Animated.timing(backdrop, { toValue: 1, duration: 220, useNativeDriver: true }),
      Animated.spring(pop, { toValue: 1, friction: 5, tension: 80, useNativeDriver: true }),
    ]).start();
    const loop = Animated.loop(
      Animated.timing(spin, { toValue: 1, duration: 6000, easing: Easing.linear, useNativeDriver: true })
    );
    loop.start();
    return () => loop.stop();
  }, [award, backdrop, pop, spin]);

  if (!award) return null;
  const st = tierStyle(award.colorKey);
  const rotate = spin.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });
  const headline = award.promotion ? '晋级！' : '解锁！';
  const subtitle = award.tierLabel
    ? `${award.seriesTitle} · ${award.tierLabel}`
    : award.seriesTitle;

  return (
    <Modal transparent animationType="none" visible onRequestClose={onClose}>
      <AnimatedView style={[styles.backdrop, { opacity: backdrop }]}>
        <AnimatedView style={[styles.card, {
          transform: [{ scale: pop.interpolate({ inputRange: [0, 1], outputRange: [0.6, 1] }) }],
          opacity: pop,
        }]}>
          <Text style={[styles.headline, { color: st.color }]}>{headline}</Text>

          <View style={styles.medalWrap}>
            <AnimatedView style={[styles.rays, { transform: [{ rotate }] }]}>
              <Svg width={220} height={220} viewBox="0 0 100 100">
                {RAYS.map((r, i) => (
                  <Line key={i} x1={r.x1} y1={r.y1} x2={r.x2} y2={r.y2}
                    stroke={st.color} strokeWidth={2.4} strokeLinecap="round" opacity={0.5} />
                ))}
                <SvgCircle cx={50} cy={50} r={20} fill={st.glow} />
              </Svg>
            </AnimatedView>
            <View style={[styles.disc, { backgroundColor: st.soft, borderColor: st.color }]}>
              <GIcon name={award.icon || 'trophy'} size={56} />
            </View>
            <View style={[styles.tierPill, { backgroundColor: st.color }]}>
              <Text style={styles.tierPillText}>{award.tierLabel || '成就'}</Text>
            </View>
          </View>

          <Text style={styles.subtitle}>{subtitle}</Text>
          {award.threshold > 0 && (
            <Text style={styles.detail}>
              达成 {formatMetric(award.threshold, award.unit)}{award.unit === 'count' ? '次' : ''}
            </Text>
          )}
          <Text style={styles.date}>{formatDate(award.earnedAt)}</Text>

          <TouchableOpacity style={[styles.btn, { backgroundColor: st.color }]} onPress={onClose} activeOpacity={0.85}>
            <Text style={styles.btnText}>收下啦</Text>
          </TouchableOpacity>
        </AnimatedView>
      </AnimatedView>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(58,47,73,0.55)', alignItems: 'center', justifyContent: 'center', padding: 32 },
  card: {
    width: '100%', maxWidth: 320, backgroundColor: colors.bgElev, borderRadius: 28,
    alignItems: 'center', paddingTop: 24, paddingBottom: 22, paddingHorizontal: 22, ...shadows.pop,
  },
  headline: { fontSize: 30, fontWeight: '900', letterSpacing: 1, marginBottom: 4 },
  medalWrap: { width: 220, height: 220, alignItems: 'center', justifyContent: 'center' },
  rays: { position: 'absolute', width: 220, height: 220, alignItems: 'center', justifyContent: 'center' },
  disc: {
    width: 110, height: 110, borderRadius: 55, borderWidth: 3, alignItems: 'center', justifyContent: 'center',
    ...shadows.sm,
  },
  tierPill: { position: 'absolute', bottom: 28, paddingHorizontal: 14, paddingVertical: 4, borderRadius: radius.pill, ...shadows.sm },
  tierPillText: { color: '#fff', fontSize: 13, fontWeight: '800', letterSpacing: 1 },
  subtitle: { fontSize: 17, fontWeight: '800', color: colors.ink900, marginTop: 2 },
  detail: { fontSize: 12.5, color: colors.ink500, marginTop: 4 },
  date: { fontSize: 12, color: colors.ink400, marginTop: 2 },
  btn: { marginTop: 18, height: 46, borderRadius: radius.pill, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 40, ...shadows.pop },
  btnText: { color: '#fff', fontSize: 15, fontWeight: '800' },
});

export default BadgeUnlockModal;
