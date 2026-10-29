package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.services.network.ChatErrors;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Response;
import static org.junit.Assert.*;

public class ChatErrorsTest {
    private ScreenState<Object> error(int code, String json) {
        return ChatErrors.from(Response.error(code, ResponseBody.create(MediaType.get("application/json"), json)));
    }

    @Test
    public void exposesHttpStatusWithoutLeakingServerInternals() {
        ScreenState<Object> result = error(500, "{\"detail\":\"Traceback password=private\"}");
        assertEquals(500, result.code);
        assertTrue(result.message.contains("HTTP 500"));
        assertTrue(result.message.contains("falhou ao gerar"));
        assertFalse(result.message.contains("private"));
    }

    @Test
    public void retainsActionableClientError() {
        assertTrue(error(400, "{\"detail\":\"Conversa encerrada. Inicie outra conversa.\"}").message.contains("Conversa encerrada"));
    }

    @Test
    public void handlesFastApiValidationArraysAndPlainProxyErrors() {
        assertTrue(error(422, "{\"detail\":[{\"msg\":\"Field required\"}]}").message.contains("validar"));
        assertTrue(error(503, "<html>Service unavailable</html>").message.contains("temporariamente indisponível"));
    }

    @Test
    public void distinguishesAuthorizationFromAvailability() {
        assertTrue(error(403, "{}").message.contains("não autorizou"));
        assertTrue(error(429, "{}").message.contains("Muitas mensagens"));
    }
}
