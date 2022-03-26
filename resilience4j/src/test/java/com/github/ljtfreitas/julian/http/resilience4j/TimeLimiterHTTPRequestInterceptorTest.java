package com.github.ljtfreitas.julian.http.resilience4j;

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestIO;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeLimiterHTTPRequestInterceptorTest {

    @Mock
    private HTTPRequest<String> request;

    @Mock
    private HTTPResponse<String> response;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Test
    void shouldCancelWhenTimeLimitIsReached() {
        TimeLimiter timeLimiter = TimeLimiter.of(Duration.ofMillis(2000));

        TimeLimiterHTTPRequestInterceptor interceptor = new TimeLimiterHTTPRequestInterceptor(timeLimiter, scheduler);

        when(request.execute()).then(i -> Promise.pending(() -> Except.just(() -> Thread.sleep(3000))
                .map(none -> response)
                .unsafe()));

        Promise<HTTPRequest<String>> limited = interceptor.intercepts(Promise.done(request));

        Exception exception = assertThrows(Exception.class, () -> limited.bind(HTTPRequestIO::execute).join().unsafe());

        assertThat(exception.getCause(), isA(TimeoutException.class));
    }

    @Test
    void shouldNotCancelBeforeTheTimeLimit() {
        TimeLimiter timeLimiter = TimeLimiter.of(Duration.ofMillis(2000));

        TimeLimiterHTTPRequestInterceptor interceptor = new TimeLimiterHTTPRequestInterceptor(timeLimiter, scheduler);

        when(response.body()).thenReturn(Except.success("success"));
        when(request.execute()).then(i -> Promise.pending(() -> Except.just(() -> Thread.sleep(1000))
                .map(none -> response)
                .unsafe()));

        Promise<HTTPRequest<String>> limited = interceptor.intercepts(Promise.done(request));

        String output = limited.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe();

        assertThat(output, equalTo(output));
    }
}