package com.goaway.contexts.roleplay.api;

import com.goaway.contexts.roleplay.api.dto.RoleplayChatRequest;
import com.goaway.contexts.roleplay.api.dto.RoleplayPersonaDTO;
import com.goaway.contexts.roleplay.application.RoleplayService;
import com.goaway.platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/roleplay")
public class RoleplayController {

    private final RoleplayService roleplayService;
    private final CurrentUserService currentUserService;

    public RoleplayController(RoleplayService roleplayService, CurrentUserService currentUserService) {
        this.roleplayService = roleplayService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/personas")
    public ResponseEntity<List<RoleplayPersonaDTO>> personas() {
        List<RoleplayPersonaDTO> list = roleplayService.personas().stream()
                .map(RoleplayPersonaDTO::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody RoleplayChatRequest request) {
        currentUserService.requireRealUserId();
        return roleplayService.streamReply(request);
    }
}
