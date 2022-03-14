package com.github.ljtfreitas.julian.http;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.DefaultHTTPClient;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.UnprocessableHTTPMessageCodec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.TEXT_PLAIN;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = 8091)
class HTTPDSLTest {

    private final HTTP.DSL httpAsDsl = new DefaultHTTP(
            new DefaultHTTPClient(),
            new HTTPMessageCodecs(List.of(new StringHTTPMessageCodec(), new UnprocessableHTTPMessageCodec())))
            .asDSL();

    private final MockServerClient mockServer;

    HTTPDSLTest(MockServerClient mockServer) {
        this.mockServer = mockServer;
    }

    @DisplayName("Should run a GET request.")
    @Nested
    @MockServerSettings(ports = 8092)
    class GET {

        @BeforeEach
        void setup() {
            HttpRequest requestDefinition = request("/hello").withMethod("GET");

            mockServer.clear(requestDefinition)
                    .when(requestDefinition)
                    .respond(response("it works")
                        .withContentType(TEXT_PLAIN));

        }

        @DisplayName("Should run a GET request, using a Class<String> as response.")
        @Test
        void shouldRunGETUsingClass() {
            Promise<String> promise = httpAsDsl.GET(URI.create("http://localhost:8092/hello"))
                    .run(String.class)
                    .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());
        }

        @DisplayName("Should run a GET request, using a Kind<String> as response.")
        @Test
        void shouldRunGETUsingKind() {
            Promise<String> promise = httpAsDsl.GET(URI.create("http://localhost:8092/hello"))
                    .run(new Kind<String>(){})
                    .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());
        }

        @DisplayName("Should build a GET request, using a JavaType<String> as response.")
        @Test
        void shouldRunGETUsingJavaType() {
            Promise<HTTPResponse<String>> response = httpAsDsl.GET(URI.create("http://localhost:8092/hello"))
                    .run(JavaType.valueOf(String.class));

            assertEquals("it works", response.then(r -> r.body().unsafe()).join().unsafe());
        }

        @DisplayName("Should run a GET request using query parameters.")
        @Test
        void shouldRunGETUsingQueryParameters() {
            HttpRequest requestDefinition = request("/hello")
                    .withMethod("GET")
                    .withQueryStringParameter("param", "value");

            mockServer.clear(requestDefinition)
                    .when(requestDefinition)
                    .respond(response("it works")
                            .withContentType(TEXT_PLAIN));

            Promise<String> promise = httpAsDsl.GET(URI.create("http://localhost:8092/hello"))
                    .parameter("param", "value")
                    .run(String.class)
                        .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());

            mockServer.verify(requestDefinition);
        }
    }

    @DisplayName("Should run a POST request.")
    @Nested
    @MockServerSettings(ports = 8093)
    class POST {

        @BeforeEach
        void setup() {
            HttpRequest requestDefinition = request("/hello").withMethod("POST");

            mockServer.clear(requestDefinition)
                    .when(requestDefinition)
                    .respond(response("it works")
                            .withContentType(TEXT_PLAIN));
        }

        @DisplayName("Should run a POST request, using a String as body.")
        @Test
        void shouldRunPOSTUsingString() {
            Promise<String> promise = httpAsDsl.POST(URI.create("http://localhost:8093/hello"))
                    .header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
                    .body("i am a body")
                    .run(String.class)
                        .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());
        }

        @DisplayName("Should run a POST request, using a String as body and TEXT_PLAIN as content type")
        @Test
        void shouldRunPOSTUsingHTTPRequestBody() {
            Promise<String> promise = httpAsDsl.POST(URI.create("http://localhost:8093/hello"))
                    .body("i am a body", MediaType.TEXT_PLAIN)
                    .run(String.class)
                        .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());
        }
    }

    @DisplayName("Should run a PUT request.")
    @Test
    void shouldRunPUT() {
        mockServer.when(request("/hello")
                        .withMethod("PUT")
                        .withBody("i am a body")
                        .withContentType(TEXT_PLAIN))
                .respond(response("it works")
                        .withContentType(TEXT_PLAIN));

        Promise<String> promise = httpAsDsl.PUT(URI.create("http://localhost:8091/hello"))
                .header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
                .body("i am a body")
                .run(String.class)
                    .then(r -> r.body().unsafe());

        assertEquals("it works", promise.join().unsafe());
    }

    @DisplayName("Should run a PATCH request.")
    @Test
    void shouldRunPATCH() {
        mockServer.when(request("/hello")
                        .withMethod("PATCH")
                        .withBody("i am a body")
                        .withContentType(TEXT_PLAIN))
                .respond(response("it works")
                        .withContentType(TEXT_PLAIN));

        Promise<String> promise = httpAsDsl.PATCH(URI.create("http://localhost:8091/hello"))
                .header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
                .body("i am a body")
                .run(String.class)
                    .then(r -> r.body().unsafe());

        assertEquals("it works", promise.join().unsafe());
    }

    @DisplayName("Should run a DELETE request.")
    @Test
    void shouldRunDELETE() {
        mockServer.when(request("/hello")
                        .withMethod("DELETE"))
                .respond(response().withStatusCode(200));

        Promise<HTTPStatus> promise = httpAsDsl.DELETE(URI.create("http://localhost:8091/hello"))
                .run()
                    .then(HTTPResponse::status);

        HTTPStatus status = promise.join().unsafe();

        assertTrue(status.is(HTTPStatusCode.OK));
    }

    @DisplayName("Should run a HEAD request.")
    @Test
    void shouldRunHEAD() {
        HttpRequest requestDefinition = request("/hello").withMethod("HEAD");

        mockServer.clear(requestDefinition)
                .when(requestDefinition)
                .respond(response().withHeader("x-my-header", "whatever"));

        Promise<HTTPHeaders> promise = httpAsDsl.HEAD(URI.create("http://localhost:8091/hello"))
                .run()
                    .then(HTTPResponse::headers);

        HTTPHeaders headers = promise.join().unsafe();

        assertThat(headers, hasItems(new HTTPHeader("x-my-header", "whatever")));

        mockServer.verify(requestDefinition);
    }

    @DisplayName("Should run a OPTIONS request.")
    @Test
    void shouldRunOPTIONS() {
        HttpRequest requestDefinition = request("/hello").withMethod("OPTIONS");

        mockServer.clear(requestDefinition)
                .when(requestDefinition)
                .respond(response().withHeader("Allow", "GET, POST, PUT, DELETE"));

        Promise<HTTPHeaders> promise = httpAsDsl.OPTIONS(URI.create("http://localhost:8091/hello"))
                .run()
                    .then(HTTPResponse::headers);

        HTTPHeaders headers = promise.join().unsafe();

        assertThat(headers, hasItems(new HTTPHeader(HTTPHeader.ALLOW, "GET, POST, PUT, DELETE")));

        mockServer.verify(requestDefinition);
    }

    @DisplayName("Should run a TRACE request.")
    @Test
    void shouldRunTRACE() {
        HttpRequest requestDefinition = request("/hello").withMethod("TRACE");

        mockServer.clear(requestDefinition)
                .when(requestDefinition)
                .respond(response().withStatusCode(200));

        Promise<HTTPStatus> promise = httpAsDsl.TRACE(URI.create("http://localhost:8091/hello"))
                .run()
                    .then(HTTPResponse::status);

        HTTPStatus status = promise.join().unsafe();

        assertTrue(status.is(HTTPStatusCode.OK));

        mockServer.verify(requestDefinition);
    }
}