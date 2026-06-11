package com.techstore.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.RefreshTokenRepository;
import com.techstore.brand.entity.Brand;
import com.techstore.brand.repository.BrandRepository;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BrandControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@techstore.test";
    private static final String CUSTOMER_EMAIL = "customer@techstore.test";
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserManagementRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        brandRepository.deleteAll();
        profileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void readApisArePublicButWriteApisRequireAdministrator() throws Exception {
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        JsonNode apple = createBrand(adminToken, "Apple", "Consumer electronics");

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Apple"));

        mockMvc.perform(get("/api/brands/{id}", apple.path("id").longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("apple"));

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody("Samsung", "Electronics")))
                .andExpect(status().isUnauthorized());

        String customerToken = createUserAndLogin(
                CUSTOMER_EMAIL,
                Role.ROLE_CUSTOMER
        );
        mockMvc.perform(post("/api/brands")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody("Samsung", "Electronics")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void administratorCanCreateReadAndUpdateBrandWithAuditFields() throws Exception {
        User admin = createUser(ADMIN_EMAIL, Role.ROLE_ADMIN);
        String adminToken = login(ADMIN_EMAIL);

        JsonNode created = createBrand(adminToken, "Asus", "Computer hardware");
        long brandId = created.path("id").longValue();
        assertThat(created.path("slug").asText()).isEqualTo("asus");
        assertThat(created.path("createdBy").longValue()).isEqualTo(admin.getId());

        mockMvc.perform(put("/api/brands/{id}", brandId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody("ASUS Republic of Gamers", "Gaming hardware")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("ASUS Republic of Gamers"))
                .andExpect(jsonPath("$.data.slug").value("asus-republic-of-gamers"))
                .andExpect(jsonPath("$.data.updatedBy").value(admin.getId()));
    }

    @Test
    void listSupportsPaginationSearchAndSorting() throws Exception {
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        createBrand(adminToken, "Apple", "Phones and computers");
        createBrand(adminToken, "Samsung", "Phones and displays");
        createBrand(adminToken, "Asus", "Gaming computers");
        createBrand(adminToken, "MSI", "Gaming computers");
        createBrand(adminToken, "Dell", "Business computers");
        createBrand(adminToken, "Lenovo", "Laptops and desktops");

        mockMvc.perform(get("/api/brands")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "name")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("Dell"))
                .andExpect(jsonPath("$.data.content[1].name").value("Lenovo"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(6))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        mockMvc.perform(get("/api/brands")
                        .param("search", "gaming")
                        .param("sortBy", "name")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("Asus"))
                .andExpect(jsonPath("$.data.content[1].name").value("MSI"));
    }

    @Test
    void rejectsDuplicateNamesInvalidBodiesAndInvalidPagination() throws Exception {
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        createBrand(adminToken, "Dell", "Computer manufacturer");

        mockMvc.perform(post("/api/brands")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody("  dell  ", "Duplicate")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BRAND_NAME_EXISTS"));

        mockMvc.perform(post("/api/brands")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody("", "Invalid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/brands")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BRAND_QUERY"));

        mockMvc.perform(get("/api/brands")
                        .param("sortBy", "deleted"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BRAND_QUERY"));
    }

    @Test
    void deleteIsSoftAndDeletedBrandIsHiddenFromPublicApis() throws Exception {
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        JsonNode lenovo = createBrand(adminToken, "Lenovo", "Computer manufacturer");
        long brandId = lenovo.path("id").longValue();

        mockMvc.perform(delete("/api/brands/{id}", brandId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        Brand deletedBrand = brandRepository.findById(brandId).orElseThrow();
        assertThat(deletedBrand.isDeleted()).isTrue();
        assertThat(deletedBrand.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/api/brands/{id}", brandId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRAND_NOT_FOUND"));

        mockMvc.perform(get("/api/brands").param("search", "Lenovo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        createBrand(adminToken, "Lenovo", "Recreated after soft delete");
        assertThat(brandRepository.count()).isEqualTo(2);
    }

    private JsonNode createBrand(
            String token,
            String name,
            String description
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brands")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody(name, description)))
                .andExpect(status().isCreated())
                .andReturn();
        return responseData(result);
    }

    private String createUserAndLogin(String email, Role role) throws Exception {
        createUser(email, role);
        return login(email);
    }

    private User createUser(String email, Role role) {
        return userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                role == Role.ROLE_ADMIN ? "Brand Admin" : "Brand Customer",
                null,
                role
        ));
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("accessToken").asText();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String brandBody(String name, String description) throws Exception {
        return objectMapper.writeValueAsString(
                java.util.Map.of(
                        "name", name,
                        "description", description == null ? "" : description
                )
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
