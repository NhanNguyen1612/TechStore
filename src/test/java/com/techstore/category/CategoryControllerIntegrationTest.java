package com.techstore.category;

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
import com.techstore.category.entity.Category;
import com.techstore.category.repository.CategoryRepository;
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
class CategoryControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@techstore.test";
    private static final String CUSTOMER_EMAIL = "customer@techstore.test";
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

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
        categoryRepository.deleteAll();
        profileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void onlyAdministratorCanAccessCategoryApis() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());

        String customerToken = createUserAndLogin(
                CUSTOMER_EMAIL,
                Role.ROLE_CUSTOMER
        );
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody("Laptop", "Portable computers")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanCreateReadAndUpdateCategoryWithAuditFields() throws Exception {
        User admin = createUser(ADMIN_EMAIL, Role.ROLE_ADMIN);
        String adminToken = login(ADMIN_EMAIL);

        JsonNode created = createCategory(
                adminToken,
                "Điện thoại",
                "Smartphones and mobile devices"
        );
        long categoryId = created.path("id").longValue();
        assertThat(created.path("slug").asText()).isEqualTo("dien-thoai");
        assertThat(created.path("createdBy").longValue()).isEqualTo(admin.getId());

        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Điện thoại"))
                .andExpect(jsonPath("$.data.slug").value("dien-thoai"));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(
                                "Điện thoại thông minh",
                                "Updated description"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Điện thoại thông minh"))
                .andExpect(jsonPath("$.data.slug").value("dien-thoai-thong-minh"))
                .andExpect(jsonPath("$.data.updatedBy").value(admin.getId()));
    }

    @Test
    void listSupportsPaginationSearchAndSorting() throws Exception {
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        createCategory(adminToken, "Smartphone", "Premium phone");
        createCategory(adminToken, "Phone Accessories", "Chargers and cases");
        createCategory(adminToken, "Laptop", "Portable computer");

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearer(adminToken))
                        .param("search", "phone")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sortBy", "name")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Phone Accessories"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearer(adminToken))
                        .param("search", "portable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Laptop"));
    }

    @Test
    void rejectsDuplicateNamesAndInvalidInput() throws Exception {
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        createCategory(adminToken, "Laptop", null);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody("  laptop  ", "Duplicate")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_EXISTS"));

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody("", "Invalid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearer(adminToken))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY_QUERY"));
    }

    @Test
    void deleteIsSoftAndDeletedCategoryIsHiddenFromApis() throws Exception {
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        JsonNode created = createCategory(adminToken, "Tablet", "Touch devices");
        long categoryId = created.path("id").longValue();

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Category deletedCategory = categoryRepository.findById(categoryId).orElseThrow();
        assertThat(deletedCategory.isDeleted()).isTrue();
        assertThat(deletedCategory.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearer(adminToken))
                        .param("search", "Tablet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        createCategory(adminToken, "Tablet", "Recreated after soft delete");
        assertThat(categoryRepository.count()).isEqualTo(2);
    }

    private JsonNode createCategory(
            String token,
            String name,
            String description
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(name, description)))
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
                role == Role.ROLE_ADMIN ? "Category Admin" : "Category Customer",
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

    private String categoryBody(String name, String description) throws Exception {
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
