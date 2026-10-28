package com.becommerce.crm.masterdata.storage.application.service;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.becommerce.crm.masterdata.contact.note.application.service.PatientTimelineRecorder;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Arquivos do paciente: o upload é registrado nas anotações e o pedido de
 * download cria a notificação com o botão "Fazer download" (metadata
 * {@code kind = STORAGE_FILE}) em vez de baixar direto no navegador.
 */
@Component
public class PatientFileNotifier {

    public static final String NOTIFICATION_KIND = "STORAGE_FILE";

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
    }

    /** Clique em baixar na aba Arquivos: o download fica disponível no sino. */
    public void downloadRequested(UUID companyId, UUID contactId, StorageResponse file, UUID userId) {
        notifications.create(companyId, new CreateNotificationRequest(userId, NotificationType.SYSTEM,
                "Arquivo pronto para download", file.fileName(), metadata(file, contactId)), userId);
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
