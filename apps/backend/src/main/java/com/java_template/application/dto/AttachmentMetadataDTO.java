package com.java_template.application.dto;

import java.util.UUID;

/**
 * Lightweight response DTO for attachment list and metadata endpoints.
 * Omits {@code content} to avoid bloating responses with base64 data.
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
