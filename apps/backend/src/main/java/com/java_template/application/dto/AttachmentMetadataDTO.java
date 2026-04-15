package com.java_template.application.dto;

import java.util.UUID;

/**
 * Lightweight response DTO for attachment list and metadata endpoints.
 *
 * <p>Intentionally omits the {@code content} field from {@link AttachmentDTO}.
 * That field may contain the full base64-encoded file as an inline fallback
 * for when the Cyoda EdgeMessage system is unavailable — returning it on every
 * list or metadata request would bloat responses by megabytes per attachment.
 *
 * <p>File content is only ever served through the dedicated
 * {@code GET /attachments/{id}/content} and {@code GET /attachments/{id}/view}
 * download endpoints, which read directly from EdgeMessage (or the inline
 * fallback) and stream bytes back — they never go through this DTO.
 */
public record AttachmentMetadataDTO(
        UUID id,
        UUID projectId,
        UUID caseId,
        String fileName,
        String fileType,
        long fileSize,
        String uploadedAt,
        UUID messageId
) {
    /**
     * Maps a full {@link AttachmentDTO} to its metadata-only projection.
     */
    public static AttachmentMetadataDTO from(AttachmentDTO dto) {
        return new AttachmentMetadataDTO(
                dto.getId(),
                dto.getProjectId(),
                dto.getCaseId(),
                dto.getFileName(),
                dto.getFileType(),
                dto.getFileSize(),
                dto.getUploadedAt(),
                dto.getMessageId()
        );
    }
}
