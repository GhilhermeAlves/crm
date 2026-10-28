package com.becommerce.crm.masterdata.storage.application.service;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.becommerce.crm.masterdata.contact.note.application.service.PatientTimelineRecorder;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Depois do upload de um arquivo do paciente: cria a notificação com o botão de
 * download (metadata {@code kind = STORAGE_FILE}) e registra o envio nas
 * anotações do paciente. Falhas aqui não desfazem o upload.
 */
@Component
public class PatientFileNotifier {

    public static final String NOTIFICATION_KIND = "STORAGE_FILE";

    private static final Logger log = LoggerFactory.getLogger(PatientFileNotifier.class);

    private final NotificationUseCase notifications;
    private final PatientTimelineRecorder timeline;
    private final ObjectMapper objectMapper;

    public PatientFileNotifier(NotificationUseCase notifications, PatientTimelineRecorder timeline,
                               ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.timeline = timeline;
        this.objectMapper = objectMapper;
    }

    public void fileUploaded(UUID companyId, UUID contactId, StorageResponse file, UUID userId) {
        timeline.record(companyId, contactId, "Arquivo enviado: " + file.fileName(), userId);
        try {
            notifications.create(companyId, new CreateNotificationRequest(userId, NotificationType.SYSTEM,
                    "Arquivo disponível para download", file.fileName(), metadata(file, contactId)), userId);
        } catch (RuntimeException ex) {
            log.warn("Não foi possível notificar o upload do arquivo {}", file.id(), ex);
        }
    }

    private String metadata(StorageResponse file, UUID contactId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("kind", NOTIFICATION_KIND);
        data.put("objectId", file.id().toString());
        data.put("fileName", file.fileName());
        data.put("contactId", contactId.toString());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
