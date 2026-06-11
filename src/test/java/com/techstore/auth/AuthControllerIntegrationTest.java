package com.techstore.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.auth.repository.RefreshTokenRepository;
import com.techstore.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String EMAIL = "customer@techstore.test";
    private static final String PASSWORD = "OldPassword123";
    private static final String NEW_PASSWORD = "NewPassword456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerProfileAndUpdateProfile() throws Exception {
        JsonNode auth = register();
        String accessToken = auth.path("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.role").value("ROLE_CUSTOMER"));

        mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Updated Customer",
                                  "phone": "+84987654321"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Customer"))
                .andExpect(jsonPath("$.data.phone").value("+84987654321"));
    }

    @Test
    void rejectDuplicateRegistrationAndInvalidLogin() throws Exception {
        register();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void rotateRefreshTokenAndRejectReuse() throws Exception {
        JsonNode auth = register();
        String refreshToken = auth.path("refreshToken").asText();

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String rotatedToken = responseData(refreshed).path("refreshToken").asText();
        org.assertj.core.api.Assertions.assertThat(rotatedToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
    }

    @Test
    void changePasswordInvalidatesOldTokensAndAllowsNewLogin() throws Exception {
        JsonNode auth = register();
        String oldAccessToken = auth.path("accessToken").asText();
        String oldRefreshToken = auth.path("refreshToken").asText();

        mockMvc.perform(put("/api/auth/change-password")
                        .header("Authorization", bearer(oldAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s",
                                  "confirmPassword": "%s"
                                }
                                """.formatted(PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(oldAccessToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefreshToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void logoutInvalidatesCurrentTokens() throws Exception {
        JsonNode auth = register();
        String accessToken = auth.path("accessToken").asText();
        String refreshToken = auth.path("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateRequestsAndProtectPrivateEndpoints() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid",
                                  "password": "short",
                                  "fullName": "",
                                  "phone": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.fullName").exists())
                .andExpect(jsonPath("$.data.phone").exists());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    private JsonNode register() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("ROLE_CUSTOMER"))
                .andReturn();
        return responseData(result);
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String registerBody() {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "fullName": "TechStore Customer",
                  "phone": "+84912345678"
                }
                """.formatted(EMAIL, PASSWORD);
    }

    private String loginBody(String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(EMAIL, password);
    }

    private String refreshBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(
                java.util.Map.of("refreshToken", refreshToken)
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
