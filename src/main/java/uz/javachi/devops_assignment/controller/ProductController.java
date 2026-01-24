package uz.javachi.devops_assignment.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.javachi.devops_assignment.model.Product;
import uz.javachi.devops_assignment.service.ProductService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final Counter productRequestCounter;
    private final Counter productErrorCounter;

    public ProductController(ProductService productService, MeterRegistry meterRegistry) {
        this.productService = productService;
        
        this.productRequestCounter = Counter.builder("products.requests.total")
                .description("Total number of product API requests")
                .tag("api", "products")
                .register(meterRegistry);
                
        this.productErrorCounter = Counter.builder("products.errors.total")
                .description("Total number of product API errors")
                .tag("api", "products")
                .register(meterRegistry);
    }

    @GetMapping
    @Timed(value = "products.get.all", description = "Time taken to get all products")
    public ResponseEntity<?> getAllProducts() {
        log.info("Get all products endpoint called");
        productRequestCounter.increment();
        
        try {
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting products", e);
            productErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Timed(value = "products.get.byid", description = "Time taken to get product by ID")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        log.info("Get product by id endpoint called: {}", id);
        productRequestCounter.increment();
        
        try {
            Product product = productService.getProductById(id);
            if (product != null) {
                return ResponseEntity.ok(product);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting product by id", e);
            productErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    @Timed(value = "products.create", description = "Time taken to create product")
    public ResponseEntity<?> createProduct(@Valid @RequestBody Product product) {
        log.info("Create product endpoint called");
        productRequestCounter.increment();
        
        try {
            Product created = productService.createProduct(product);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating product", e);
            productErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Timed(value = "products.update", description = "Time taken to update product")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        log.info("Update product endpoint called: {}", id);
        productRequestCounter.increment();
        
        try {
            Product updated = productService.updateProduct(id, product);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating product", e);
            productErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Timed(value = "products.delete", description = "Time taken to delete product")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        log.info("Delete product endpoint called: {}", id);
        productRequestCounter.increment();
        
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting product", e);
            productErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/farmer/{farmerId}")
    @Timed(value = "products.get.byfarmer", description = "Time taken to get products by farmer")
    public ResponseEntity<?> getProductsByFarmer(@PathVariable String farmerId) {
        log.info("Get products by farmer endpoint called: {}", farmerId);
        productRequestCounter.increment();
        
        try {
            List<Product> products = productService.getProductsByFarmer(farmerId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting products by farmer", e);
            productErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/price")
    @Timed(value = "products.update.price", description = "Time taken to update product price")
    public ResponseEntity<?> updateProductPrice(@PathVariable Long id, @Valid @RequestBody PriceUpdateRequest request) {
        log.info("Update product price endpoint called: {}", id);
        productRequestCounter.increment();
        
        try {
            Product updated = productService.updateProductPrice(id, request.getPrice());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating product price", e);
            productErrorCounter.increment();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // DTO for price update request
    public static class PriceUpdateRequest {
        @jakarta.validation.constraints.NotNull(message = "Price is required")
        @jakarta.validation.constraints.Positive(message = "Price must be positive")
        private Double price;

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }
}
