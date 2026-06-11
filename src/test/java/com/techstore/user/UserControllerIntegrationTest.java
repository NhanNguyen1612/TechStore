package com.techstore.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.RefreshTokenRepository;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import com.techstore.user.storage.AvatarStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    private static final String CUSTOMER_EMAIL = "customer@techstore.test";
    private static final String ADMIN_EMAIL = "admin@techstore.test";
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserManagementRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AvatarStorage avatarStorage;

    @BeforeEach
    void cleanDatabase() {
        profileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticatedUserCanViewOwnProfileButCannotViewAnotherUserById() throws Exception {
        JsonNode customerAuth = registerCustomer();
        Long customerId = customerAuth.path("user").path("id").longValue();
        String customerToken = customerAuth.path("accessToken").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(customerId))
                .andExpect(jsonPath("$.data.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.data.role").value("ROLE_CUSTOMER"));

        mockMvc.perform(get("/api/users/{id}", customerId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void administratorCanViewAndUpdateUserRoleAndOldTokenIsInvalidated() throws Exception {
        JsonNode customerAuth = registerCustomer();
        Long customerId = customerAuth.path("user").path("id").longValue();
        String oldCustomerToken = customerAuth.path("accessToken").asText();
        String adminToken = createAdminAndLogin();

        mockMvc.perform(get("/api/users/{id}", customerId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(CUSTOMER_EMAIL));

        mockMvc.perform(put("/api/users/{id}", customerId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Store Staff",
                                  "phone": "+84987654321",
                                  "role": "ROLE_STAFF",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Store Staff"))
                .andExpect(jsonPath("$.data.role").value("ROLE_STAFF"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(oldCustomerToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(CUSTOMER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("ROLE_STAFF"));
    }

    @Test
    void cannotDisableOrDemoteTheLastActiveAdministrator() throws Exception {
        User admin = createAdmin();
        String adminToken = login(ADMIN_EMAIL).path("accessToken").asText();

        mockMvc.perform(put("/api/users/{id}", admin.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Main Admin",
                                  "phone": "",
                                  "role": "ROLE_CUSTOMER",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMIN_PROTECTED"));
    }

    @Test
    void uploadAvatarPersistsUrlAndAuditUser() throws Exception {
        JsonNode customerAuth = registerCustomer();
        Long customerId = customerAuth.path("user").path("id").longValue();
        String customerToken = customerAuth.path("accessToken").asText();
        when(avatarStorage.upload(anyLong(), any()))
                .thenReturn(new AvatarStorage.StoredAvatar(
                        "https://res.cloudinary.com/test/avatar.webp",
                        "techstore-test/avatars/user-avatar"
                ));

        MockMultipartFile avatar = new MockMultipartFile(
                "avatar",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/users/avatar")
                        .file(avatar)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl")
                        .value("https://res.cloudinary.com/test/avatar.webp"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl")
                        .value("https://res.cloudinary.com/test/avatar.webp"))
                .andExpect(jsonPath("$.data.avatarCreatedBy").value(customerId))
                .andExpect(jsonPath("$.data.avatarUpdatedBy").value(customerId));
    }

    @Test
    void rejectMissingAndUnsupportedAvatar() throws Exception {
        String customerToken = registerCustomer().path("accessToken").asText();

        mockMvc.perform(multipart("/api/users/avatar")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AVATAR_REQUIRED"));

        MockMultipartFile textFile = new MockMultipartFile(
                "avatar",
                "avatar.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/users/avatar")
                        .file(textFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AVATAR_TYPE"));
    }

    private JsonNode registerCustomer() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "fullName": "TechStore Customer",
                                  "phone": "+84912345678"
                                }
                                """.formatted(CUSTOMER_EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return responseData(result);
    }

    private String createAdminAndLogin() throws Exception {
        createAdmin();
        return login(ADMIN_EMAIL).path("accessToken").asText();
    }

    private User createAdmin() {
        return userRepository.saveAndFlush(new User(
                ADMIN_EMAIL,
                passwordEncoder.encode(PASSWORD),
                "Main Admin",
                null,
                Role.ROLE_ADMIN
        ));
    }

    private JsonNode login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result);
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String loginBody(String email) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, PASSWORD);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
