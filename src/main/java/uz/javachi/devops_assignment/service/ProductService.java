package uz.javachi.devops_assignment.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.javachi.devops_assignment.model.Product;
import uz.javachi.devops_assignment.repository.ProductRepository;

import java.util.List;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final Counter productCreateCounter;
    private final Counter productUpdateCounter;
    private final Counter productDeleteCounter;
    private final Counter productErrorCounter;
    private final Counter productPriceUpdateCounter;
    private final Timer productQueryTimer;

    public ProductService(ProductRepository productRepository, 
                         NotificationService notificationService,
                         MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        
        this.productCreateCounter = Counter.builder("products.created.total")
                .description("Total number of products created")
                .tag("operation", "create")
                .register(meterRegistry);
                
        this.productUpdateCounter = Counter.builder("products.updated.total")
                .description("Total number of products updated")
                .tag("operation", "update")
                .register(meterRegistry);
                
        this.productDeleteCounter = Counter.builder("products.deleted.total")
                .description("Total number of products deleted")
                .tag("operation", "delete")
                .register(meterRegistry);
                
        this.productPriceUpdateCounter = Counter.builder("products.price.updated.total")
                .description("Total number of product price updates")
                .tag("operation", "price_update")
                .register(meterRegistry);
                
        this.productErrorCounter = Counter.builder("products.errors.total")
                .description("Total number of product operation errors")
                .tag("api", "products")
                .register(meterRegistry);
                
        this.productQueryTimer = Timer.builder("products.database.query.time")
                .description("Time taken for product database queries")
                .tag("operation", "query")
                .register(meterRegistry);
    }

    @Timed(value = "products.service.getAll", description = "Time to fetch all products")
    public List<Product> getAllProducts() {
        return productQueryTimer.record(() -> {
            log.info("Getting all products");
            return productRepository.findAll();
        });
    }

    @Timed(value = "products.service.getById", description = "Time to fetch product by ID")
    public Product getProductById(Long id) {
        return productQueryTimer.record(() -> {
            log.info("Getting product by id: {}", id);
            return productRepository.findById(id).orElse(null);
        });
    }

    @Timed(value = "products.service.create", description = "Time to create product")
    public Product createProduct(Product product) {
        return productQueryTimer.record(() -> {
            log.info("Creating new product: {}", product.getName());
            Product saved = productRepository.save(product);
            productCreateCounter.increment();
            return saved;
        });
    }

    @Timed(value = "products.service.update", description = "Time to update product")
    public Product updateProduct(Long id, Product product) {
        return productQueryTimer.record(() -> {
            log.info("Updating product: {}", id);
            Product existing = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
            
            existing.setName(product.getName());
            existing.setDescription(product.getDescription());
            existing.setPrice(product.getPrice());
            existing.setQuantity(product.getQuantity());
            existing.setCategory(product.getCategory());
            
            Product updated = productRepository.save(existing);
            productUpdateCounter.increment();
            return updated;
        });
    }

    @Timed(value = "products.service.delete", description = "Time to delete product")
    public void deleteProduct(Long id) {
        productQueryTimer.record(() -> {
            log.info("Deleting product: {}", id);
            if (!productRepository.existsById(id)) {
                throw new RuntimeException("Product not found with id: " + id);
            }
            productRepository.deleteById(id);
            productDeleteCounter.increment();
        });
    }

    @Timed(value = "products.service.getByFarmer", description = "Time to fetch products by farmer")
    public List<Product> getProductsByFarmer(String farmerId) {
        return productQueryTimer.record(() -> {
            log.info("Getting products for farmer: {}", farmerId);
            return productRepository.findByFarmerId(farmerId);
        });
    }

    @Timed(value = "products.service.updatePrice", description = "Time to update product price")
    public Product updateProductPrice(Long id, Double newPrice) {
        return productQueryTimer.record(() -> {
            log.info("Updating product price: {} to {}", id, newPrice);
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
            
            Double oldPrice = product.getPrice();
            
            // Only send notification if price actually changed
            if (oldPrice != null && !oldPrice.equals(newPrice)) {
                product.setPrice(newPrice);
                Product updated = productRepository.save(product);
                productPriceUpdateCounter.increment();
                
                // Send notification about price change
                notificationService.sendPriceUpdateNotification(updated, oldPrice);
                
                return updated;
            } else {
                // Price didn't change, just return existing product
                return product;
            }
        });
    }
}
