package com.atlante;

import com.atlante.service.MailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    MailService mailService;

    private void forgot(String email) throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isAccepted());
    }

    private org.springframework.test.web.servlet.ResultActions reset(String token, String password) throws Exception {
        return mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("token", token, "password", password))));
    }

    private org.springframework.test.web.servlet.ResultActions login(String id, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", id, "password", password))));
    }

    @Test
    void fullResetFlowWithSingleUseToken() throws Exception {
        String username = unique("smemorato");
        register(username);
        String email = username + "@test.local";

        forgot(email.toUpperCase());

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(5000)).sendPasswordReset(eq(email), anyString(), link.capture(), anyLong());
        assertThat(link.getValue()).startsWith("http://localhost:5173/reimposta-password?token=");
        String token = link.getValue().substring(link.getValue().indexOf("token=") + 6);

        reset(token, "corta").andExpect(status().isBadRequest());
        reset(token, "NuovaPassword1").andExpect(status().isNoContent());

        login(username, "Password12345").andExpect(status().isUnauthorized());
        login(username, "NuovaPassword1").andExpect(status().isOk());

        // il token è monouso
        reset(token, "AltraPassword22").andExpect(status().isBadRequest());
    }

    @Test
    void unknownEmailAnswersTheSameAndSendsNothing() throws Exception {
        forgot(unique("fantasma") + "@test.local");
        verify(mailService, after(500).never()).sendPasswordReset(anyString(), anyString(), anyString(), anyLong());
        reset("token-inventato", "NuovaPassword1").andExpect(status().isBadRequest());
    }
}
