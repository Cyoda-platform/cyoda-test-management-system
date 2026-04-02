package com.java_template.application.controller;

import com.java_template.application.dto.AttachmentDTO;
import com.java_template.application.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.java_template.common.dto.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for Attachment operations.
 * File content is stored in Cyoda EdgeMessage; metadata is returned via GET.
 */
@RestController
@RequestMapping("/projects/{projectId}/attachments")
@Tag(name = "Attachments", description = "Attachment management endpoints")
public class AttachmentController {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentController.class);
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Upload attachment file to Cyoda EdgeMessage")
    public ResponseEntity<?> uploadAttachment(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caseId", required = false) UUID caseId) {
        try {
            AttachmentDTO uploaded = attachmentService.uploadAttachment(projectId, caseId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Upload failed"));
        }
    }

    @GetMapping("/by-case/{caseId}")
    @Operation(summary = "Get all attachments for a specific test case")
    public ResponseEntity<List<AttachmentDTO>> getAttachmentsByCase(
            @PathVariable UUID projectId,
            @PathVariable UUID caseId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByCaseId(caseId));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create attachment metadata only (no file content)")
    public ResponseEntity<AttachmentDTO> createAttachmentMetadata(
            @PathVariable UUID projectId,
            @RequestBody AttachmentDTO attachment) {
        attachment.setProjectId(projectId);
        AttachmentDTO created = attachmentService.uploadAttachment(attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Get all attachments for a project")
    public ResponseEntity<PageResult<AttachmentDTO>> getAttachmentsByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByProjectId(projectId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attachment metadata by ID")
    public ResponseEntity<AttachmentDTO> getAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        return attachmentService.getAttachmentById(id)
                .filter(a -> a.getProjectId().equals(projectId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/content")
    @Operation(summary = "Download attachment file content from Cyoda EdgeMessage")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        try {
            Optional<AttachmentDTO> meta = attachmentService.getAttachmentById(id)
                    .filter(a -> a.getProjectId().equals(projectId));
            if (meta.isEmpty()) return ResponseEntity.notFound().build();

            Optional<byte[]> content = attachmentService.getAttachmentContent(id);
            if (content.isEmpty()) return ResponseEntity.notFound().build();

            AttachmentDTO attachment = meta.get();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(
                            attachment.getFileType() != null ? attachment.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(content.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/view")
    @Operation(summary = "View attachment file content inline in browser (no download)")
    public ResponseEntity<byte[]> viewAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        try {
            logger.info("📸 View attachment request: projectId={}, attachmentId={}", projectId, id);

            Optional<AttachmentDTO> meta = attachmentService.getAttachmentById(id)
                    .filter(a -> a.getProjectId().equals(projectId));
            if (meta.isEmpty()) {
                logger.warn("⚠️  Attachment not found or project mismatch: id={}, projectId={}", id, projectId);
                return ResponseEntity.notFound().build();
            }

            AttachmentDTO attachment = meta.get();
            logger.debug("✅ Found attachment metadata: fileName={}, fileType={}, fileSize={}",
                    attachment.getFileName(), attachment.getFileType(), attachment.getFileSize());

            Optional<byte[]> content = attachmentService.getAttachmentContent(id);
            if (content.isEmpty()) {
                logger.warn("⚠️  Could not retrieve content for attachment: id={}", id);
                return ResponseEntity.notFound().build();
            }

            byte[] data = content.get();
            logger.info("✅ Returning attachment for inline view: fileName={}, size={} bytes, contentType={}",
                    attachment.getFileName(), data.length, attachment.getFileType());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + attachment.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(
                            attachment.getFileType() != null ? attachment.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(data);
        } catch (Exception e) {
            logger.error("❌ Error in view attachment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete attachment and its EdgeMessage")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID projectId,
            @PathVariable UUID id) {
        if (attachmentService.deleteAttachment(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

