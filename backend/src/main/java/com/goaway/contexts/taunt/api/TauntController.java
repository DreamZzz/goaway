package com.goaway.contexts.taunt.api;

import com.goaway.contexts.taunt.api.dto.TauntInboxItemDTO;
import com.goaway.contexts.taunt.api.dto.TauntPreviewDTO;
import com.goaway.contexts.taunt.application.TauntContentService;
import com.goaway.contexts.taunt.application.TauntService;
import com.goaway.contexts.taunt.domain.TauntTrigger;
import com.goaway.contexts.taunt.infrastructure.persistence.TauntLogRepository;
import com.goaway.platform.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自助预览 / 自测毒舌推送（需登录）。生产可用于「给自己来一条」体验，本地用于联调。
 */
@RestController
@RequestMapping("/api/taunt")
public class TauntController {

    private final TauntContentService contentService;
    private final TauntService tauntService;
    private final TauntLogRepository tauntLogRepository;
    private final CurrentUserService currentUserService;

    public TauntController(TauntContentService contentService, TauntService tauntService,
                           TauntLogRepository tauntLogRepository, CurrentUserService currentUserService) {
        this.contentService = contentService;
        this.tauntService = tauntService;
        this.tauntLogRepository = tauntLogRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * 毒舌收件箱增量同步：返回成功发出的、id 大于 sinceId 的毒舌（升序，至多 50 条）。
     * 客户端落到本地「最讨厌的人」会话作未读消息。
     */
    @GetMapping("/inbox")
    public ResponseEntity<List<TauntInboxItemDTO>> inbox(@RequestParam(defaultValue = "0") Long sinceId) {
        Long userId = currentUserService.requireRealUserId();
        List<TauntInboxItemDTO> items = tauntLogRepository
                .findTop50ByUserIdAndSuccessTrueAndIdGreaterThanOrderByIdAsc(userId, sinceId)
                .stream().map(TauntInboxItemDTO::from).toList();
        return ResponseEntity.ok(items);
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
