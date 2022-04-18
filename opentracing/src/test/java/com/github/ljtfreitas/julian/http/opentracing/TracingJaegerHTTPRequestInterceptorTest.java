package com.github.ljtfreitas.julian.http.opentracing;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.QueryParameters;
import com.github.ljtfreitas.julian.http.DefaultHTTP;
import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPMethod;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.client.DefaultHTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientException;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.json.jsonp.JsonPHTTPMessageCodec;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.opentracing.Tracer;
import io.opentracing.tag.Tag;
import io.opentracing.tag.Tags;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.apache.thrift.transport.TTransportException;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TracingJaegerHTTPRequestInterceptorTest {

    private final JaegerAllInOneContainer jaegerContainer = new JaegerAllInOneContainer().init(); {
        jaegerContainer.start();
    };

    private final MockServerContainer mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:latest")); {
        mockServerContainer.start();
    };

    private final MockServerClient mockServer = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());

    private final Function<String, Predicate<JsonObject>> hasKey = expected -> j -> j.getString("key").equals(expected);
    private final Function<Tag<?>, Predicate<JsonObject>> hasTagWithName = hasKey.compose(Tag::getKey);
    private final Function<String, Predicate<JsonObject>> hasStringValue = expected -> j -> j.getString("value").equals(expected);
    private final Function<Integer, Predicate<JsonObject>> hasIntValue = expected -> j -> j.getInt("value") == expected;
    private final Function<Matcher<String>, Predicate<JsonObject>> hasStringValueMatching = matcher -> j -> matcher.matches(j.getString("value"));

    @BeforeEach
    void before() {
        mockServer.when(request("/success").withHeader("uber-trace-id", ".*"))
                .respond(response("hello").withStatusCode(200));

        mockServer.when(request("/error").withHeader("uber-trace-id", ".*"))
                .respond(response().withStatusCode(500));
    }

    @AfterEach
    void after() {
        mockServerContainer.stop();
        jaegerContainer.stop();
    }

    @Test
    void shouldSendTraceToJaeger(TestReporter reporter) throws TTransportException, InterruptedException {
        Tracer tracer = jaegerContainer.tracer();

        HTTP http = new DefaultHTTP(new DefaultHTTPClient(),
                new HTTPMessageCodecs(List.of(new StringHTTPMessageCodec(), new JsonPHTTPMessageCodec())),
                new TracingHTTPRequestInterceptor(tracer));

        URI requestURI = URI.create("http://localhost:" + mockServer.getPort() + "/success");

        HTTPResponse<String> response = http.asDSL()
                .GET(requestURI)
                .run(String.class)
                .join()
                .unsafe();

        assertEquals("hello", response.body().unsafe());

        Thread.currentThread().join(2000);

        JsonArray traces = http.asDSL()
                .GET(jaegerContainer.traces())
                .run(JsonObject.class)
                .join()
                .unsafe()
                .body()
                .unsafe()
                .getJsonArray("data");

        assertThat(traces, hasSize(1));

        JsonObject trace = traces.get(0).asJsonObject();

        reporter.publishEntry("TraceId: " + trace.getString("traceID"));

        JsonArray spans = trace.getJsonArray("spans");

        assertThat(traces, hasSize(1));

        JsonObject span = spans.get(0).asJsonObject();

        reporter.publishEntry("SpanId: " + span.getString("spanID"));

        assertEquals("GET", span.getString("operationName"));

        List<JsonObject> tags = span.getJsonArray("tags").stream().map(JsonValue::asJsonObject).collect(toList());

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.COMPONENT).and(hasStringValue.apply("julian-http-client"))),
                "a tag 'component' with a value 'julian-http-client'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.SPAN_KIND).and(hasStringValue.apply("client"))),
                "a 'span.kind' tag with a value 'client'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.HTTP_URL).and(hasStringValue.apply(requestURI.toString()))),
                "a 'http.url' tag with a value '" + requestURI + "'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.HTTP_METHOD).and(hasStringValue.apply(HTTPMethod.GET.name()))),
                "a 'http.method' tag with a value 'GET'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.HTTP_STATUS).and(hasIntValue.apply(HTTPStatusCode.OK.value()))),
                "a 'http.status_code' tag with a value '200'");
    }

    @Test
    void shouldSendTraceToJaegerWithHTTPErrorStatus(TestReporter reporter) throws TTransportException, InterruptedException {
        Tracer tracer = jaegerContainer.tracer();

        HTTP http = new DefaultHTTP(new DefaultHTTPClient(),
                new HTTPMessageCodecs(List.of(new StringHTTPMessageCodec(), new JsonPHTTPMessageCodec())),
                new TracingHTTPRequestInterceptor(tracer));

        URI requestURI = URI.create("http://localhost:" + mockServer.getPort() + "/error");

        HTTPResponse<Void> response = http.asDSL()
                .GET(requestURI)
                .run()
                .join()
                .unsafe();

        assertTrue(response.status().is(HTTPStatusCode.INTERNAL_SERVER_ERROR));

        Thread.currentThread().join(2000);

        JsonArray traces = http.asDSL()
                .GET(jaegerContainer.traces())
                .run(JsonObject.class)
                .join()
                .unsafe()
                .body()
                .unsafe()
                .getJsonArray("data");

        assertThat(traces, hasSize(1));

        JsonObject trace = traces.get(0).asJsonObject();

        reporter.publishEntry("TraceId: " + trace.getString("traceID"));

        JsonArray spans = trace.getJsonArray("spans");

        assertThat(traces, hasSize(1));

        JsonObject span = spans.get(0).asJsonObject();

        reporter.publishEntry("SpanId: " + span.getString("spanID"));

        assertEquals("GET", span.getString("operationName"));

        List<JsonObject> tags = span.getJsonArray("tags").stream().map(JsonValue::asJsonObject).collect(toList());

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.COMPONENT).and(hasStringValue.apply("julian-http-client"))),
                "a tag 'component' with a value 'julian-http-client'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.SPAN_KIND).and(hasStringValue.apply("client"))),
                "a 'span.kind' tag with a value 'client'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.HTTP_URL).and(hasStringValue.apply(requestURI.toString()))),
                "a 'http.url' tag with a value '" + requestURI + "'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.HTTP_METHOD).and(hasStringValue.apply(HTTPMethod.GET.name()))),
                "a 'http.method' tag with a value 'GET'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.HTTP_STATUS).and(hasIntValue.apply(HTTPStatusCode.INTERNAL_SERVER_ERROR.value()))),
                "a 'http.status_code' tag with a value '500'");
    }

    @Test
    void shouldSendTraceToJaegerInCaseOfException(TestReporter reporter) throws TTransportException, InterruptedException {
        Tracer tracer = jaegerContainer.tracer();

        HTTP http = new DefaultHTTP(new DefaultHTTPClient(),
                new HTTPMessageCodecs(List.of(new StringHTTPMessageCodec(), new JsonPHTTPMessageCodec())),
                new TracingHTTPRequestInterceptor(tracer));

        URI requestURI = URI.create("http://localhost:" + (mockServer.getPort() + 1) + "/i-dont-know");

        Promise<HTTPResponse<Void>> response = http.asDSL()
                .GET(requestURI)
                .run();

        response.onSuccess(r -> fail("an error was expected...instead, we have a response: " + r));

        Thread.currentThread().join(2000);

        JsonArray traces = http.asDSL()
                .GET(jaegerContainer.traces())
                .run(JsonObject.class)
                .join()
                .unsafe()
                .body()
                .unsafe()
                .getJsonArray("data");

        assertThat(traces, hasSize(1));

        JsonObject trace = traces.get(0).asJsonObject();

        reporter.publishEntry("TraceId: " + trace.getString("traceID"));

        JsonArray spans = trace.getJsonArray("spans");

        assertThat(traces, hasSize(1));

        JsonObject span = spans.get(0).asJsonObject();

        reporter.publishEntry("SpanId: " + span.getString("spanID"));

        assertEquals("GET", span.getString("operationName"));

        List<JsonObject> tags = span.getJsonArray("tags").stream().map(JsonValue::asJsonObject).collect(toList());

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.COMPONENT).and(hasStringValue.apply("julian-http-client"))),
                "a tag 'component' with a value 'julian-http-client'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.SPAN_KIND).and(hasStringValue.apply("client"))),
                "a 'span.kind' tag with a value 'client'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.HTTP_URL).and(hasStringValue.apply(requestURI.toString()))),
                "a 'http.url' tag with a value '" + requestURI + "'");

        assertTrue(tags.stream().anyMatch(hasTagWithName.apply(Tags.HTTP_METHOD).and(hasStringValue.apply(HTTPMethod.GET.name()))),
                "a 'http.method' tag with a value 'GET'");

        JsonArray logs = span.getJsonArray("logs");

        assertThat(logs, hasSize(1));

        List<JsonObject> errorFields = logs.get(0).asJsonObject().getJsonArray("fields").stream().map(JsonValue::asJsonObject).collect(toList());

        assertTrue(errorFields.stream().anyMatch(hasKey.apply("event").and(hasStringValue.apply(Tags.ERROR.getKey()))),
                "a 'event' log field with a value 'error'");

        assertTrue(errorFields.stream().anyMatch(hasKey.apply("error.object").and(hasStringValueMatching.apply(startsWith(HTTPClientException.class.getCanonicalName())))),
                "a 'error' log field with a value containing the exception name");
    }

    private static class JaegerAllInOneContainer extends GenericContainer<JaegerAllInOneContainer> {

        private static final int JAEGER_QUERY_PORT = 16686;
        private static final int JAEGER_COLLECTOR_THRIFT_PORT = 14268;
        private static final int JAEGER_ADMIN_PORT = 14269;
        private static final int ZIPKIN_PORT = 9411;

        JaegerAllInOneContainer() {
            super("jaegertracing/all-in-one:latest");
        }

        JaegerTracer tracer() throws TTransportException {
            String endpoint = "http://localhost:" + collectorThriftPort() + "/api/traces";

            Sender sender = new HttpSender.Builder(endpoint).build();
            Reporter reporter = new RemoteReporter.Builder()
                    .withSender(sender)
                    .withFlushInterval(500)
                    .build();

            JaegerTracer.Builder tracerBuilder = new JaegerTracer.Builder("julian-http-client")
                    .withSampler(new ConstSampler(true))
                    .withReporter(reporter);

            return tracerBuilder.build();
        }

        int collectorThriftPort() {
            return getMappedPort(JAEGER_COLLECTOR_THRIFT_PORT);
        }

        JaegerAllInOneContainer init() {
            waitingFor(new BoundPortHttpWaitStrategy(JAEGER_ADMIN_PORT));

            withEnv("COLLECTOR_ZIPKIN_HOST_PORT", String.valueOf(ZIPKIN_PORT));

            withExposedPorts(
                    JAEGER_ADMIN_PORT,
                    JAEGER_COLLECTOR_THRIFT_PORT,
                    JAEGER_QUERY_PORT,
                    ZIPKIN_PORT
            );

            return this;
        }

        URI traces() {
            String parameters = new QueryParameters()
                    .join("service", "julian-http-client")
                    .serialize();

            return URI.create("http://localhost:" + queryPort() + "/api/traces?" + parameters);
        }

        int queryPort() {
            return getMappedPort(JAEGER_QUERY_PORT);
        }

        private static class BoundPortHttpWaitStrategy extends HttpWaitStrategy {

            private final int port;

            public BoundPortHttpWaitStrategy(int port) {
                this.port = port;
            }

            @Override
            protected Set<Integer> getLivenessCheckPorts() {
                return Collections.singleton(waitStrategyTarget.getMappedPort(port));
            }
        }
    }
}
