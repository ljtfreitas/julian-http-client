package com.github.ljtfreitas.julian.http.resilience4j;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.FailureHTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestIO;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterHTTPRequestInterceptorTest {

    @Mock
    private HTTPRequest<String> request;

    @Mock
    private HTTPResponse<String> response;

    @Test
    void shouldRejectCallsWhenReachTheLimit(TestReporter reporter) {
        RateLimiter rateLimiter = RateLimiter.of("rateLimiter", RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(50))
                .limitRefreshPeriod(Duration.ofMillis(5000))
                .limitForPeriod(1)
                .build());

        rateLimiter.getEventPublisher()
                .onSuccess(e -> reporter.publishEntry("RateLimiterOnSuccess: " + e))
                .onFailure(e -> reporter.publishEntry("RateLimiterOnFailure: " + e));

        RateLimiterHTTPRequestInterceptor interceptor = new RateLimiterHTTPRequestInterceptor(rateLimiter);

        Promise<HTTPRequest<String>> limited = interceptor.intercepts(Promise.done(request));

        when(request.execute()).thenReturn(Promise.done(response));

        limited.bind(HTTPRequestIO::execute).join().unsafe();

        HTTPResponse<String> failed = limited.bind(HTTPRequestIO::execute).join().unsafe();

        assertThat(failed, isA(FailureHTTPResponse.class));
        assertThat(failed.status().is(HTTPStatusCode.TOO_MANY_REQUESTS), is(true));
    }

    @Test
    void shouldNotRejectCallsBeforeReachTheLimit(TestReporter reporter) {
        RateLimiter rateLimiter = RateLimiter.of("rateLimiter", RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(50))
                .limitRefreshPeriod(Duration.ofMillis(5000))
                .limitForPeriod(2)
                .build());

        rateLimiter.getEventPublisher()
                .onSuccess(e -> reporter.publishEntry("RateLimiterOnSuccess: " + e))
                .onFailure(e -> reporter.publishEntry("RateLimiterOnFailure: " + e));

        RateLimiterHTTPRequestInterceptor interceptor = new RateLimiterHTTPRequestInterceptor(rateLimiter);

        Promise<HTTPRequest<String>> limited = interceptor.intercepts(Promise.done(request));

        when(request.execute()).thenReturn(Promise.done(response));

        limited.bind(HTTPRequestIO::execute).join().unsafe();

        HTTPResponse<String> success = limited.bind(HTTPRequestIO::execute).join().unsafe();

        assertThat(success, sameInstance(response));
    }

    @Test
    void shouldNotObfuscateOtherExceptionsBeforeReachTheLimit(TestReporter reporter) {
        RateLimiter rateLimiter = RateLimiter.of("rateLimiter", RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(50))
                .limitRefreshPeriod(Duration.ofMillis(5000))
                .limitForPeriod(1)
                .build());

        rateLimiter.getEventPublisher()
                .onSuccess(e -> reporter.publishEntry("RateLimiterOnSuccess: " + e))
                .onFailure(e -> reporter.publishEntry("RateLimiterOnFailure: " + e));

        RateLimiterHTTPRequestInterceptor interceptor = new RateLimiterHTTPRequestInterceptor(rateLimiter);

        Promise<HTTPRequest<String>> limited = interceptor.intercepts(Promise.done(request));

        RuntimeException failure = new RuntimeException("ooops");

        when(request.execute()).thenReturn(Promise.failed(failure));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> limited.bind(HTTPRequestIO::execute).join().unsafe());

        assertThat(exception, sameInstance(failure));
    }
}