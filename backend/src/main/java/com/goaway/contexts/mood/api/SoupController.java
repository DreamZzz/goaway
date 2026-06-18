package com.goaway.contexts.mood.api;

import com.goaway.contexts.mood.api.dto.SoupDTO;
import com.goaway.contexts.mood.application.SoupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/soup")
public class SoupController {

    private final SoupService soupService;

    public SoupController(SoupService soupService) {
        this.soupService = soupService;
    }

    @GetMapping("/daily")
    public ResponseEntity<SoupDTO> daily() {
        return ResponseEntity.ok(new SoupDTO(soupService.daily(LocalDate.now())));
    }

    @GetMapping("/random")
    public ResponseEntity<SoupDTO> random() {
        return ResponseEntity.ok(new SoupDTO(soupService.random()));
    }
}
