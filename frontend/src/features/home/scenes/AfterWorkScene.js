import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const ENTRIES = [
  {
    key: 'weekly',
    icon: 'document-text-outline',
    title: 'AI 周报',
    desc: '碎片成果一键成稿',
    route: 'Weekly',
    accent: colors.brand500,
  },
  {
    key: 'roleplay',
    icon: 'flame-outline',
    title: '找 AI 对线',
    desc: '老板 / 同事 / 甲方，怼回去解压',
    route: 'Roleplay',
    accent: '#E06A4E',
  },
];

const AfterWorkScene = ({ navigation }) => (
  <View style={styles.wrap}>
    <Text style={styles.lead}>下班了，给情绪找个出口 👇</Text>
    {ENTRIES.map((e) => (
      <TouchableOpacity key={e.key} style={styles.card} onPress={() => navigation.navigate(e.route)} activeOpacity={0.85}>
        <View style={[styles.iconWrap, { backgroundColor: e.accent }]}>
          <Icon name={e.icon} size={22} color="#fff" />
        </View>
        <View style={styles.info}>
          <Text style={styles.title}>{e.title}</Text>
          <Text style={styles.desc}>{e.desc}</Text>
        </View>
        <Icon name="chevron-forward" size={18} color={colors.ink300} />
      </TouchableOpacity>
    ))}
  </View>
);

const styles = StyleSheet.create({
  wrap: { gap: spacing.sm },
  lead: { fontSize: 14, color: colors.ink500, marginBottom: 4 },
  card: {
    flexDirection: 'row', alignItems: 'center', gap: 14, padding: 18,
    backgroundColor: colors.bgElev, borderRadius: radius.lg,
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  iconWrap: { width: 44, height: 44, borderRadius: 14, alignItems: 'center', justifyContent: 'center' },
  info: { flex: 1, gap: 3 },
  title: { fontSize: 16.5, fontWeight: '700', color: colors.ink900 },
  desc: { fontSize: 12.5, color: colors.ink500 },
});

export default AfterWorkScene;
