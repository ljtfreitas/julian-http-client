package com.github.ljtfreitas.julian.http.resilience4j;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.BadRequest;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestIO;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.HTTPStatusGroup;
import com.github.ljtfreitas.julian.http.client.HTTPClientException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerHTTPRequestInterceptorTest {

    @Mock
    private HTTPRequest<String> request;

    @Mock
    private HTTPResponse<String> response;

    @Nested
    class ClosedToOpen {

        @SuppressWarnings("unchecked")
        @Test
        void shouldOpenTheCircuitWithTwoExecutions(TestReporter reporter) {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .minimumNumberOfCalls(2)
                    .failureRateThreshold(50.0f)
                    .build();

            CircuitBreaker circuitBreaker = CircuitBreaker.of("sample", config);

            circuitBreaker.getEventPublisher()
                    .onError(e -> reporter.publishEntry("CircuitBreakerOnError: " + e))
                    .onSuccess(e -> reporter.publishEntry("CircuitBreakerOnSuccess: " + e))
                    .onCallNotPermitted(e -> reporter.publishEntry("CircuitBreakerOnCallNotPermitted: " + e));

            CircuitBreakerHTTPRequestInterceptor interceptor = new CircuitBreakerHTTPRequestInterceptor(circuitBreaker);

            Promise<HTTPRequest<String>> promise = interceptor.intercepts(Promise.done(request));

            when(response.body()).thenReturn(Attempt.success("success"));
            when(response.status()).thenReturn(new HTTPStatus(HTTPStatusCode.OK));

            when(request.execute()).thenReturn(Promise.done(response), Promise.failed(new RuntimeException("ooops")));

            assertEquals("success", promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
            assertThrows(RuntimeException.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());

            assertThrows(CallNotPermittedException.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldOpenTheCircuitWith100Executions(TestReporter reporter) {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .minimumNumberOfCalls(100)
                    .failureRateThreshold(10.0f)
                    .build();

            CircuitBreaker circuitBreaker = CircuitBreaker.of("sample", config);

            circuitBreaker.getEventPublisher()
                    .onError(e -> reporter.publishEntry("CircuitBreakerOnError: " + e))
                    .onSuccess(e -> reporter.publishEntry("CircuitBreakerOnSuccess: " + e))
                    .onCallNotPermitted(e -> reporter.publishEntry("CircuitBreakerOnCallNotPermitted: " + e));

            CircuitBreakerHTTPRequestInterceptor interceptor = new CircuitBreakerHTTPRequestInterceptor(circuitBreaker);

            Promise<HTTPRequest<String>> promise = interceptor.intercepts(Promise.done(request));

            when(response.status()).thenReturn(new HTTPStatus(HTTPStatusCode.OK));

            Promise<HTTPResponse<String>>[] all = rangeClosed(1, 99)
                    .mapToObj(i -> (i % 2) == 0 ? Promise.done(response) : Promise.failed(new RuntimeException("ooops")))
                    .toArray(Promise[]::new);

            when(request.execute()).thenReturn(Promise.done(response), all);

            rangeClosed(1, 100).forEach(value -> promise.bind(HTTPRequestIO::execute).join());

            verify(request, times(100)).execute();

            assertThrows(CallNotPermittedException.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe());
        }
    }

    @Nested
    class OpenToHalfOpen {

        @SuppressWarnings("unchecked")
        @Test
        void shouldChangeToHalfOpenState(TestReporter reporter) throws InterruptedException {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .minimumNumberOfCalls(2)
                    .failureRateThreshold(50.0f)
                    .waitDurationInOpenState(Duration.ofMillis(2000))
                    .build();

            CircuitBreaker circuitBreaker = CircuitBreaker.of("sample", config);

            circuitBreaker.getEventPublisher()
                    .onError(e -> reporter.publishEntry("CircuitBreakerOnError: " + e))
                    .onSuccess(e -> reporter.publishEntry("CircuitBreakerOnSuccess: " + e))
                    .onCallNotPermitted(e -> reporter.publishEntry("CircuitBreakerOnCallNotPermitted: " + e));

            CircuitBreakerHTTPRequestInterceptor interceptor = new CircuitBreakerHTTPRequestInterceptor(circuitBreaker);

            Promise<HTTPRequest<String>> promise = interceptor.intercepts(Promise.done(request));

            when(response.body()).thenReturn(Attempt.success("success"));
            when(response.status()).thenReturn(new HTTPStatus(HTTPStatusCode.OK));

            when(request.execute()).thenReturn(Promise.done(response), Promise.failed(new RuntimeException("ooops")), Promise.done(response));

            assertEquals("success", promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
            assertThrows(RuntimeException.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());

            assertThrows(CallNotPermittedException.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());

            Thread.sleep(2000);

            assertEquals("success", promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
        }
    }

    @Test
    void anyKindOfExceptionMustBeHandledAsFailure(TestReporter reporter) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("sample", config);

        circuitBreaker.getEventPublisher()
                .onError(e -> reporter.publishEntry("CircuitBreakerOnError: " + e))
                .onSuccess(e -> reporter.publishEntry("CircuitBreakerOnSuccess: " + e))
                .onCallNotPermitted(e -> reporter.publishEntry("CircuitBreakerOnCallNotPermitted: " + e));

        CircuitBreakerHTTPRequestInterceptor interceptor = new CircuitBreakerHTTPRequestInterceptor(circuitBreaker);

        Promise<HTTPRequest<String>> promise = interceptor.intercepts(Promise.done(request));

        when(request.execute()).thenReturn(Promise.failed(new HTTPClientException("IO error", new IOException("oops"))));

        assertThrows(HTTPClientException.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
        assertThrows(HTTPClientException.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());

        assertThrows(CallNotPermittedException.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
    }

    @SuppressWarnings("unchecked")
    @Test
    void weAreAbleToSayWhatResponsesWeAreWorryAbout(TestReporter reporter) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("sample", config);

        circuitBreaker.getEventPublisher()
                .onError(e -> reporter.publishEntry("CircuitBreakerOnError: " + e))
                .onSuccess(e -> reporter.publishEntry("CircuitBreakerOnSuccess: " + e))
                .onCallNotPermitted(e -> reporter.publishEntry("CircuitBreakerOnCallNotPermitted: " + e));

        CircuitBreakerHTTPRequestInterceptor interceptor = new CircuitBreakerHTTPRequestInterceptor(circuitBreaker, r -> !r.status().is(HTTPStatusGroup.SERVER_ERROR));

        Promise<HTTPRequest<String>> promise = interceptor.intercepts(Promise.done(request));

        when(response.body()).thenReturn(Attempt.success("success"));
        when(response.status()).thenReturn(new HTTPStatus(HTTPStatusCode.OK));

        when(request.execute()).thenReturn(Promise.done(response), Promise.done(HTTPResponse.failed(new BadRequest(HTTPHeaders.empty(), Promise.done(new byte[0])))), Promise.done(response));

        assertEquals("success", promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
        assertThrows(BadRequest.class, () -> promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
        assertEquals("success", promise.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
    }

}