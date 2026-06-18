package com.goaway.contexts.media.api;

import com.goaway.contexts.media.api.dto.FileUploadResponse;
import com.goaway.contexts.media.application.FileStorageService;
import com.goaway.contexts.media.application.InvalidMediaException;
import com.goaway.platform.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/uploads")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;

    @Autowired
    public FileUploadController(FileStorageService fileStorageService, CurrentUserService currentUserService) {
        this.fileStorageService = fileStorageService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/single")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FileUploadResponse> uploadSingleFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new FileUploadResponse());
        }

        Long currentUserId = currentUserService.getCurrentUser().map(user -> user.getId()).orElse(null);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new FileUploadResponse());
        }

        try {
            String fileName = fileStorageService.storeFile(file, currentUserId);
            String fileUrl = fileStorageService.getFileUrlFromFileName(fileName);
            
            FileUploadResponse response = new FileUploadResponse(
                fileName,
                fileUrl,
                file.getContentType(),
                file.getSize()
            );
            
            return ResponseEntity.ok(response);
        } catch (InvalidMediaException exception) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new FileUploadResponse());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new FileUploadResponse());
        }
    }

    @PostMapping("/multiple")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FileUploadResponse>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(List.of());
        }

        Long currentUserId = currentUserService.getCurrentUser().map(user -> user.getId()).orElse(null);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }

        try {
            List<FileUploadResponse> responses = Arrays.stream(files)
                    .map(file -> {
                        String fileName = fileStorageService.storeFile(file, currentUserId);
                        String fileUrl = fileStorageService.getFileUrlFromFileName(fileName);
                        return new FileUploadResponse(
                                fileName,
                                fileUrl,
                                file.getContentType(),
                                file.getSize()
                        );
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (InvalidMediaException exception) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(List.of());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @DeleteMapping("/{fileName}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileName) {
        Long currentUserId = currentUserService.getCurrentUser().map(user -> user.getId()).orElse(null);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!fileStorageService.isOwnedBy(fileName, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean deleted = fileStorageService.deleteFile(fileName, currentUserId);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
