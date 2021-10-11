package com.github.ljtfreitas.julian.http.client.reactor;

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPMethod;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestDefinition;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;
import reactor.core.Exceptions;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec.TEXT_PLAIN_MEDIA_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.TEXT_PLAIN;

@ExtendWith(MockServerExtension.class)
class ReactorNettyHTTPClientTest {

    private ReactorNettyHTTPClient client = new ReactorNettyHTTPClient();

    private final MockServerClient mockServer;

    ReactorNettyHTTPClientTest(MockServerClient mockServer) {
        this.mockServer = mockServer;
    }

    @Nested
    @MockServerSettings(ports = 8090)
    class HTTPMethods {

        @ParameterizedTest(name = "HTTP Request: {0} and HTTP Response: {1}")
        @ArgumentsSource(HTTPMethodProvider.class)
        void shouldRunRequestAndReadTheResponse(HttpRequest expectedRequest, HttpResponse expectedResponse, HTTPRequestDefinition request) {
            mockServer.when(expectedRequest).respond(expectedResponse);

            HTTPClientResponse response = client.request(request).execute().join().unsafe();

            assertAll(() -> assertEquals(expectedResponse.getStatusCode(), response.status().code()),
                      () -> assertEquals(expectedResponse.getBodyAsString(), response.body().deserialize(String::new).orElse(null)));
        }
    }

    @Nested
    @MockServerSettings(ports = 8090)
    class WithHeaders {

        @ParameterizedTest(name = "HTTP Request: {0} and HTTP Response: {1}")
        @ArgumentsSource(HTTPHeadersProvider.class)
        void shouldRunRequestAndReadTheResponse(HttpRequest expectedRequest, HttpResponse expectedResponse, HTTPRequestDefinition request) {
            mockServer.when(expectedRequest).respond(expectedResponse);

            HTTPClientResponse response = client.request(request).execute().join().unsafe();

            HTTPHeader[] expectedHeaders = expectedResponse.getHeaderList().stream()
                    .map(h -> HTTPHeader.create(h.getName().getValue(), h.getValues().stream()
                            .map(NottableString::getValue)
                            .collect(Collectors.toUnmodifiableList())))
                    .toArray(HTTPHeader[]::new);

            assertAll(() -> assertEquals(expectedResponse.getStatusCode(), response.status().code()),
                      () -> assertThat(response.headers(), hasItems(expectedHeaders)));
        }
    }

    @Nested
    class WithBody {

        @Nested
        class Request {

            @Nested
            @MockServerSettings(ports = 8090)
            class Success {

                @Test
                void shouldSerializeTheContentToHTTPRequestBody() {
                    String requestBodyAsString = "{\"message\":\"hello\"}";
                    String expectedResponse = "it works!";

                    mockServer.when(request("/hello")
                            .withMethod("POST")
                            .withBody(requestBodyAsString))
                            .respond(response(expectedResponse)
                                    .withContentType(TEXT_PLAIN));

                    HTTPRequestDefinition request = new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "POST",
                            HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
                            new DefaultHTTPRequestBody(TEXT_PLAIN_MEDIA_TYPE, () -> BodyPublishers.ofString(requestBodyAsString)),
                            JavaType.valueOf(String.class));

                    HTTPClientResponse response = client.request(request).execute().join().unsafe();

                    assertEquals(expectedResponse, response.body().deserialize(String::new).orElse(""));
                }
            }
        }

        @Nested
        class Response {

            @Nested
            @MockServerSettings(ports = 8090)
            class Success {

                @Test
                void shouldSerializeTheHTTPResponseBody() {
                    String expectedResponse = "response";

                    HttpRequest requestSpec = request("/hello").withMethod("GET");

                    mockServer.clear(requestSpec)
                            .when(requestSpec)
                            .respond(response(expectedResponse)
                                    .withContentType(TEXT_PLAIN));

                    HTTPRequestDefinition request = new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "GET");

                    HTTPClientResponse response = client.request(request).execute().join().unsafe();

                    assertEquals(expectedResponse, response.body().deserialize(String::new).orElse(""));
                }
            }
        }
    }

    @Nested
    class Failures {

        @Nested
        @MockServerSettings(ports = 8090)
        class HTTPStatusCodes {

            @ParameterizedTest(name = "{0} {1}")
            @ArgumentsSource(HTTPFailureStatusCodesProvider.class)
            void failures(int statusCode, String reason) {
                mockServer.when(request("/hello/" + statusCode)
                        .withMethod("GET"))
                        .respond(response().withStatusCode(statusCode)
                                .withReasonPhrase(reason)
                                .withHeader("X-Whatever", "whatever"));

                HTTPRequestDefinition request = new HTTPRequestDefinition() {

                    @Override
                    public URI path() {
                        return URI.create("http://localhost:8090/hello/" + statusCode);
                    }

                    @Override
                    public HTTPMethod method() {
                        return HTTPMethod.GET;
                    }

                    @Override
                    public HTTPHeaders headers() {
                        return HTTPHeaders.empty();
                    }

                    @Override
                    public Optional<HTTPRequestBody> body() {
                        return Optional.empty();
                    }

                    @Override
                    public JavaType returnType() {
                        return JavaType.none();
                    }
                };

                HTTPClientResponse response = client.request(request).execute().join().unsafe();

                assertAll(() -> assertEquals(statusCode, response.status().code()),
                          () -> assertThat(response.status().message(), anyOf(equalTo(reason), nullValue())),
                          () -> assertThat(response.headers(), hasItems(new HTTPHeader("X-Whatever", List.of("whatever")))));
            }
        }

        @Nested
        class ConnectionFailures {

            @Test
            void unknownHost() {
                HTTPRequestDefinition request = new HTTPRequestDefinition() {
                    @Override
                    public URI path() {
                        return URI.create("http://localhost:8091/hello");
                    }

                    @Override
                    public HTTPMethod method() {
                        return HTTPMethod.GET;
                    }

                    @Override
                    public HTTPHeaders headers() {
                        return HTTPHeaders.empty();
                    }

                    @Override
                    public Optional<HTTPRequestBody> body() {
                        return Optional.empty();
                    }

                    @Override
                    public JavaType returnType() {
                        return JavaType.none();
                    }
                };

                Except<HTTPClientResponse> response = client.request(request).execute().join();

                response.consumes(r -> fail("a connection error was expected..."))
                        .failure(e -> assertThat(e.getCause(), instanceOf(ConnectException.class)));
            }
        }
    }

    static class HTTPMethodProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {

            HttpResponse expectedResponse = response("it works!").withContentType(TEXT_PLAIN);

            String requestBodyAsString = "{\"message\":\"hello\"}";

            return Stream.of(arguments(request("/hello").withMethod("GET"),
                                expectedResponse, new SimpleHTTPRequestDefinition("http://localhost:8090/hello")),
                             arguments(request("/hello").withMethod("POST").withBody(requestBodyAsString),
                                expectedResponse, new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "POST",
                                             HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
                                             new DefaultHTTPRequestBody(TEXT_PLAIN_MEDIA_TYPE, () -> BodyPublishers.ofString(requestBodyAsString)),
                                             JavaType.valueOf(String.class))),
                             arguments(request("/hello").withMethod("PUT").withBody(requestBodyAsString),
                                expectedResponse, new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "POST",
                                             HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
                                             new DefaultHTTPRequestBody(TEXT_PLAIN_MEDIA_TYPE, () -> BodyPublishers.ofString(requestBodyAsString)),
                                             JavaType.valueOf(String.class))),
                             arguments(request("/hello").withMethod("PATCH").withBody(requestBodyAsString),
                                expectedResponse, new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "PATCH",
                                             HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
                                             new DefaultHTTPRequestBody(TEXT_PLAIN_MEDIA_TYPE, () -> BodyPublishers.ofString(requestBodyAsString)),
                                             JavaType.valueOf(String.class))),
                             arguments(request("/hello").withMethod("DELETE"),
                                expectedResponse, new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "DELETE")),
                             arguments(request("/hello").withMethod("TRACE"),
                                response().withStatusCode(200),
                                new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "TRACE")),
                             arguments(request("/hello").withMethod("HEAD"),
                                response().withStatusCode(200),
                                new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "HEAD")));
        }
    }

    static class HTTPHeadersProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
            HttpResponse expectedResponse = response().withStatusCode(HTTPStatusCode.OK.value())
                    .withHeader("X-Response-Header-1", "response-header-value-1")
                    .withHeader("X-Response-Header-2", "response-header-value-2")
                    .withHeader("X-Response-Header-3", "value1", "value2");

            return Stream.of(arguments(request("/hello").withMethod("GET")
                                            .withHeader("X-Whatever-1", "whatever-header-value-1")
                                            .withHeader("X-Whatever-2", "whatever-header-value-2")
                                            .withHeader("X-Whatever-3", "value1", "value2"),
                                       expectedResponse, new SimpleHTTPRequestDefinition("http://localhost:8090/hello", "GET",
                                            HTTPHeaders.create(new HTTPHeader("X-Whatever-1", "whatever-header-value-1"),
                                                               new HTTPHeader("X-Whatever-2", "whatever-header-value-2"),
                                                               new HTTPHeader("X-Whatever-3", List.of("value1", "value2"))))));
        }
    }

    static class HTTPFailureStatusCodesProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
            return IntStream.range(400, 600)
                    .mapToObj(code -> org.junit.jupiter.params.provider.Arguments.of(code, HTTPStatusCode.select(code).map(HTTPStatusCode::message).orElse("unknown")));
        }
    }

    private static class SimpleHTTPRequestDefinition implements HTTPRequestDefinition {

        private final URI path;
        private final HTTPMethod method;
        private final HTTPHeaders headers;
        private final HTTPRequestBody body;
        private final JavaType returnType;

        SimpleHTTPRequestDefinition(String path) {
            this(path, "GET");
        }

        SimpleHTTPRequestDefinition(String path, String httpMethod) {
            this(path, httpMethod, HTTPHeaders.empty());
        }

        SimpleHTTPRequestDefinition(String path, String httpMethod, HTTPHeaders headers) {
            this(path, httpMethod, headers, null, JavaType.none());
        }

        SimpleHTTPRequestDefinition(String path, String httpMethod, HTTPHeaders headers, HTTPRequestBody body, JavaType returnType) {
            this.path = URI.create(path);
            this.method = HTTPMethod.valueOf(httpMethod);
            this.headers = headers;
            this.body = body;
            this.returnType = returnType;
        }

        @Override
        public URI path() {
            return path;
        }

        @Override
        public HTTPMethod method() {
            return method;
        }

        @Override
        public HTTPHeaders headers() {
            return headers;
        }

        @Override
        public Optional<HTTPRequestBody> body() {
            return Optional.ofNullable(body);
        }

        @Override
        public JavaType returnType() {
            return returnType;
        }
    }
}