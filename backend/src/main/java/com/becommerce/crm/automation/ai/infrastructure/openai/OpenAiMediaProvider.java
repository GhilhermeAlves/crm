package com.becommerce.crm.automation.ai.infrastructure.openai;

import com.becommerce.crm.automation.ai.application.port.output.AiMediaProvider;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Mídia via OpenAI (mesma chave do chat): transcrição ({@code /v1/audio/transcriptions}),
 * leitura de imagem/PDF ({@code /v1/chat/completions} com conteúdo multimodal) e
 * voz ({@code /v1/audio/speech}, saída opus). Nunca loga conteúdo nem a chave.
 */
@Component
public class OpenAiMediaProvider implements AiMediaProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String transcribeModel;
    private final String visionModel;
    private final String speechModel;
    private final String speechVoice;

    @Autowired
    public OpenAiMediaProvider(
            RestClient.Builder restClientBuilder,
            @Value("${app.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.media.transcribe-model:gpt-4o-mini-transcribe}") String transcribeModel,
            @Value("${app.ai.media.vision-model:gpt-4o-mini}") String visionModel,
            @Value("${app.ai.media.speech-model:gpt-4o-mini-tts}") String speechModel,
            @Value("${app.ai.media.speech-voice:shimmer}") String speechVoice) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
        this.transcribeModel = transcribeModel;
        this.visionModel = visionModel;
        this.speechModel = speechModel;
        this.speechVoice = speechVoice;
    }

    @Override
    public String transcribe(byte[] audio, String mimeType) {
        requireKey();
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return "audio." + audioExtension(mimeType);
            }
        });
        form.add("model", transcribeModel);
        form.add("language", "pt");
        form.add("response_format", "json");
        Map<?, ?> response = call(() -> restClient.post().uri("/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AiProviderException("Transcrição HTTP " + res.getStatusCode().value(), false);
                })
                .body(Map.class));
        Object text = response != null ? response.get("text") : null;
        return text == null ? "" : String.valueOf(text).trim();
    }

    @Override
    public String describe(byte[] content, String mimeType, String fileName, String instruction) {
        requireKey();
        String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(content);
        Map<String, Object> mediaPart = mimeType != null && mimeType.startsWith("image/")
                ? Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                : Map.of("type", "file", "file", Map.of(
                        "filename", fileName != null && !fileName.isBlank() ? fileName : "documento.pdf",
                        "file_data", dataUrl));
        Map<String, Object> body = Map.of(
                "model", visionModel,
                "max_tokens", 500,
                "messages", List.of(Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", instruction), mediaPart))));
        Map<?, ?> response = call(() -> restClient.post().uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AiProviderException("Leitura de mídia HTTP " + res.getStatusCode().value(), false);
                })
                .body(Map.class));
        return firstChoiceContent(response);
    }

    @Override
    public byte[] speech(String text) {
        requireKey();
        Map<String, Object> body = Map.of(
                "model", speechModel,
                "voice", speechVoice,
                "input", text,
                "response_format", "opus",
                "instructions", "Fale em português do Brasil, com tom acolhedor, natural e calmo, "
                        + "como uma recepcionista simpática de consultório.");
        byte[] audio = call(() -> restClient.post().uri("/v1/audio/speech")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AiProviderException("Voz HTTP " + res.getStatusCode().value(), false);
                })
                .body(byte[].class));
        if (audio == null || audio.length == 0) {
            throw new AiProviderException("Voz: resposta vazia", false);
        }
        return audio;
    }

    private void requireKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Chave de API do OpenAI não configurada (app.ai.api-key).", false);
        }
    }

    private static <T> T call(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (AiProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiProviderException("Falha de mídia OpenAI: " + e.getClass().getSimpleName(), true);
        }
    }

    private static String firstChoiceContent(Map<?, ?> response) {
        if (response != null && response.get("choices") instanceof List<?> choices && !choices.isEmpty()
                && choices.get(0) instanceof Map<?, ?> choice
                && choice.get("message") instanceof Map<?, ?> message && message.get("content") != null) {
            return String.valueOf(message.get("content")).trim();
        }
        return "";
    }

    static String audioExtension(String mimeType) {
        if (mimeType == null) {
            return "ogg";
        }
        String m = mimeType.toLowerCase(java.util.Locale.ROOT);
        if (m.contains("mpeg") || m.contains("mp3")) return "mp3";
        if (m.contains("mp4") || m.contains("m4a") || m.contains("aac")) return "m4a";
        if (m.contains("wav")) return "wav";
        if (m.contains("webm")) return "webm";
        return "ogg";
    }
}
