package com.becommerce.crm.communication.omnichannel.domain;

/** Tipo de conteúdo da mensagem. */
public enum MessageType {
    TEXT,
    /** Nota de voz / áudio (o corpo guarda a transcrição quando disponível). */
    AUDIO,
    /** Foto (o corpo guarda legenda e o resumo do que a imagem mostra). */
    IMAGE,
    /** Documento, ex.: PDF (o corpo guarda o resumo do conteúdo). */
    DOCUMENT
}
