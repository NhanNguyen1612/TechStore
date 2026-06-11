package com.techstore.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.techstore.chat.repository.ConversationRepository;
import com.techstore.chat.repository.MessageRepository;
import com.techstore.order.entity.Order;
import com.techstore.order.repository.OrderRepository;
import com.techstore.payment.entity.Payment;
import com.techstore.payment.repository.MomoCallbackRepository;
import com.techstore.payment.repository.PaymentRepository;
import com.techstore.payment.repository.PaymentTransactionRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.review.repository.ReviewRepository;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import com.techstore.wishlist.repository.WishlistRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class DashboardControllerIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MomoCallbackRepository callbackRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

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
    void adminSeesRevenueOrdersProductsAndCustomers() throws Exception {
        String adminToken = createUserAndLogin(
                "admin@dashboard.test",
                Role.ROLE_ADMIN
        );
        createUser("staff@dashboard.test", Role.ROLE_STAFF);
        User firstCustomer = createUser(
                "first@dashboard.test",
                Role.ROLE_CUSTOMER
        );
        User secondCustomer = createUser(
                "second@dashboard.test",
                Role.ROLE_CUSTOMER
        );
        Catalog catalog = createCatalog();
        Product laptop = createProduct(
                catalog,
                "Dashboard Laptop",
                "DASH-LAPTOP",
                "1000.00",
                10
        );
        Product mouse = createProduct(
                catalog,
                "Dashboard Mouse",
                "DASH-MOUSE",
                "500.00",
                0
        );

        Order momoOrder = createOrder(
                "DASH-MOMO",
                firstCustomer,
                laptop,
                2
        );
        momoOrder.addItem(mouse, 1);
        momoOrder.markPaid();
        orderRepository.saveAndFlush(momoOrder);
        Payment payment = new Payment(momoOrder, 2500L);
        payment.beginAttempt("REQ-DASH", "MOMO-DASH");
        payment.markPaid(123456L, 0, "Paid", Instant.now());
        paymentRepository.saveAndFlush(payment);

        Order codOrder = createOrder(
                "DASH-COD",
                secondCustomer,
                mouse,
                3
        );
        codOrder.confirm(Instant.now());
        orderRepository.saveAndFlush(codOrder);

        Order cancelled = createOrder(
                "DASH-CANCELLED",
                firstCustomer,
                laptop,
                1
        );
        cancelled.cancel(Instant.now());
        orderRepository.saveAndFlush(cancelled);

        createOrder("DASH-PENDING", secondCustomer, laptop, 1);

        mockMvc.perform(get("/api/dashboard/overview")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(4000.0))
                .andExpect(jsonPath("$.data.momoRevenue").value(2500.0))
                .andExpect(jsonPath("$.data.codRevenue").value(1500.0))
                .andExpect(jsonPath("$.data.totalOrders").value(4))
                .andExpect(jsonPath("$.data.totalUsers").value(4))
                .andExpect(jsonPath("$.data.totalCustomers").value(2))
                .andExpect(jsonPath("$.data.cancelledOrders").value(1))
                .andExpect(jsonPath("$.data.cancellationRate").value(25.0));

        mockMvc.perform(get("/api/dashboard/revenue")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(4000.0));

        mockMvc.perform(get("/api/dashboard/orders")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(4))
                .andExpect(jsonPath(
                        "$.data.byStatus[?(@.status == 'PAID')].count"
                ).value(1))
                .andExpect(jsonPath(
                        "$.data.byStatus[?(@.status == 'CONFIRMED')].count"
                ).value(1))
                .andExpect(jsonPath(
                        "$.data.byStatus[?(@.status == 'CANCELLED')].count"
                ).value(1));

        mockMvc.perform(get("/api/dashboard/products")
                        .param("limit", "1")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProducts").value(2))
                .andExpect(jsonPath("$.data.outOfStockProducts").value(1))
                .andExpect(jsonPath("$.data.topSellingProducts.length()")
                        .value(1))
                .andExpect(jsonPath(
                        "$.data.topSellingProducts[0].productName"
                ).value("Dashboard Mouse"))
                .andExpect(jsonPath(
                        "$.data.topSellingProducts[0].quantitySold"
                ).value(4))
                .andExpect(jsonPath(
                        "$.data.topSellingProducts[0].revenue"
                ).value(2000.0));

        mockMvc.perform(get("/api/dashboard/customers")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCustomers").value(2))
                .andExpect(jsonPath("$.data.newCustomers").value(2))
                .andExpect(jsonPath("$.data.topCustomers[0].customerId")
                        .value(firstCustomer.getId()))
                .andExpect(jsonPath("$.data.topCustomers[0].totalOrders")
                        .value(1))
                .andExpect(jsonPath("$.data.topCustomers[0].totalSpent")
                        .value(2500.0));
    }

    @Test
    void dashboardIsAdminOnlyAndValidatesQueries() throws Exception {
        String adminToken = createUserAndLogin(
                "query-admin@dashboard.test",
                Role.ROLE_ADMIN
        );
        String staffToken = createUserAndLogin(
                "query-staff@dashboard.test",
                Role.ROLE_STAFF
        );
        String customerToken = createUserAndLogin(
                "query-customer@dashboard.test",
                Role.ROLE_CUSTOMER
        );

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/overview")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/dashboard/overview")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dashboard/revenue")
                        .param("from", "2026-06-08")
                        .param("to", "2026-06-07")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

        mockMvc.perform(get("/api/dashboard/products")
                        .param("limit", "51")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_DASHBOARD_QUERY"));

        mockMvc.perform(get("/api/dashboard/orders")
                        .param("from", "not-a-date")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_DASHBOARD_QUERY"));
    }

    @Test
    void futureDateRangeReturnsZeroPeriodStatistics() throws Exception {
        String adminToken = createUserAndLogin(
                "future-admin@dashboard.test",
                Role.ROLE_ADMIN
        );
        createUser("future-customer@dashboard.test", Role.ROLE_CUSTOMER);

        mockMvc.perform(get("/api/dashboard/overview")
                        .param("from", "2099-01-01")
                        .param("to", "2099-12-31")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(0.0))
                .andExpect(jsonPath("$.data.totalOrders").value(0))
                .andExpect(jsonPath("$.data.cancelledOrders").value(0))
                .andExpect(jsonPath("$.data.cancellationRate").value(0.0))
                .andExpect(jsonPath("$.data.totalUsers").value(2));

        mockMvc.perform(get("/api/dashboard/customers")
                        .param("from", "2099-01-01")
                        .param("to", "2099-12-31")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCustomers").value(1))
                .andExpect(jsonPath("$.data.newCustomers").value(0))
                .andExpect(jsonPath("$.data.topCustomers").isEmpty());
    }

    private Order createOrder(
            String code,
            User customer,
            Product product,
            int quantity
    ) {
        Order order = new Order(code, customer);
        order.addItem(product, quantity);
        return orderRepository.saveAndFlush(order);
    }

    private Catalog createCatalog() {
        Category category = categoryRepository.saveAndFlush(
                new Category(
                        "Dashboard Category",
                        "dashboard-category",
                        null
                )
        );
        Brand brand = brandRepository.saveAndFlush(
                new Brand("Dashboard Brand", "dashboard-brand", null)
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
        createUser(email, role);
        return login(email);
    }

    private User createUser(String email, Role role) {
        return userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                "Dashboard User",
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
        JsonNode data = objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).path("data");
        return data.path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        reviewRepository.deleteAll();
        callbackRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
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
