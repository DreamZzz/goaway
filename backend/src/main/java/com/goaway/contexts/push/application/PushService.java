package com.goaway.contexts.push.application;

import com.goaway.contexts.push.domain.PushDevice;
import com.goaway.contexts.push.domain.PushFrequency;
import com.goaway.contexts.push.domain.PushPreference;
import com.goaway.contexts.push.infrastructure.persistence.PushDeviceRepository;
import com.goaway.contexts.push.infrastructure.persistence.PushPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备 token 注册、推送偏好读写、活跃水位维护。taunt 调度域复用本服务取设备与偏好。
 */
@Service
public class PushService {

    private final PushDeviceRepository deviceRepository;
    private final PushPreferenceRepository preferenceRepository;

    public PushService(PushDeviceRepository deviceRepository, PushPreferenceRepository preferenceRepository) {
        this.deviceRepository = deviceRepository;
        this.preferenceRepository = preferenceRepository;
    }

    /** 注册/刷新设备 token：token 已存在则迁移到当前用户；同时确保有偏好行并刷新活跃。 */
    @Transactional
    public void registerDevice(Long userId, String deviceToken, String platform) {
        PushDevice device = deviceRepository.findByDeviceToken(deviceToken)
                .orElseGet(() -> new PushDevice(userId, deviceToken, platform));
        device.setUserId(userId);
        device.setPlatform(platform);
        deviceRepository.save(device);
        getOrCreatePreference(userId);
        markActive(userId);
    }

    @Transactional(readOnly = true)
    public PushPreference getPreference(Long userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> new PushPreference(userId));
    }

    @Transactional
    public PushPreference getOrCreatePreference(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> preferenceRepository.save(new PushPreference(userId)));
    }

    @Transactional
    public PushPreference updatePreference(Long userId, Boolean enabled, String frequency,
                                           Integer quietStart, Integer quietEnd) {
        PushPreference pref = getOrCreatePreference(userId);
        if (enabled != null) {
            pref.setEnabled(enabled);
        }
        if (frequency != null) {
            pref.setFrequency(PushFrequency.fromString(frequency));
        }
        if (quietStart != null) {
            pref.setQuietStart(clampHour(quietStart));
        }
        if (quietEnd != null) {
            pref.setQuietEnd(clampHour(quietEnd));
        }
        return preferenceRepository.save(pref);
    }

    @Transactional
    public void markActive(Long userId) {
        PushPreference pref = getOrCreatePreference(userId);
        pref.setLastActiveAt(LocalDateTime.now());
        preferenceRepository.save(pref);
    }

    /** 成功发出一条毒舌后记录水位，供间隔频控。 */
    @Transactional
    public void markTaunted(Long userId) {
        PushPreference pref = getOrCreatePreference(userId);
        pref.setLastTauntAt(LocalDateTime.now());
        preferenceRepository.save(pref);
    }

    @Transactional(readOnly = true)
    public List<String> deviceTokensForUser(Long userId) {
        return deviceRepository.findByUserId(userId).stream().map(PushDevice::getDeviceToken).toList();
    }

    /** APNs 返回 token 失效时，删除该设备避免反复重试。 */
    @Transactional
    public void removeDevice(String deviceToken) {
        deviceRepository.deleteByDeviceToken(deviceToken);
    }

    private Integer clampHour(Integer hour) {
        if (hour == null) {
            return null;
        }
        return Math.max(0, Math.min(23, hour));
    }
}
