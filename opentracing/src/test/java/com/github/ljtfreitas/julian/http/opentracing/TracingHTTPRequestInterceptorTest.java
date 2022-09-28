package com.github.ljtfreitas.julian.http.opentracing;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPMethod;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockSpan.LogEntry;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TracingHTTPRequestInterceptorTest {

    @Mock(answer = RETURNS_SELF)
    private HTTPRequest<String> request;

    private final HTTPResponse<String> response = HTTPResponse.success(new HTTPStatus(HTTPStatusCode.OK), HTTPHeaders.empty(), "hello");

    @Captor
    private ArgumentCaptor<HTTPHeaders> captor;

    private final MockTracer tracer = new MockTracer();

    private final TracingHTTPRequestInterceptor interceptor = new TracingHTTPRequestInterceptor(tracer);

    @BeforeEach
    void beforeEach() {
        when(request.path()).thenReturn(URI.create("http://whatever.com"));
        when(request.method()).thenReturn(HTTPMethod.GET);
        when(request.headers()).thenReturn(HTTPHeaders.empty());

        when(request.execute()).thenReturn(Promise.done(response));
    }

    @Nested
    class InjectOnRequest {

        private final MockSpan parent = tracer.buildSpan("test").start();

        @BeforeEach
        void beforeEach() {
            tracer.scopeManager().activate(parent);
        }

        @Test
        void shouldInjectHeaders() {
            Promise<HTTPRequest<String>> requestAsPromise = interceptor.intercepts(Promise.done(request));

            HTTPRequest<String> intercepted = requestAsPromise.join().unsafe();

            assertThat(intercepted, isA(TracingHTTPRequest.class));

            Promise<HTTPResponse<String>> responseAsPromise = intercepted.execute();

            assertSame(response, responseAsPromise.join().unsafe());

            verify(request).headers(captor.capture());

            Map<String, HTTPHeader> headers = captor.getValue().asMap();

            List<MockSpan> spans = tracer.finishedSpans();
            assertThat(spans, hasSize(1));

            MockSpan spanOfRequest = spans.get(0);

            assertThat(headers, allOf(
                    hasEntry("spanid", new HTTPHeader("spanid", Long.toString(spanOfRequest.context().spanId()))),
                    hasEntry("traceid", new HTTPHeader("traceid", Long.toString(spanOfRequest.context().traceId())))));

            assertThat(spanOfRequest.tags(), allOf(
                    hasEntry(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT),
                    hasEntry(equalTo(Tags.COMPONENT.getKey()), notNullValue()),
                    hasEntry(Tags.HTTP_METHOD.getKey(), request.method().name()),
                    hasEntry(Tags.HTTP_URL.getKey(), request.path().toString())));
        }

        @Test
        void shouldUseParentSpan() {
            try (Scope scope = tracer.activateSpan(parent)) {
                Promise<HTTPRequest<String>> requestAsPromise = interceptor.intercepts(Promise.done(request));

                HTTPRequest<String> intercepted = requestAsPromise.join().unsafe();

                assertThat(intercepted, isA(TracingHTTPRequest.class));

                Promise<HTTPResponse<String>> responseAsPromise = intercepted.execute();

                assertSame(response, responseAsPromise.join().unsafe());

            } finally {
                parent.finish();
            }

            verify(request).headers(captor.capture());

            Map<String, HTTPHeader> headers = captor.getValue().asMap();

            List<MockSpan> spans = tracer.finishedSpans();
            assertThat(spans, hasSize(2));

            MockSpan spanOfRequest = spans.get(0);

            assertThat(headers, allOf(
                    hasEntry("spanid", new HTTPHeader("spanid", Long.toString(spanOfRequest.context().spanId()))),
                    hasEntry("traceid", new HTTPHeader("traceid", Long.toString(spanOfRequest.context().traceId())))));

            assertThat(spanOfRequest.tags(), allOf(
                    hasEntry(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT),
                    hasEntry(equalTo(Tags.COMPONENT.getKey()), notNullValue()),
                    hasEntry(Tags.HTTP_METHOD.getKey(), request.method().name()),
                    hasEntry(Tags.HTTP_URL.getKey(), request.path().toString())));
        }

        @Test
        void shouldInjectHeadersEvenInCaseOfExceptions() {
            when(request.headers(any())).thenReturn(request);

            RuntimeException failure = new RuntimeException("oops");
            Promise<HTTPResponse<String>> failed = Promise.failed(failure);

            when(request.execute()).thenReturn(failed);

            Promise<HTTPRequest<String>> requestAsPromise = interceptor.intercepts(Promise.done(request));

            HTTPRequest<String> intercepted = requestAsPromise.join().unsafe();

            assertThat(intercepted, isA(TracingHTTPRequest.class));

            Promise<HTTPResponse<String>> responseAsPromise = intercepted.execute();

            assertSame(failed, responseAsPromise);

            List<MockSpan> spans = tracer.finishedSpans();
            assertThat(spans, hasSize(1));

            MockSpan spanOfRequest = spans.get(0);

            assertThat(spanOfRequest.tags(), allOf(
                    hasEntry(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT),
                    hasEntry(equalTo(Tags.COMPONENT.getKey()), notNullValue()),
                    hasEntry(Tags.HTTP_METHOD.getKey(), request.method().name()),
                    hasEntry(Tags.HTTP_URL.getKey(), request.path().toString())));

            assertThat(spanOfRequest.tags(), hasEntry(Tags.ERROR.getKey(), true));

            List<LogEntry> logs = spanOfRequest.logEntries();
            assertThat(logs, hasSize(1));

            LogEntry errorLog = logs.get(0);

            Map<String, String> fieldsAsString = errorLog.fields().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

            assertThat(fieldsAsString, allOf(
                    hasEntry("event", Tags.ERROR.getKey()),
                    hasEntry(equalTo("error.object"), allOf(
                            startsWith(failure.getClass().getCanonicalName()),
                            endsWith(failure.getMessage())))));
        }
    }
}