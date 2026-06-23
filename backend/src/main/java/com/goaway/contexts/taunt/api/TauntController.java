package com.goaway.contexts.taunt.api;

import com.goaway.contexts.taunt.api.dto.TauntPreviewDTO;
import com.goaway.contexts.taunt.application.TauntContentService;
import com.goaway.contexts.taunt.application.TauntService;
import com.goaway.contexts.taunt.domain.TauntTrigger;
import com.goaway.platform.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 自助预览 / 自测毒舌推送（需登录）。生产可用于「给自己来一条」体验，本地用于联调。
 */
@RestController
@RequestMapping("/api/taunt")
public class TauntController {

    private final TauntContentService contentService;
    private final TauntService tauntService;
    private final CurrentUserService currentUserService;

    public TauntController(TauntContentService contentService, TauntService tauntService,
                           CurrentUserService currentUserService) {
        this.contentService = contentService;
        this.tauntService = tauntService;
        this.currentUserService = currentUserService;
    }

    /** 仅生成内容、不推送（看千人千面效果）。 */
    @PostMapping("/preview")
    public ResponseEntity<TauntPreviewDTO> preview() {
        Long userId = currentUserService.requireRealUserId();
        String content = contentService.generate(userId, TauntTrigger.SCHEDULED);
        return ResponseEntity.ok(new TauntPreviewDTO(content, false));
    }

    /** 生成并真正推送给自己（绕过频控，用于自测/体验）。 */
    @PostMapping("/test")
    public ResponseEntity<TauntPreviewDTO> testSend() {
        Long userId = currentUserService.requireRealUserId();
        TauntService.DeliverResult result = tauntService.deliver(userId, TauntTrigger.SCHEDULED);
        return ResponseEntity.ok(new TauntPreviewDTO(result.content(), result.success()));
    }
}
