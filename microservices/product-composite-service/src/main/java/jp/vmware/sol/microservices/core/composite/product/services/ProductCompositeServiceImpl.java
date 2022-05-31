package jp.vmware.sol.microservices.core.composite.product.services;

import jp.vmware.sol.api.composite.product.*;
import jp.vmware.sol.api.core.product.Product;
import jp.vmware.sol.api.core.recommendation.Recommendation;
import jp.vmware.sol.api.core.review.Review;
import jp.vmware.sol.util.exceptions.NotFoundException;
import jp.vmware.sol.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);
    private ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;

    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public void createCompositeProduct(ProductAggregate body) {
        try {
            // トランザクション処理なし(部分更新の可能性あり)
            LOG.debug("createCompositeProduct: create a new composite entity for productId: {}", body.getProductId());
            Product newProduct = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            integration.createProduct(newProduct);

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(recommendation -> {
                    Recommendation newRecommendation =
                            new Recommendation(
                                    body.getProductId(),
                                    recommendation.getRecommendationId(),
                                    recommendation.getAuthor(),
                                    recommendation.getRate(),
                                    recommendation.getContent(),
                                    null);
                    integration.createRecommendation(newRecommendation);
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(review -> {
                    Review newReview =
                            new Review(
                                    body.getProductId(),
                                    review.getReviewId(),
                                    review.getAuthor(),
                                    review.getSubject(),
                                    review.getContent(),
                                    null);
                    integration.createReview(newReview);
                });
            }

            LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

        } catch (RuntimeException ex) {
            LOG.warn("createCompositeProduct failed", ex);
            throw ex;
        }
    }

    @Override
    public Mono<ProductAggregate> getCompositeProduct(int productId) {
        // Product, Recommendation, 及び Review サービス呼び出しの並行実行
        return Mono.zip(
                // lambda
                values -> createProductAggregate(
                        (Product) values[0],
                        (List<Recommendation>)values[1],
                        (List<Review>)values[2],
                        serviceUtil.getServiceAddress()),
                integration.getProduct(productId),
                integration.getRecommendations(productId).collectList(),
                integration.getReviews(productId).collectList()
        ).doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString())).log();
    }

    @Override
    public void deleteCompositeProduct(int productId) {
        // トランザクション処理なし(部分更新の可能性あり)
        LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);
        integration.deleteProduct(productId);
        integration.deleteRecommendations(productId);
        integration.deleteReviews(productId);
        LOG.debug("deleteCompositeProduct: aggregate entities deleted for productId: {}", productId);
    }

    private ProductAggregate createProductAggregate(
            Product product,
            List<Recommendation> recommendations,
            List<Review> reviews,
            String serviceAddress) {

        // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
                recommendations.stream()
                        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
                        .collect(Collectors.toList());

        // 3. Copy summary review info, if available
        List<ReviewSummary> reviewSummaries = (reviews == null)  ? null :
                reviews.stream()
                        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
                        .collect(Collectors.toList());

        // 4. Create info regarding the involved microservices addresses
        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);

    }
}
