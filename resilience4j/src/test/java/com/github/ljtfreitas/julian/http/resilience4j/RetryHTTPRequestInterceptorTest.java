package com.github.ljtfreitas.julian.http.resilience4j;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.BadRequest;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestIO;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.InternalServerError;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.HTTPStatusGroup;
import com.github.ljtfreitas.julian.http.client.HTTPClientException;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryHTTPRequestInterceptorTest {

    @Mock
    private HTTPRequest<String> request;

    @Mock
    private HTTPResponse<String> response;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Test
    void shouldNotRetryOnSuccess() {
        Retry retry = Retry.ofDefaults("retry");

        RetryHTTPRequestInterceptor interceptor = new RetryHTTPRequestInterceptor(retry, scheduler);

        when(response.body()).thenReturn(Attempt.success("success"));
        when(request.execute()).thenReturn(Promise.done(response));

        Promise<HTTPRequest<String>> retryable = interceptor.intercepts(Promise.done(request));

        assertEquals("success", retryable.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldRetryOnException() {
        Retry retry = Retry.of("retry", RetryConfig.custom()
                .maxAttempts(3)
                .retryOnException(e -> e instanceof HTTPClientException)
                .build());

        RetryHTTPRequestInterceptor interceptor = new RetryHTTPRequestInterceptor(retry, scheduler);

        when(response.body()).thenReturn(Attempt.success("success"));
        when(request.execute()).thenReturn(
                Promise.failed(new HTTPClientException("oops", new IOException())),
                Promise.failed(new HTTPClientException("oops, again", new IOException())),
                Promise.done(response));

        Promise<HTTPRequest<String>> retryable = interceptor.intercepts(Promise.done(request));

        assertEquals("success", retryable.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());

        verify(request, times(3)).execute();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldRetryToUnacceptableResults() {
        Retry retry = Retry.of("retry", RetryConfig.<HTTPResponse<String>> custom()
                .maxAttempts(2)
                .retryOnResult(r -> r.status().is(HTTPStatusGroup.SERVER_ERROR))
                .build());

        RetryHTTPRequestInterceptor interceptor = new RetryHTTPRequestInterceptor(retry, scheduler);

        when(response.body()).thenReturn(Attempt.success("success"));
        when(response.status()).thenReturn(new HTTPStatus(HTTPStatusCode.OK));

        when(request.execute()).thenReturn(
                Promise.done(HTTPResponse.failed(new InternalServerError(HTTPHeaders.empty(), Promise.done("oops".getBytes())))),
                Promise.done(response));

        Promise<HTTPRequest<String>> retryable = interceptor.intercepts(Promise.done(request));

        assertEquals("success", retryable.bind(HTTPRequestIO::execute).join().unsafe().body().unsafe());

        verify(request, times(2)).execute();
    }

    @Test
    void shouldNotRetryWhenIgnoredExceptionsAreThrowed() {
        Retry retry = Retry.of("retry", RetryConfig.<HTTPResponse<String>> custom()
                .maxAttempts(3)
                .retryOnException(e -> e instanceof HTTPClientException)
                .build());

        RetryHTTPRequestInterceptor interceptor = new RetryHTTPRequestInterceptor(retry, scheduler);

        HTTPMessageException failure = new HTTPMessageException("oops", new RuntimeException());

        when(request.execute()).thenReturn(Promise.failed(failure));

        Promise<HTTPRequest<String>> failed = interceptor.intercepts(Promise.done(request));

        HTTPMessageException exception = assertThrows(HTTPMessageException.class, () -> failed.bind(HTTPRequestIO::execute).join().unsafe());

        assertSame(failure, exception);

        verify(request, times(1)).execute();
    }

    @Test
    void shouldNotRetryToAcceptableResults() {
        Retry retry = Retry.of("retry", RetryConfig.<HTTPResponse<String>> custom()
                .maxAttempts(3)
                .retryOnResult(r -> r.status().is(HTTPStatusGroup.SERVER_ERROR))
                .build());

        RetryHTTPRequestInterceptor interceptor = new RetryHTTPRequestInterceptor(retry, scheduler);

        HTTPResponse<String> badRequest = HTTPResponse.failed(new BadRequest(HTTPHeaders.empty(), Promise.done("oops".getBytes())));

        when(request.execute()).thenReturn(Promise.done(badRequest));

        Promise<HTTPRequest<String>> failed = interceptor.intercepts(Promise.done(request));

        HTTPResponse<String> actual = failed.bind(HTTPRequestIO::execute).join().unsafe();

        assertSame(badRequest, actual);

        verify(request, times(1)).execute();
    }

}