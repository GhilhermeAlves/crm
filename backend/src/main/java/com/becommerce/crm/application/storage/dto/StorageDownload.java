package com.becommerce.crm.application.storage.dto;

import java.util.UUID;

/**
 * Conteúdo de um objeto de armazenamento para download (recuperação). Escopado
 * por tenant no backend (RLS) e pela checagem de empresa do controller.
 */
public record StorageDownload(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        byte[] data
) {}
