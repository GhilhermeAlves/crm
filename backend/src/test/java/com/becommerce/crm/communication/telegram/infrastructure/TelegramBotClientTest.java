package com.becommerce.crm.communication.telegram.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramBotClientTest {

    private MockRestServiceServer server;
    private TelegramBotClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TelegramBotClient(builder, "https://api.telegram.org");
    }

    @Test
    void getMeReturnsUsername() {
        server.expect(requestTo("https://api.telegram.org/botTOK/getMe"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"username\":\"leo_bot\"}}", MediaType.APPLICATION_JSON));
        assertEquals("leo_bot", client.getMe("TOK"));
    }

    @Test
    void getMeInvalidTokenThrows() {
        server.expect(requestTo("https://api.telegram.org/botBAD/getMe"))
              .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"ok\":false}").contentType(MediaType.APPLICATION_JSON));
        var ex = assertThrows(TelegramBotClient.TelegramApiException.class, () -> client.getMe("BAD"));
        assertFalse(ex.toString().contains("BAD"));
        assertNull(ex.getCause());
    }

    @Test
    void sendMessageLongTextOnlyLastPartCarriesKeyboard() {
        server.expect(requestTo("https://api.telegram.org/botTOK/sendMessage"))
              .andExpect(jsonPath("$.reply_markup").doesNotExist())
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":1}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.telegram.org/botTOK/sendMessage"))
              .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][0].callback_data").value("c:1"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":2}}", MediaType.APPLICATION_JSON));
        long id = client.sendMessage("TOK", 1, "a".repeat(5000),
                List.of(List.of(new TelegramBotClient.Button("OK", "c:1"))));
        assertEquals(2, id);
        server.verify();
    }

    @Test
    void sendMessageSendsTextUnchangedWithKeyboard() {
        server.expect(requestTo("https://api.telegram.org/botTOK/sendMessage"))
              .andExpect(jsonPath("$.chat_id").value(42))
              .andExpect(jsonPath("$.text").value("  marca a Maria  "))
              .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][0].callback_data").value("c:1"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":7}}", MediaType.APPLICATION_JSON));
        long id = client.sendMessage("TOK", 42, "  marca a Maria  ",
                List.of(List.of(new TelegramBotClient.Button("✅ Confirmar", "c:1"))));
        assertEquals(7, id);
    }

    @Test
    void sendMessageSplitsLongText() {
        String longText = "a".repeat(5000);
        server.expect(ExpectedCount.times(2), requestTo("https://api.telegram.org/botTOK/sendMessage"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":1}}", MediaType.APPLICATION_JSON));
        client.sendMessage("TOK", 1, longText, null);
        server.verify();
    }

    @Test
    void downloadFileResolvesPathThenDownloads() {
        server.expect(requestTo("https://api.telegram.org/botTOK/getFile?file_id=F1"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"file_path\":\"voice/a.oga\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.telegram.org/file/botTOK/voice/a.oga"))
              .andRespond(withSuccess(new byte[]{1, 2, 3}, MediaType.APPLICATION_OCTET_STREAM));
        assertArrayEquals(new byte[]{1, 2, 3}, client.downloadFile("TOK", "F1"));
    }
}
