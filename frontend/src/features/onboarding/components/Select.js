import React, { useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

/**
 * 轻量下拉选择：点击弹出底部列表选择。
 */
const Select = ({ label, value, options, placeholder = '请选择', onChange }) => {
  const [open, setOpen] = useState(false);
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label}</Text>
      <TouchableOpacity style={styles.field} onPress={() => setOpen(true)} activeOpacity={0.7}>
        <Text style={[styles.value, !value && styles.placeholder]} numberOfLines={1}>
          {value || placeholder}
        </Text>
        <Icon name="chevron-down" size={16} color={colors.ink400} />
      </TouchableOpacity>

      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable style={styles.backdrop} onPress={() => setOpen(false)}>
          <Pressable style={styles.sheet}>
            <Text style={styles.sheetTitle}>{label}</Text>
            <ScrollView style={styles.list} showsVerticalScrollIndicator={false}>
              {options.map((opt) => {
                const active = opt === value;
                return (
                  <TouchableOpacity
                    key={opt}
                    style={styles.option}
                    onPress={() => { onChange(opt); setOpen(false); }}
                    activeOpacity={0.7}
                  >
                    <Text style={[styles.optionText, active && styles.optionActive]}>{opt}</Text>
                    {active && <Icon name="checkmark" size={18} color={colors.brand500} />}
                  </TouchableOpacity>
                );
              })}
            </ScrollView>
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 12 },
  label: { fontSize: 15, color: colors.ink700, fontWeight: '500' },
  field: { flexDirection: 'row', alignItems: 'center', gap: 4, maxWidth: '60%' },
  value: { fontSize: 15, color: colors.ink900 },
  placeholder: { color: colors.ink300 },
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.35)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: colors.bgElev, borderTopLeftRadius: radius.lg, borderTopRightRadius: radius.lg, paddingHorizontal: spacing.md, paddingTop: spacing.md, paddingBottom: spacing.xl, maxHeight: '70%', ...shadows.pop },
  sheetTitle: { fontSize: 16, fontWeight: '700', color: colors.ink900, marginBottom: 8, textAlign: 'center' },
  list: { flexGrow: 0 },
  option: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 14, borderBottomWidth: 0.5, borderBottomColor: colors.ink100 },
  optionText: { fontSize: 15.5, color: colors.ink900 },
  optionActive: { color: colors.brand500, fontWeight: '700' },
});

export default Select;
