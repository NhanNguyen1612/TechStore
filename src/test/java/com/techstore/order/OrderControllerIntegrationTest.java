package com.techstore.order;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.techstore.order.entity.Order;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.repository.OrderRepository;
import com.techstore.order.repository.OrderStatusHistoryRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import com.techstore.wishlist.repository.WishlistRepository;
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
class OrderControllerIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

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
    void customerCreatesOrderFromCartWithItemsTotalsAndStockDeduction()
            throws Exception {
        String token = createUserAndLogin("buyer@order.test", Role.ROLE_CUSTOMER);
        Catalog catalog = createCatalog();
        Product laptop = createProduct(
                catalog,
                "Laptop",
                "ORDER-LAPTOP",
                "1000.00",
                10
        );
        Product mouse = createProduct(
                catalog,
                "Mouse",
                "ORDER-MOUSE",
                "25.50",
                20
        );
        addToCart(token, laptop.getId(), 2);
        addToCart(token, mouse.getId(), 3);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderCode")
                        .value(org.hamcrest.Matchers.matchesPattern(
                                "ORD-[0-9]{8}-[A-Z0-9]{10}"
                        )))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.totalQuantity").value(5))
                .andExpect(jsonPath("$.data.totalAmount").value(2076.5))
                .andReturn();
        long orderId = responseData(result).path("id").longValue();

        assertThat(productRepository.findById(laptop.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(8);
        assertThat(productRepository.findById(mouse.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(17);
        assertThat(cartRepository.count()).isEqualTo(1);
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalQuantity").value(0));

        mockMvc.perform(get("/api/orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(orderId));

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(1000.0));
    }

    @Test
    void insufficientStockRollsBackOrderStockChangesAndCartClear()
            throws Exception {
        String token = createUserAndLogin("rollback@order.test", Role.ROLE_CUSTOMER);
        Catalog catalog = createCatalog();
        Product first = createProduct(
                catalog,
                "First Product",
                "ORDER-FIRST",
                "100.00",
                5
        );
        Product second = createProduct(
                catalog,
                "Second Product",
                "ORDER-SECOND",
                "200.00",
                5
        );
        addToCart(token, first.getId(), 2);
        addToCart(token, second.getId(), 3);

        second.update(
                second.getName(),
                second.getSlug(),
                second.getSku(),
                second.getDescription(),
                second.getPrice(),
                2,
                second.getCategory(),
                second.getBrand()
        );
        productRepository.saveAndFlush(second);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        assertThat(orderRepository.count()).isZero();
        assertThat(productRepository.findById(first.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(5);
        assertThat(productRepository.findById(second.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(2);

        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.totalQuantity").value(5));
    }

    @Test
    void cancellingOrderRestoresStockAndPreventsRepeatedCancellation()
            throws Exception {
        String token = createUserAndLogin("cancel@order.test", Role.ROLE_CUSTOMER);
        Product product = createProduct(
                createCatalog(),
                "Cancelable Product",
                "ORDER-CANCEL",
                "300.00",
                6
        );
        addToCart(token, product.getId(), 4);
        long orderId = createOrder(token);

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());

        assertThat(productRepository.findById(product.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(6);

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        assertThat(productRepository.findById(product.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(6);
    }

    @Test
    void staffCanProgressStatusWhileCustomerCannotUseManagementActions()
            throws Exception {
        String customerToken = createUserAndLogin(
                "status@order.test",
                Role.ROLE_CUSTOMER
        );
        String staffToken = createUserAndLogin(
                "staff@order.test",
                Role.ROLE_STAFF
        );
        Product product = createProduct(
                createCatalog(),
                "Shipping Product",
                "ORDER-SHIPPING",
                "400.00",
                10
        );
        addToCart(customerToken, product.getId(), 1);
        long orderId = createOrder(customerToken);

        mockMvc.perform(put("/api/orders/{id}/confirm", orderId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(put("/api/orders/{id}/shipping", orderId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(put("/api/orders/{id}/confirm", orderId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(put("/api/orders/{id}/shipping", orderId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPING"));

        mockMvc.perform(put("/api/orders/{id}/delivered", orderId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.deliveredAt").isNotEmpty());

        mockMvc.perform(get("/api/orders/{id}/timeline", orderId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[1].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data[2].status").value("SHIPPING"))
                .andExpect(jsonPath("$.data[3].status").value("DELIVERED"));

        Order delivered = orderRepository.findById(orderId).orElseThrow();
        assertThat(delivered.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(historyRepository
                .findAllByOrderIdOrderByCreatedAtAscIdAsc(orderId))
                .hasSize(4);
    }

    @Test
    void trackingIncludesCheckoutPaymentAndOwnershipData() throws Exception {
        String ownerToken = createUserAndLogin(
                "tracking-owner@order.test",
                Role.ROLE_CUSTOMER
        );
        String outsiderToken = createUserAndLogin(
                "tracking-outsider@order.test",
                Role.ROLE_CUSTOMER
        );
        String adminToken = createUserAndLogin(
                "tracking-admin@order.test",
                Role.ROLE_ADMIN
        );
        Product product = createProduct(
                createCatalog(),
                "Tracked Product",
                "ORDER-TRACKED",
                "850000.00",
                5
        );
        addToCart(ownerToken, product.getId(), 2);

        MvcResult created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientName": "Nguyen Nhan",
                                  "phone": "0123456789",
                                  "shippingAddress": "Da Nang",
                                  "note": "Call before delivery",
                                  "paymentMethod": "COD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recipientName")
                        .value("Nguyen Nhan"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andReturn();
        JsonNode data = responseData(created);
        long orderId = data.path("id").longValue();
        String orderCode = data.path("orderCode").asText();

        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data[0].paymentStatus").value("UNPAID"));

        mockMvc.perform(get("/api/orders/code/{code}", orderCode)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId));

        mockMvc.perform(get("/api/orders/{id}/tracking", orderId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerName")
                        .value("Nguyen Nhan"))
                .andExpect(jsonPath("$.data.phone").value("0123456789"))
                .andExpect(jsonPath("$.data.shippingAddress")
                        .value("Da Nang"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.items[0].productName")
                        .value("Tracked Product"))
                .andExpect(jsonPath("$.data.timeline[0].status")
                        .value("PENDING"));

        mockMvc.perform(get("/api/orders/{id}/tracking", orderId)
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));

        mockMvc.perform(get("/api/orders/code/{code}", orderCode)
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders/{id}/tracking", orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderCode").value(orderCode));
    }

    @Test
    void customersSeeOnlyTheirOrdersWhileManagersSeeAllOrders()
            throws Exception {
        String firstToken = createUserAndLogin(
                "first@order.test",
                Role.ROLE_CUSTOMER
        );
        String secondToken = createUserAndLogin(
                "second@order.test",
                Role.ROLE_CUSTOMER
        );
        String adminToken = createUserAndLogin(
                "admin@order.test",
                Role.ROLE_ADMIN
        );
        Product product = createProduct(
                createCatalog(),
                "Private Order Product",
                "ORDER-PRIVATE",
                "150.00",
                10
        );
        addToCart(firstToken, product.getId(), 1);
        long orderId = createOrder(firstToken);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    private void addToCart(String token, Long productId, int quantity)
            throws Exception {
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "productId", productId,
                                "quantity", quantity
                        ))))
                .andExpect(status().isOk());
    }

    private long createOrder(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return responseData(result).path("id").longValue();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).path("data");
    }

    private Catalog createCatalog() {
        Category category = categoryRepository.saveAndFlush(
                new Category("Order Category", "order-category", null)
        );
        Brand brand = brandRepository.saveAndFlush(
                new Brand("Order Brand", "order-brand", null)
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
                "Order User",
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
        return responseData(result).path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
        orderRepository.deleteAll();
        wishlistRepository.deleteAll();
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
