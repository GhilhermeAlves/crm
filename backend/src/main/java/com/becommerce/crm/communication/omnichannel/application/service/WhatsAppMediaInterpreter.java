package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.port.output.AiMediaProvider;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Transforma áudio, foto e PDF recebidos em texto que o agente entende:
 * baixa a mídia na Evolution, transcreve/resume via IA e grava o resultado no
 * corpo da mensagem (a equipe vê na Inbox). A mídia em si não é guardada.
 *
 * <p>LGPD: o resumo de imagem/PDF é descritivo — nunca diagnóstico.
 */
@Service
public class WhatsAppMediaInterpreter {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMediaInterpreter.class);

    /** ~10 MB: acima disso a mídia não é processada (resposta "a equipe vai olhar"). */
    static final int MAX_MEDIA_BYTES = 10 * 1024 * 1024;

    static final String IMAGE_INSTRUCTION = """
            Você auxilia a recepção de um consultório odontológico. Um paciente enviou esta imagem pelo WhatsApp.
            Descreva em até 3 frases, em português, o que ela mostra de forma objetiva (ex.: foto de dentes,
            pedido de exame, carteirinha de convênio, comprovante). Transcreva textos importantes visíveis
            (nomes de exames, datas, valores, convênio). NÃO faça diagnóstico nem avaliação clínica.""";

    static final String DOCUMENT_INSTRUCTION = """
            Você auxilia a recepção de um consultório odontológico. Um paciente enviou este documento pelo WhatsApp.
            Resuma em até 4 frases, em português, de que se trata (ex.: laudo, pedido de exame, orçamento, guia de
            convênio) e as informações administrativas relevantes (datas, nomes de exames, valores, convênio).
            NÃO faça diagnóstico nem interprete resultados clinicamente.""";

    private final WhatsAppProvider whatsAppProvider;
    private final AiMediaProvider mediaProvider;
    private final OmnichannelMessageRepository messageRepository;

    public WhatsAppMediaInterpreter(WhatsAppProvider whatsAppProvider, AiMediaProvider mediaProvider,
                                    OmnichannelMessageRepository messageRepository) {
        this.whatsAppProvider = whatsAppProvider;
        this.mediaProvider = mediaProvider;
        this.messageRepository = messageRepository;
    }

    /**
     * Texto que representa a mensagem para o agente. Para TEXT devolve o corpo;
     * para mídia, a transcrição/resumo (já gravada na mensagem) ou um aviso
     * quando não deu para entender.
     */
    public String interpret(Channel channel, Message inbound) {
        MessageType type = inbound.getType();
        if (type == null || type == MessageType.TEXT || inbound.getExternalMessageId() == null) {
            return inbound.getBody();
        }
        String caption = captionOf(inbound.getBody());
        String text;
        try {
            Optional<WhatsAppProvider.MediaContent> media = whatsAppProvider.downloadMedia(
                    channel.getExternalId(), inbound.getExternalMessageId(), channel.getSecretsRef());
            if (media.isEmpty() || media.get().data() == null || media.get().data().length == 0) {
                text = unreadable(type, caption);
            } else if (media.get().data().length > MAX_MEDIA_BYTES) {
                text = label(type) + " (arquivo grande demais para leitura automática)"
                        + (caption != null ? " — legenda: " + caption : "");
            } else {
                text = convert(type, media.get(), caption);
            }
        } catch (OmnichannelProviderException | AiProviderException e) {
            log.warn("[WHATSAPP][MEDIA] falha ao interpretar {} (message={}): {}",
                    type, inbound.getId(), e.getMessage());
            text = unreadable(type, caption);
        }
        messageRepository.updateBody(inbound.getId(), text);
        return text;
    }

    private String convert(MessageType type, WhatsAppProvider.MediaContent media, String caption) {
        return switch (type) {
            case AUDIO -> {
                String transcript = mediaProvider.transcribe(media.data(), media.mimeType());
                yield transcript.isBlank() ? "🎤 Áudio (sem fala identificada)" : "🎤 Áudio: " + transcript;
            }
            case IMAGE -> {
                String description = mediaProvider.describe(media.data(),
                        media.mimeType() != null ? media.mimeType() : "image/jpeg", null, IMAGE_INSTRUCTION);
                yield "🖼️ Imagem" + (caption != null ? " (legenda: " + caption + ")" : "") + ": " + description;
            }
            case DOCUMENT -> {
                String summary = mediaProvider.describe(media.data(), "application/pdf",
                        media.fileName(), DOCUMENT_INSTRUCTION);
                yield "📄 Documento" + (media.fileName() != null ? " " + media.fileName() : "")
                        + (caption != null ? " (legenda: " + caption + ")" : "") + ": " + summary;
            }
            case TEXT -> caption;
        };
    }

    private static String unreadable(MessageType type, String caption) {
        return label(type) + " (não foi possível ler automaticamente; a equipe vai verificar)"
                + (caption != null ? " — legenda: " + caption : "");
    }

    private static String label(MessageType type) {
        return switch (type) {
            case AUDIO -> "🎤 Áudio";
            case IMAGE -> "🖼️ Imagem";
            case DOCUMENT -> "📄 Documento";
            case TEXT -> "Mensagem";
        };
    }

    /** O parser grava a legenda como corpo, ou um marcador "[tipo]" quando não há legenda. */
    static String captionOf(String body) {
        if (body == null || body.isBlank() || (body.startsWith("[") && body.endsWith("]"))) {
            return null;
        }
        return body.trim();
    }
}
