package com.goaway.contexts.roleplay.api;

import com.goaway.contexts.account.application.GuestSessionService;
import com.goaway.contexts.account.domain.User;
import com.goaway.contexts.roleplay.api.dto.ReportContentRequest;
import com.goaway.contexts.roleplay.api.dto.RoleplayChatRequest;
import com.goaway.contexts.roleplay.api.dto.RoleplayPersonaDTO;
import com.goaway.contexts.roleplay.application.RoleplayService;
import com.goaway.contexts.roleplay.domain.ContentReport;
import com.goaway.contexts.roleplay.infrastructure.persistence.ContentReportRepository;
import com.goaway.platform.security.CurrentUserService;
import com.goaway.platform.security.GuestSecuritySupport;
import com.goaway.shared.dto.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/roleplay")
public class RoleplayController {

    private final RoleplayService roleplayService;
    private final CurrentUserService currentUserService;
    private final GuestSessionService guestSessionService;
    private final GuestSecuritySupport guestSecuritySupport;
    private final ContentReportRepository contentReportRepository;

    public RoleplayController(RoleplayService roleplayService, CurrentUserService currentUserService,
                              GuestSessionService guestSessionService, GuestSecuritySupport guestSecuritySupport,
                              ContentReportRepository contentReportRepository) {
        this.roleplayService = roleplayService;
        this.currentUserService = currentUserService;
        this.guestSessionService = guestSessionService;
        this.guestSecuritySupport = guestSecuritySupport;
        this.contentReportRepository = contentReportRepository;
    }

    @GetMapping("/personas")
    public ResponseEntity<List<RoleplayPersonaDTO>> personas() {
        List<RoleplayPersonaDTO> list = roleplayService.personas().stream()
                .map(RoleplayPersonaDTO::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody RoleplayChatRequest request, HttpServletRequest http) {
        // 游客可试用：消耗一次游客额度，耗尽抛 GUEST_TRIAL_EXHAUSTED(403) 引导登录；真实用户不限次。
        if (currentUserService.isGuest()) {
            Long guestUserId = currentUserService.getCurrentUser().map(User::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请登录后使用该功能"));
            String installationId = http.getHeader(GuestSecuritySupport.GUEST_INSTALLATION_HEADER);
            String clientIp = guestSecuritySupport.resolveClientIp(http);
            guestSessionService.consumeInspirationTrial(guestUserId, installationId, clientIp, "roleplay_chat");
        } else {
            currentUserService.requireRealUserId();
        }
        return roleplayService.streamReply(request);
    }

    /**
     * 举报 AI 生成的不当内容（对线回复 / 毒舌）。游客与登录用户均可举报，开发者后续跟进处理。
     */
    @PostMapping("/report")
    public ResponseEntity<MessageResponse> report(@Valid @RequestBody ReportContentRequest request) {
        Long userId = currentUserService.getCurrentUser().map(User::getId).orElse(null);
        String source = request.getSource() == null || request.getSource().isBlank() ? "roleplay" : request.getSource();
        contentReportRepository.save(new ContentReport(userId, source, request.getContent(), request.getReason()));
        return ResponseEntity.ok(new MessageResponse("已收到举报，我们会尽快处理"));
    }
}
