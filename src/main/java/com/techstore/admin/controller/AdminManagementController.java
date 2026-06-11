package com.techstore.admin.controller;

import com.techstore.admin.dto.request.AdminRequests;
import com.techstore.admin.dto.response.AdminResponses;
import com.techstore.admin.service.AdminManagementService;
import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.entity.Role;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.brand.dto.request.CreateBrandRequest;
import com.techstore.brand.dto.request.UpdateBrandRequest;
import com.techstore.category.dto.request.CreateCategoryRequest;
import com.techstore.category.dto.request.UpdateCategoryRequest;
import com.techstore.order.entity.OrderStatus;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.product.dto.request.CreateProductRequest;
import com.techstore.product.dto.request.UpdateProductRequest;
import com.techstore.review.entity.ReviewStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Management")
public class AdminManagementController {

    private final AdminManagementService service;

    public AdminManagementController(AdminManagementService service) {
        this.service = service;
    }

    @GetMapping("/users")
    @Operation(summary = "List and filter users")
    public ApiResponse<AdminResponses.PageData<AdminResponses.UserData>> users(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ok("Users retrieved", service.users(
                search, role, active, page, size, sort
        ));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user detail")
    public ApiResponse<AdminResponses.UserData> user(@PathVariable Long id) {
        return ok("User retrieved", service.user(id));
    }

    @PostMapping("/users")
    @Operation(summary = "Create user")
    public ResponseEntity<ApiResponse<AdminResponses.UserData>> createUser(
            @Valid @RequestBody AdminRequests.CreateUser request
    ) {
        return created("User created", service.createUser(request));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user")
    public ApiResponse<AdminResponses.UserData> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.UpdateUser request
    ) {
        return ok("User updated", service.updateUser(id, request));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Activate or deactivate user")
    public ApiResponse<AdminResponses.UserData> userStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.ChangeStatus request
    ) {
        return ok("User status updated", service.updateUserStatus(
                id, request.active()
        ));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Change user role")
    public ApiResponse<AdminResponses.UserData> userRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.ChangeRole request
    ) {
        return ok("User role updated", service.updateUserRole(
                id, request.role()
        ));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Soft delete user")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ApiResponse.success("User deleted");
    }

    @GetMapping("/products")
    @Operation(summary = "List and filter products")
    public ApiResponse<AdminResponses.PageData<AdminResponses.ProductData>> products(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ok("Products retrieved", service.products(
                search, categoryId, brandId, active, page, size, sort
        ));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get product detail")
    public ApiResponse<AdminResponses.ProductData> product(@PathVariable Long id) {
        return ok("Product retrieved", service.product(id));
    }

    @PostMapping(value = "/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create product from JSON")
    public ResponseEntity<ApiResponse<AdminResponses.ProductData>> createProductJson(
            @Valid @RequestBody CreateProductRequest request
    ) {
        return created("Product created", service.createProduct(request, List.of()));
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create product and upload multiple images")
    public ResponseEntity<ApiResponse<AdminResponses.ProductData>> createProductMultipart(
            @Valid @RequestPart("request") CreateProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return created("Product created", service.createProduct(request, images));
    }

    @PutMapping(
            value = "/products/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Update product from JSON")
    public ApiResponse<AdminResponses.ProductData> updateProductJson(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ok("Product updated", service.updateProduct(id, request, List.of()));
    }

    @PutMapping(
            value = "/products/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Update product and upload multiple images")
    public ApiResponse<AdminResponses.ProductData> updateProductMultipart(
            @PathVariable Long id,
            @Valid @RequestPart("request") UpdateProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ok("Product updated", service.updateProduct(id, request, images));
    }

    @PutMapping("/products/{id}/stock")
    @Operation(summary = "Update product stock")
    public ApiResponse<AdminResponses.ProductData> productStock(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.UpdateStock request
    ) {
        return ok("Stock updated", service.updateStock(
                id, request.stockQuantity()
        ));
    }

    @PutMapping("/products/{id}/status")
    @Operation(summary = "Activate or deactivate product")
    public ApiResponse<AdminResponses.ProductData> productStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.ChangeStatus request
    ) {
        return ok("Product status updated", service.updateProductStatus(
                id, request.active()
        ));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Soft delete product")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        service.deleteProduct(id);
        return ApiResponse.success("Product deleted");
    }

    @GetMapping("/categories")
    @Operation(summary = "List categories")
    public ApiResponse<AdminResponses.PageData<AdminResponses.TaxonomyData>> categories(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ok("Categories retrieved", service.categories(
                search, page, size, sort
        ));
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Get category")
    public ApiResponse<AdminResponses.TaxonomyData> category(@PathVariable Long id) {
        return ok("Category retrieved", service.category(id));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create category")
    public ResponseEntity<ApiResponse<AdminResponses.TaxonomyData>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return created("Category created", service.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update category")
    public ApiResponse<AdminResponses.TaxonomyData> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ok("Category updated", service.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Soft delete unused category")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ApiResponse.success("Category deleted");
    }

    @GetMapping("/brands")
    @Operation(summary = "List brands")
    public ApiResponse<AdminResponses.PageData<AdminResponses.TaxonomyData>> brands(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ok("Brands retrieved", service.brands(search, page, size, sort));
    }

    @GetMapping("/brands/{id}")
    @Operation(summary = "Get brand")
    public ApiResponse<AdminResponses.TaxonomyData> brand(@PathVariable Long id) {
        return ok("Brand retrieved", service.brand(id));
    }

    @PostMapping("/brands")
    @Operation(summary = "Create brand")
    public ResponseEntity<ApiResponse<AdminResponses.TaxonomyData>> createBrand(
            @Valid @RequestBody CreateBrandRequest request
    ) {
        return created("Brand created", service.createBrand(request));
    }

    @PutMapping("/brands/{id}")
    @Operation(summary = "Update brand")
    public ApiResponse<AdminResponses.TaxonomyData> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBrandRequest request
    ) {
        return ok("Brand updated", service.updateBrand(id, request));
    }

    @DeleteMapping("/brands/{id}")
    @Operation(summary = "Soft delete unused brand")
    public ApiResponse<Void> deleteBrand(@PathVariable Long id) {
        service.deleteBrand(id);
        return ApiResponse.success("Brand deleted");
    }

    @GetMapping("/orders")
    @Operation(summary = "List and filter orders")
    public ApiResponse<AdminResponses.PageData<AdminResponses.OrderData>> orders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ok("Orders retrieved", service.orders(
                status, paymentMethod, from, to, page, size, sort
        ));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get order detail")
    public ApiResponse<AdminResponses.OrderData> order(@PathVariable Long id) {
        return ok("Order retrieved", service.order(id));
    }

    @PutMapping("/orders/{id}/confirm")
    @Operation(summary = "Confirm order")
    public ApiResponse<AdminResponses.OrderData> confirmOrder(@PathVariable Long id) {
        return transition(id, OrderStatus.CONFIRMED);
    }

    @PutMapping("/orders/{id}/shipping")
    @Operation(summary = "Start shipping")
    public ApiResponse<AdminResponses.OrderData> shippingOrder(@PathVariable Long id) {
        return transition(id, OrderStatus.SHIPPING);
    }

    @PutMapping("/orders/{id}/delivered")
    @Operation(summary = "Mark order delivered")
    public ApiResponse<AdminResponses.OrderData> deliveredOrder(@PathVariable Long id) {
        return transition(id, OrderStatus.DELIVERED);
    }

    @PutMapping("/orders/{id}/complete")
    @Operation(summary = "Complete order")
    public ApiResponse<AdminResponses.OrderData> completeOrder(@PathVariable Long id) {
        return transition(id, OrderStatus.COMPLETED);
    }

    @PutMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel order and restore stock")
    public ApiResponse<AdminResponses.OrderData> cancelOrder(@PathVariable Long id) {
        return transition(id, OrderStatus.CANCELLED);
    }

    @GetMapping("/payments")
    @Operation(summary = "List and filter payments")
    public ApiResponse<AdminResponses.PageData<AdminResponses.PaymentData>> payments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ok("Payments retrieved", service.payments(
                status, from, to, page, size, sort
        ));
    }

    @GetMapping("/payments/{id}")
    @Operation(summary = "Get payment transaction and callback history")
    public ApiResponse<AdminResponses.PaymentData> payment(@PathVariable Long id) {
        return ok("Payment retrieved", service.payment(id));
    }

    @GetMapping("/payments/order/{orderId}")
    @Operation(summary = "Get payment by order")
    public ApiResponse<AdminResponses.PaymentData> paymentByOrder(
            @PathVariable Long orderId
    ) {
        return ok("Payment retrieved", service.paymentByOrder(orderId));
    }

    @PutMapping("/payments/{id}/status")
    @Operation(summary = "Update payment status")
    public ApiResponse<AdminResponses.PaymentData> paymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.ChangePaymentStatus request
    ) {
        return ok("Payment status updated", service.updatePaymentStatus(
                id, request.status()
        ));
    }

    @GetMapping("/coupons")
    @Operation(summary = "List coupons")
    public ApiResponse<AdminResponses.PageData<AdminResponses.CouponData>> coupons(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ok("Coupons retrieved", service.coupons(
                search, active, page, size, sort
        ));
    }

    @GetMapping("/coupons/{id}")
    @Operation(summary = "Get coupon")
    public ApiResponse<AdminResponses.CouponData> coupon(@PathVariable Long id) {
        return ok("Coupon retrieved", service.coupon(id));
    }

    @PostMapping("/coupons")
    @Operation(summary = "Create coupon")
    public ResponseEntity<ApiResponse<AdminResponses.CouponData>> createCoupon(
            @Valid @RequestBody AdminRequests.UpsertCoupon request
    ) {
        return created("Coupon created", service.createCoupon(request));
    }

    @PutMapping("/coupons/{id}")
    @Operation(summary = "Update coupon")
    public ApiResponse<AdminResponses.CouponData> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.UpsertCoupon request
    ) {
        return ok("Coupon updated", service.updateCoupon(id, request));
    }

    @PutMapping("/coupons/{id}/status")
    @Operation(summary = "Activate or deactivate coupon")
    public ApiResponse<AdminResponses.CouponData> couponStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.ChangeStatus request
    ) {
        return ok("Coupon status updated", service.updateCouponStatus(
                id, request.active()
        ));
    }

    @DeleteMapping("/coupons/{id}")
    @Operation(summary = "Soft delete coupon")
    public ApiResponse<Void> deleteCoupon(@PathVariable Long id) {
        service.deleteCoupon(id);
        return ApiResponse.success("Coupon deleted");
    }

    @GetMapping("/reviews")
    @Operation(summary = "List and filter reviews")
    public ApiResponse<AdminResponses.PageData<AdminResponses.ReviewData>> reviews(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ok("Reviews retrieved", service.reviews(
                rating, status, page, size, sort
        ));
    }

    @GetMapping("/reviews/{id}")
    @Operation(summary = "Get review")
    public ApiResponse<AdminResponses.ReviewData> review(@PathVariable Long id) {
        return ok("Review retrieved", service.review(id));
    }

    @PutMapping("/reviews/{id}/approve")
    @Operation(summary = "Approve review")
    public ApiResponse<AdminResponses.ReviewData> approveReview(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ok("Review approved", service.moderateReview(
                id, ReviewStatus.APPROVED, principal.getId()
        ));
    }

    @PutMapping("/reviews/{id}/hide")
    @Operation(summary = "Hide review")
    public ApiResponse<AdminResponses.ReviewData> hideReview(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ok("Review hidden", service.moderateReview(
                id, ReviewStatus.HIDDEN, principal.getId()
        ));
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Delete review")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        service.deleteReview(id);
        return ApiResponse.success("Review deleted");
    }

    @GetMapping("/conversations")
    @Operation(summary = "List conversations")
    public ApiResponse<AdminResponses.PageData<AdminResponses.ConversationData>>
            conversations(
                    @RequestParam(required = false) Boolean closed,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size,
                    @RequestParam(defaultValue = "createdAt,desc") String sort
            ) {
        return ok("Conversations retrieved", service.conversations(
                closed, page, size, sort
        ));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get conversation")
    public ApiResponse<AdminResponses.ConversationData> conversation(
            @PathVariable Long id
    ) {
        return ok("Conversation retrieved", service.conversation(id));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Get conversation messages")
    public ApiResponse<AdminResponses.PageData<AdminResponses.MessageData>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ok("Messages retrieved", service.messages(id, page, size));
    }

    @PutMapping("/conversations/{id}/assign-staff")
    @Operation(summary = "Assign conversation to staff")
    public ApiResponse<AdminResponses.ConversationData> assignStaff(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequests.AssignStaff request
    ) {
        return ok("Staff assigned", service.assignStaff(id, request.staffId()));
    }

    @PutMapping("/conversations/{id}/close")
    @Operation(summary = "Close conversation")
    public ApiResponse<AdminResponses.ConversationData> closeConversation(
            @PathVariable Long id
    ) {
        return ok("Conversation closed", service.closeConversation(id));
    }

    @GetMapping("/notifications")
    @Operation(summary = "List notification history")
    public ApiResponse<AdminResponses.PageData<AdminResponses.NotificationData>>
            notifications(
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "20") int size
            ) {
        return ok("Notifications retrieved", service.notifications(page, size));
    }

    @PostMapping("/notifications")
    @Operation(summary = "Create notification for all users or a role")
    public ResponseEntity<ApiResponse<AdminResponses.NotificationData>>
            createNotification(
                    @Valid @RequestBody AdminRequests.CreateNotification request,
                    @AuthenticationPrincipal AuthUserPrincipal principal
            ) {
        return created("Notification created", service.createNotification(
                request, principal.getId()
        ));
    }

    @DeleteMapping("/notifications/{id}")
    @Operation(summary = "Delete notification history")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        service.deleteNotification(id);
        return ApiResponse.success("Notification deleted");
    }

    private ApiResponse<AdminResponses.OrderData> transition(
            Long id, OrderStatus status
    ) {
        return ok("Order status updated", service.transitionOrder(id, status));
    }

    private <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.success(message, data);
    }

    private <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, data));
    }
}
