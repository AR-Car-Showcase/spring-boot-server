package com.arcarshowcaseserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API must answer bad input with 4xx, not 500. Every case below used to fall
 * through to the catch-all {@code Exception} handler because the advice imported
 * Tomcat's {@code org.apache.coyote.BadRequestException} instead of the project's own.
 */
@SpringBootTest(properties = {
        "security.public-endpoints=/api/auth/signup,/api/cars/car/**,/api/cars/rating/**"
})
@AutoConfigureMockMvc
class ApiErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void applicationBadRequestReturns400() throws Exception {
        mockMvc.perform(get("/api/cars/car/-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void beanValidationFailureReturns400WithFieldDetail() throws Exception {
        String body = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "a",
                                  "email": "not-an-email",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        // The mobile client renders body.message, so it must stay human-readable.
        assertThat(json.get("message").asText()).isNotBlank();
        assertThat(json.get("fieldErrors").fieldNames()).toIterable()
                .contains("email", "password", "username");
    }

    @Test
    void pathVariableConstraintViolationReturns400() throws Exception {
        mockMvc.perform(get("/api/cars/rating/9"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPathVariableTypeReturns400() throws Exception {
        mockMvc.perform(get("/api/cars/car/not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingResourceStillReturns404() throws Exception {
        mockMvc.perform(get("/api/cars/car/99999999"))
                .andExpect(status().isNotFound());
    }

    /**
     * The mobile client branches on this exact shape to offer "resend verification".
     * It is produced by a separate advice, so the catch-all must not preempt it.
     */
    @Test
    void unverifiedLoginReturns403WithErrorCode() throws Exception {
        String username = "unv_" + (System.nanoTime() % 1000000L);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s@example.com",
                                  "password": "Pass@1234"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk());

        String body = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Pass@1234"
                                }
                                """.formatted(username)))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("errorCode").asText()).isEqualTo("ACCOUNT_NOT_VERIFIED");
        assertThat(json.get("email").asText()).isEqualTo(username + "@example.com");
    }
}
