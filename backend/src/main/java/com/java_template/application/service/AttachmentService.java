package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java_template.application.dto.AttachmentDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EdgeMessageService;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Attachment operations.
 * File content is stored in Cyoda EdgeMessage; metadata is stored in the repository.
 */
@Service
public class AttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentService.class);
    private static final String EDGE_MESSAGE_SUBJECT = "attachment";
    private static final ModelSpec MODEL_SPEC =
            new ModelSpec().withName(AttachmentDTO.ENTITY_NAME).withVersion(AttachmentDTO.ENTITY_VERSION);

    private final EntityService entityService;
    private final EdgeMessageService edgeMessageService;
    private final ObjectMapper objectMapper;

    public AttachmentService(EntityService entityService,
                             EdgeMessageService edgeMessageService,
                             ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.edgeMessageService = edgeMessageService;
        this.objectMapper = objectMapper;
    }

    private AttachmentDTO withId(EntityWithMetadata<AttachmentDTO> result) {
        AttachmentDTO entity = result.entity();
        entity.setId(result.getId());
        return entity;
    }

    private PageResult<AttachmentDTO> toPage(PageResult<EntityWithMetadata<AttachmentDTO>> result) {
        return PageResult.of(result.searchId(),
                result.data().stream().map(this::withId).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements());
    }

    /**
     * Uploads a file. Stores metadata in EdgeMessage (NOT the file content!).
     * File content stays in memory temporarily, then entity references the EdgeMessage.
     */
    public AttachmentDTO uploadAttachment(UUID projectId, UUID caseId, MultipartFile file) throws IOException {
        long fileSizeBytes = file.getSize();

        logger.info("📤 Starting attachment upload: fileName='{}', size={}KB",
                file.getOriginalFilename(), fileSizeBytes / 1024);

        AttachmentDTO attachment = new AttachmentDTO();
        attachment.setProjectId(projectId);
        attachment.setCaseId(caseId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(fileSizeBytes);
        attachment.setUploadedAt(java.time.LocalDateTime.now());

        // Store file content + metadata in EdgeMessage.
        // Nginx is configured with proxy-body-size: 150m and Spring Boot multipart limits are 100MB,
        // so uploads up to 100MB are supported end-to-end.
        try {
            logger.info("📝 Creating EdgeMessage with file content for '{}'...", file.getOriginalFilename());

            ObjectNode content = objectMapper.createObjectNode();
            content.put("fileName", file.getOriginalFilename());
            content.put("fileType", file.getContentType());
            content.put("fileSize", fileSizeBytes);
            content.put("uploadedAt", attachment.getUploadedAt().toString());
            // Base64-encode file bytes so the binary content travels safely as JSON through gRPC
            content.put("data", Base64.getEncoder().encodeToString(file.getBytes()));

            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("projectId", projectId.toString());
            if (caseId != null) metadata.put("caseId", caseId.toString());
            metadata.put("contentType", file.getContentType());

            UUID messageId = edgeMessageService.createMessage(EDGE_MESSAGE_SUBJECT, content, metadata);
            attachment.setMessageId(messageId);
            logger.info("✅ EdgeMessage created for file '{}': messageId={} (size: {}KB)",
                    file.getOriginalFilename(), messageId, fileSizeBytes / 1024);
        } catch (Exception e) {
            logger.error("❌ EdgeMessage creation failed for file '{}': {}",
                    file.getOriginalFilename(), e.getMessage());
            logger.warn("⚠️  Will proceed without EdgeMessage reference (metadata stored in entity only — view/download will not work)");
        }

        logger.info("💾 Creating Attachment entity in Cyoda...");
        AttachmentDTO result = withId(entityService.create(attachment));
        logger.info("✅ Attachment entity created successfully");
        return result;
    }

    /**
     * Creates attachment metadata only (without file content).
     */
    public AttachmentDTO uploadAttachment(AttachmentDTO attachment) {
        return withId(entityService.create(attachment));
    }

    /**
     * Retrieves attachment metadata by ID.
     */
    public Optional<AttachmentDTO> getAttachmentById(UUID id) {
        try {
            return Optional.of(withId(entityService.getById(id, MODEL_SPEC, AttachmentDTO.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves the raw file content (base64-decoded).
     * Tries EdgeMessage first; falls back to inline base64 stored directly in the entity.
     */
    public Optional<byte[]> getAttachmentContent(UUID id) throws Exception {
        return getAttachmentById(id)
                .map(a -> {
                    // Fallback: content stored inline as base64
                    if (a.getMessageId() == null) {
                        if (a.getContent() == null) return null;
                        return Base64.getDecoder().decode(a.getContent());
                    }
                    try {
                        var content = edgeMessageService.getMessageContent(a.getMessageId());
                        if (content == null || !content.has("data")) return null;
                        return Base64.getDecoder().decode(content.get("data").asText());
                    } catch (Exception e) {
                        // EdgeMessage failed; try inline content as last resort
                        if (a.getContent() != null) {
                            logger.warn("EdgeMessage retrieval failed; serving inline content for attachment {}", id);
                            return Base64.getDecoder().decode(a.getContent());
                        }
                        throw new IllegalStateException("Failed to retrieve file content: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * Retrieves all attachments for a specific test case.
     */
    public List<AttachmentDTO> getAttachmentsByCaseId(UUID caseId) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(0).pageSize(1000).build();
        return entityService.findAll(MODEL_SPEC, AttachmentDTO.class, params)
                .data().stream()
                .filter(a -> a.entity().getCaseId() != null && a.entity().getCaseId().equals(caseId))
                .map(this::withId)
                .toList();
    }

    /**
     * Retrieves all attachments for a specific project.
     */
    public PageResult<AttachmentDTO> getAttachmentsByProjectId(UUID projectId, int page, int size) {
        SearchAndRetrievalParams params = SearchAndRetrievalParams.builder()
                .pageNumber(page).pageSize(size).build();
        PageResult<EntityWithMetadata<AttachmentDTO>> allAttachments =
                entityService.findAll(MODEL_SPEC, AttachmentDTO.class, params);

        List<EntityWithMetadata<AttachmentDTO>> filteredAttachments = allAttachments.data().stream()
                .filter(a -> a.entity().getProjectId() != null && a.entity().getProjectId().equals(projectId))
                .toList();

        return PageResult.of(allAttachments.searchId(),
                filteredAttachments.stream().map(this::withId).toList(),
                page, size, filteredAttachments.size());
    }

    /**
     * Deletes attachment metadata and the corresponding EdgeMessage.
     */
    public boolean deleteAttachment(UUID id) {
        return getAttachmentById(id).map(attachment -> {
            if (attachment.getMessageId() != null) {
                edgeMessageService.deleteMessage(attachment.getMessageId());
                logger.info("Deleted EdgeMessage: {}", attachment.getMessageId());
            }
            entityService.deleteById(id);
            return true;
        }).orElse(false);
    }
}

