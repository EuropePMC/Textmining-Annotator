package uk.ac.ebi.literature.textminingapi;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RetryConfig {

    private static final int MAX_RETRY_ATTEMPTS = 4;
    private static final int MAX_TIME_WAIT = 10000;
    private static final int INTERVAL = 100;
    private static final int INIT_INTERVAL = 100;

    private final SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(MAX_RETRY_ATTEMPTS);
    private final ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
    private final NeverRetryPolicy neverRetryPolicy = new NeverRetryPolicy();

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        return builder.build();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExceptionClassifierRetryPolicy policy = new ExceptionClassifierRetryPolicy();
        policy.setExceptionClassifier(this.configureStatusCodeBasedRetryPolicy());
        exponentialBackOffPolicy.setInitialInterval(INIT_INTERVAL);
        exponentialBackOffPolicy.setMaxInterval(MAX_TIME_WAIT);
        exponentialBackOffPolicy.setMultiplier(INTERVAL);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        retryTemplate.setRetryPolicy(policy);
        return retryTemplate;
    }

    private Classifier<Throwable, RetryPolicy> configureStatusCodeBasedRetryPolicy() {

        return throwable -> {
            if (throwable instanceof HttpStatusCodeException) {
                HttpStatusCodeException exception = (HttpStatusCodeException) throwable;
                return this.getRetryPolicyForStatus(exception.getStatusCode());
            }
            return simpleRetryPolicy;
        };
    }

    private RetryPolicy getRetryPolicyForStatus(HttpStatus httpStatus) {
        switch (httpStatus) {
            case BAD_GATEWAY:
            case SERVICE_UNAVAILABLE:
            case INTERNAL_SERVER_ERROR:
            case GATEWAY_TIMEOUT:
            case TOO_MANY_REQUESTS:
                return simpleRetryPolicy;
            default:
                return neverRetryPolicy;
        }
    }

}
