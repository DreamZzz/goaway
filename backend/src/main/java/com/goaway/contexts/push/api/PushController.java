package com.goaway.contexts.push.api;

import com.goaway.contexts.push.api.dto.PushPrefDTO;
import com.goaway.contexts.push.api.dto.RegisterDeviceRequest;
import com.goaway.contexts.push.api.dto.UpdatePushPrefRequest;
import com.goaway.contexts.push.application.PushService;
import com.goaway.platform.security.CurrentUserService;
import com.goaway.shared.dto.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 推送设备注册与偏好。均需真实登录用户（游客/未登录 401）。
 */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushService pushService;
    private final CurrentUserService currentUserService;

    public PushController(PushService pushService, CurrentUserService currentUserService) {
        this.pushService = pushService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/devices")
    public ResponseEntity<MessageResponse> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        Long userId = currentUserService.requireRealUserId();
        String platform = request.getPlatform() == null || request.getPlatform().isBlank()
                ? "ios" : request.getPlatform();
        pushService.registerDevice(userId, request.getDeviceToken().trim(), platform);
        return ResponseEntity.ok(new MessageResponse("设备已注册"));
    }

    @GetMapping("/prefs")
    public ResponseEntity<PushPrefDTO> getPrefs() {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(PushPrefDTO.from(pushService.getPreference(userId)));
    }

    @PutMapping("/prefs")
    public ResponseEntity<PushPrefDTO> updatePrefs(@Valid @RequestBody UpdatePushPrefRequest request) {
        Long userId = currentUserService.requireRealUserId();
        return ResponseEntity.ok(PushPrefDTO.from(pushService.updatePreference(
                userId, request.getEnabled(), request.getFrequency(),
                request.getQuietStart(), request.getQuietEnd())));
    }

    @PostMapping("/active")
    public ResponseEntity<MessageResponse> markActive() {
        Long userId = currentUserService.requireRealUserId();
        pushService.markActive(userId);
        return ResponseEntity.ok(new MessageResponse("ok"));
    }
}
