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
 *
 * <p>The {@code attachmentType}, {@code runId}, and {@code stepKey} fields mirror
 * their counterparts in {@link AttachmentDTO}. They are populated only when the
 * source DTO carries them (null for legacy records created before type discrimination
 * was introduced).
 */
public record AttachmentMetadataDTO(
        UUID id,
        UUID projectId,
        UUID caseId,
        UUID defectId,
        String fileName,
        String fileType,
        long fileSize,
        String uploadedAt,
        UUID messageId,
        String attachmentType,
        UUID runId,
        String stepKey
) {
    /** Maps a full {@link AttachmentDTO} to its metadata-only projection. */
    public static AttachmentMetadataDTO from(AttachmentDTO dto) {
        return new AttachmentMetadataDTO(
                dto.getId(),
                dto.getProjectId(),
                dto.getCaseId(),
                dto.getDefectId(),
                dto.getFileName(),
                dto.getFileType(),
                dto.getFileSize(),
                dto.getUploadedAt(),
                dto.getMessageId(),
                dto.getAttachmentType(),
                dto.getRunId(),
                dto.getStepKey()
        );
    }
}
