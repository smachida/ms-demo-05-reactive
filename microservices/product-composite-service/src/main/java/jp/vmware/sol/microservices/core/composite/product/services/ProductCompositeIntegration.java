package jp.vmware.sol.microservices.core.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.vmware.sol.api.core.product.Product;
import jp.vmware.sol.api.core.product.ProductService;
import jp.vmware.sol.api.core.recommendation.Recommendation;
import jp.vmware.sol.api.core.recommendation.RecommendationService;
import jp.vmware.sol.api.core.review.Review;
import jp.vmware.sol.api.core.review.ReviewService;
import jp.vmware.sol.api.event.Event;
import jp.vmware.sol.util.exceptions.InvalidInputException;
import jp.vmware.sol.util.exceptions.NotFoundException;
import jp.vmware.sol.util.http.HttpErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.retry.MessageKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static jp.vmware.sol.api.event.Event.Type.CREATE;
import static jp.vmware.sol.api.event.Event.Type.DELETE;
import static reactor.core.publisher.Flux.empty;

@EnableBinding(ProductCompositeIntegration.MessageSources.class)
@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final WebClient webClient;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    // 非同期メッセージング
    private MessageSources messageSources;

    public interface MessageSources {
        String OUTPUT_PRODUCTS = "output-products";
        String OUTPUT_RECOMMENDATIONS = "output-recommendations";
        String OUTPUT_REVIEWS = "output-reviews";

        @Output(OUTPUT_PRODUCTS)
        MessageChannel outputProducts();

        @Output(OUTPUT_RECOMMENDATIONS)
        MessageChannel outputRecommendation();

        @Output(OUTPUT_REVIEWS)
        MessageChannel outputReviews();
    }

    @Autowired
    public ProductCompositeIntegration(
        WebClient.Builder webClient,
        ObjectMapper mapper,
        MessageSources messageSources,
        @Value("${app.product-service.host}") String productServiceHost,
        @Value("${app.product-service.port}") String productServicePort,
        @Value("${app.recommendation-service.host}") String recommendationServiceHost,
        @Value("${app.recommendation-service.port}") String recommendationServicePort,
        @Value("${app.review-service.host}") String reviewServiceHost,
        @Value("${app.review-service.port}") String reviewServicePort
    ) {
        this.webClient = webClient.build();
        this.mapper = mapper;
        this.messageSources = messageSources;

        this.productServiceUrl =
                "http://" + productServiceHost + ":" + productServicePort;
        this.recommendationServiceUrl =
                "http://" + recommendationServiceHost + ":" + recommendationServicePort;
        this.reviewServiceUrl =
                "http://" + reviewServiceHost + ":" + reviewServicePort;
    }


    @Override
    public Product createProduct(Product product) {
        messageSources.outputProducts().send(
                MessageBuilder.withPayload(
                        new Event(CREATE, product.getProductId(), product)).build());
        return product;
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        String url = productServiceUrl + "/product/" + productId;
        LOG.debug("Will call the getProduct API on URL: {}", url);

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Product.class)
                .log()
                .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    @Override
    public void deleteProduct(int productId) {
        messageSources.outputProducts().send(
                MessageBuilder.withPayload(
                        new Event(DELETE, productId, null)).build());
    }

    @Override
    public Recommendation createRecommendation(Recommendation recommendation) {
        messageSources.outputRecommendation().send(
                MessageBuilder.withPayload(
                        new Event(CREATE, recommendation.getProductId(), recommendation)).build());
        return recommendation;
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        String url = recommendationServiceUrl + "/recommendation?productId=" + productId;
        LOG.debug("Will call the getRecommendations API on URL: {}", url);

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(Recommendation.class)
                .log()
                .onErrorResume(error -> empty());
    }

    @Override
    public void deleteRecommendations(int productId) {
        messageSources.outputRecommendation().send(
                MessageBuilder.withPayload(
                        new Event(DELETE, productId, null)).build());
    }

    @Override
    public Review createReview(Review review) {
        messageSources.outputReviews().send(
                MessageBuilder.withPayload(
                        new Event(CREATE, review.getProductId(), review)).build());
        return review;
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        String url = reviewServiceUrl + "/review?productId=" + productId;
        LOG.debug("Will call the getReviews API on URL: {}", url);

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(Review.class)
                .log()
                .onErrorResume(error -> empty());
    }

    @Override
    public void deleteReviews(int productId) {
        messageSources.outputReviews().send(
                MessageBuilder.withPayload(
                        new Event(DELETE, productId, null)).build());
    }

    // 死活監視メソッド
    public Mono<Health> getProductHealth() {
        return getHealth(productServiceUrl);
    }

    public Mono<Health> getRecommendationHealth() {
        return getHealth(recommendationServiceUrl);
    }

    public Mono<Health> getReviewHealth() {
        return getHealth(reviewServiceUrl);
    }

    private Mono<Health> getHealth(String url) {
        url += "/actuator/health";
        LOG.debug("Will call the Health API on URL: {}", url);
        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(s -> new Health.Builder().up().build())
                .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
                .log();
    }

    private Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException)ex;

        switch (wcre.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY :
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }

    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ioex.getMessage();
        }
    }
}
