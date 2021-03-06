package jp.vmware.sol.api.core.product;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface ProductService {

    @PostMapping(
            value = "/product",
            consumes = "application/json",
            produces = "application/json"
    )
    Product createProduct(@RequestBody Product product);

    @GetMapping(
            value = "/product/{productId}",
            produces = "application/json"
    )
    Mono<Product> getProduct(@PathVariable int productId);

    @DeleteMapping(value = "/product/{productId}")
    void deleteProduct(@PathVariable int productId);
}
