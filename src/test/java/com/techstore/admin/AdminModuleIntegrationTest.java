package com.techstore.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
class AdminModuleIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String customerToken;
    private String marker;

    @BeforeEach
    void setUp() throws Exception {
        marker = UUID.randomUUID().toString().substring(0, 8);
        adminToken = createUserAndLogin("admin-" + marker + "@test.dev", Role.ROLE_ADMIN);
        customerToken = createUserAndLogin(
                "customer-" + marker + "@test.dev",
                Role.ROLE_CUSTOMER
        );
    }

    @Test
    void adminEndpointsRequireAdministratorRole() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void administratorCanQueryUsersManageCouponsAndReadDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .param("search", marker)
                        .param("role", "ROLE_CUSTOMER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].role").value("ROLE_CUSTOMER"));

        Instant startsAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant endsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        mockMvc.perform(post("/api/admin/coupons")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "code", "ADMIN-" + marker,
                                "name", "Admin integration coupon",
                                "type", "PERCENTAGE",
                                "value", 10,
                                "minimumOrderAmount", 100000,
                                "usageLimit", 50,
                                "startsAt", startsAt.toString(),
                                "endsAt", endsAt.toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(
                        "ADMIN-" + marker.toUpperCase()
                ))
                .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalRevenue").isNumber());
    }

    @Test
    void allAdminListAndDashboardQueriesAreExecutable() throws Exception {
        for (String path : java.util.List.of(
                "/api/admin/products",
                "/api/admin/categories",
                "/api/admin/brands",
                "/api/admin/orders",
                "/api/admin/payments",
                "/api/admin/reviews",
                "/api/admin/conversations",
                "/api/admin/notifications",
                "/api/admin/dashboard/overview",
                "/api/admin/dashboard/revenue",
                "/api/admin/dashboard/orders",
                "/api/admin/dashboard/products",
                "/api/admin/dashboard/customers",
                "/api/admin/dashboard/payments",
                "/api/admin/dashboard/reviews",
                "/api/admin/dashboard/inventory"
        )) {
            mockMvc.perform(get(path).header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    private String createUserAndLogin(String email, Role role) throws Exception {
        userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                "Admin module test",
                null,
                role
        ));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .path("data");
        return data.path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
