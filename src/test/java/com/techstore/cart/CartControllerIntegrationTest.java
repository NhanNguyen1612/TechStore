package com.techstore.cart;

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
import com.techstore.cart.repository.CartRepository;
import com.techstore.category.entity.Category;
import com.techstore.category.repository.CategoryRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
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
class CartControllerIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
    void cleanBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanAfter() {
        cleanDatabase();
    }

    @Test
    void customerCanManageCartAndTotals() throws Exception {
        String token = createUserAndLogin("customer@cart.test", Role.ROLE_CUSTOMER);
        Catalog catalog = createCatalog();
        Product laptop = createProduct(
                catalog,
                "Laptop",
                "LAPTOP-1",
                "100.00",
                10
        );
        Product mouse = createProduct(
                catalog,
                "Mouse",
                "MOUSE-1",
                "25.50",
                20
        );

        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalQuantity").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0.0));

        add(token, laptop.getId(), 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(200.0))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(200.0));

        add(token, laptop.getId(), 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        add(token, mouse.getId(), 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(5))
                .andExpect(jsonPath("$.data.totalAmount").value(351.0));

        update(token, laptop.getId(), 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(6))
                .andExpect(jsonPath("$.data.totalAmount").value(451.0));

        remove(token, mouse.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(4))
                .andExpect(jsonPath("$.data.totalAmount").value(400.0));

        mockMvc.perform(delete("/api/cart/clear")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalQuantity").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0.0));

        assertThat(cartRepository.count()).isEqualTo(1);
    }

    @Test
    void addAndUpdateRejectQuantitiesAboveStock() throws Exception {
        String token = createUserAndLogin("stock@cart.test", Role.ROLE_CUSTOMER);
        Catalog catalog = createCatalog();
        Product product = createProduct(
                catalog,
                "Limited Laptop",
                "LIMITED-1",
                "999.00",
                3
        );

        add(token, product.getId(), 2).andExpect(status().isOk());

        add(token, product.getId(), 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        update(token, product.getId(), 4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].availableStock").value(3));
    }

    @Test
    void validatesRequestsAndRejectsMissingOrDeletedProducts() throws Exception {
        String token = createUserAndLogin("validation@cart.test", Role.ROLE_CUSTOMER);
        Catalog catalog = createCatalog();
        Product product = createProduct(
                catalog,
                "Deleted Laptop",
                "DELETED-1",
                "500.00",
                5
        );

        add(token, product.getId(), 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        add(token, 999999L, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        product.softDelete(java.time.Instant.now());
        productRepository.saveAndFlush(product);

        add(token, product.getId(), 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        update(token, product.getId(), 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));

        remove(token, product.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
    }

    @Test
    void cartEndpointsAreCustomerOnlyAndCartsAreIsolated() throws Exception {
        String firstCustomer = createUserAndLogin(
                "first@cart.test",
                Role.ROLE_CUSTOMER
        );
        String secondCustomer = createUserAndLogin(
                "second@cart.test",
                Role.ROLE_CUSTOMER
        );
        String admin = createUserAndLogin("admin@cart.test", Role.ROLE_ADMIN);
        String staff = createUserAndLogin("staff@cart.test", Role.ROLE_STAFF);
        Product product = createProduct(
                createCatalog(),
                "Private Cart Product",
                "PRIVATE-1",
                "75.00",
                8
        );

        add(firstCustomer, product.getId(), 3).andExpect(status().isOk());

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", bearer(secondCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/cart").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/cart").header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private org.springframework.test.web.servlet.ResultActions add(
            String token,
            Long productId,
            int quantity
    ) throws Exception {
        return mockMvc.perform(post("/api/cart/add")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemBody(productId, quantity)));
    }

    private org.springframework.test.web.servlet.ResultActions update(
            String token,
            Long productId,
            int quantity
    ) throws Exception {
        return mockMvc.perform(put("/api/cart/update")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemBody(productId, quantity)));
    }

    private org.springframework.test.web.servlet.ResultActions remove(
            String token,
            Long productId
    ) throws Exception {
        return mockMvc.perform(delete("/api/cart/remove")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        java.util.Map.of("productId", productId)
                )));
    }

    private String itemBody(Long productId, int quantity) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "productId", productId,
                "quantity", quantity
        ));
    }

    private Catalog createCatalog() {
        Category category = categoryRepository.saveAndFlush(
                new Category("Cart Category", "cart-category", null)
        );
        Brand brand = brandRepository.saveAndFlush(
                new Brand("Cart Brand", "cart-brand", null)
        );
        return new Catalog(category, brand);
    }

    private Product createProduct(
            Catalog catalog,
            String name,
            String sku,
            String price,
            int stock
    ) {
        return productRepository.saveAndFlush(new Product(
                name,
                name.toLowerCase().replace(' ', '-'),
                sku,
                null,
                new BigDecimal(price),
                stock,
                catalog.category(),
                catalog.brand()
        ));
    }

    private String createUserAndLogin(String email, Role role) throws Exception {
        userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                "Cart User",
                null,
                role
        ));
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
        JsonNode data = objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).path("data");
        return data.path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        profileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private record Catalog(Category category, Brand brand) {
    }
}
