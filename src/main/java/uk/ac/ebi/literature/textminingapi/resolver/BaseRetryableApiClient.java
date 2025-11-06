package uk.ac.ebi.literature.textminingapi.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
public class BaseRetryableApiClient {

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @SuppressWarnings("rawtypes")
    protected final <T> T get(String url, HttpEntity httpEntity,
                              Class<T> responseType, Object... urlVariables) {
        try {
            ResponseEntity<T> responseEntity = retryTemplate
                    .execute(context -> restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                            responseType, urlVariables));
            return responseEntity.getBody();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw ex;
            }
            return (T) ex.getMessage();
        }
    }

    protected final HttpStatus getResponse(String url, HttpEntity httpEntity,
                                           Class<?> responseType, Object... urlVariables) {
        try {
            ResponseEntity<?> responseEntity = retryTemplate
                    .execute(context -> restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                            responseType, urlVariables));
            return responseEntity.getStatusCode();
        } catch (HttpClientErrorException ex) {
            return ex.getStatusCode();
        }
    }

}