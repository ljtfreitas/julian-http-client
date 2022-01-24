package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Kind;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.client.DefaultHTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;

import java.nio.charset.StandardCharsets;

import static com.github.ljtfreitas.julian.http.HTTPRequest.DELETE;
import static com.github.ljtfreitas.julian.http.HTTPRequest.GET;
import static com.github.ljtfreitas.julian.http.HTTPRequest.HEAD;
import static com.github.ljtfreitas.julian.http.HTTPRequest.OPTIONS;
import static com.github.ljtfreitas.julian.http.HTTPRequest.PATCH;
import static com.github.ljtfreitas.julian.http.HTTPRequest.POST;
import static com.github.ljtfreitas.julian.http.HTTPRequest.PUT;
import static com.github.ljtfreitas.julian.http.HTTPRequest.TRACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.TEXT_PLAIN;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = 8091)
class HTTPRequestTest {

    private final HTTPClient client = new DefaultHTTPClient();

    private final MockServerClient mockServer;

    HTTPRequestTest(MockServerClient mockServer) {
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
            Promise<String, HTTPException> promise = GET("http://localhost:8092/hello")
                    .response(String.class, StringHTTPMessageCodec.get())
                    .build(client)
                        .execute()
                        .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());
        }

        @DisplayName("Should run a GET request, using a Kind<String> as response.")
        @Test
        void shouldRunGETUsingKind() {
            Promise<String, HTTPException> promise = GET("http://localhost:8092/hello")
                    .response(new Kind<>() {}, StringHTTPMessageCodec.get())
                    .build(client)
                        .execute()
                        .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());
        }

        @DisplayName("Should build a GET request, using a JavaType<String> as response.")
        @Test
        void shouldRunGETUsingJavaType() {
            Promise<String, HTTPException> promise = GET("http://localhost:8092/hello")
                    .response(JavaType.valueOf(String.class), StringHTTPMessageCodec.get())
                    .build(client)
                        .execute()
                        .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());
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

            Promise<String, HTTPException> promise = GET("http://localhost:8092/hello")
                    .param("param", "value")
                    .response(String.class, StringHTTPMessageCodec.get())
                    .build(client)
                        .execute()
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
            Promise<String, HTTPException> promise = POST("http://localhost:8093/hello")
                    .header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
                    .body("i am a body", StringHTTPMessageCodec.get())
                    .response(String.class, StringHTTPMessageCodec.get())
                    .build(client)
                        .execute()
                        .then(r -> r.body().unsafe());

            assertEquals("it works", promise.join().unsafe());
        }

        @DisplayName("Should run a POST request, using a HTTPRequestBody")
        @Test
        void shouldRunPOSTUsingHTTPRequestBody() {
            HTTPRequestBody body = StringHTTPMessageCodec.get().write("i am a body", StandardCharsets.UTF_8);

            Promise<String, HTTPException> promise = POST("http://localhost:8093/hello")
                    .body(body)
                    .response(String.class, new StringHTTPMessageCodec())
                    .build(client)
                        .execute()
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

        Promise<String, HTTPException> promise = PUT("http://localhost:8091/hello")
                .header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
                .body("i am a body", StringHTTPMessageCodec.get())
                .response(String.class, new StringHTTPMessageCodec())
                .build(client)
                    .execute()
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

        Promise<String, HTTPException> promise = PATCH("http://localhost:8091/hello")
                .header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
                .body("i am a body", StringHTTPMessageCodec.get())
                .response(String.class, new StringHTTPMessageCodec())
                .build(client)
                    .execute()
                    .then(r -> r.body().unsafe());

        assertEquals("it works", promise.join().unsafe());
    }

    @DisplayName("Should run a DELETE request.")
    @Test
    void shouldRunDELETE() {
        mockServer.when(request("/hello")
                        .withMethod("DELETE"))
                .respond(response().withStatusCode(200));

        Promise<HTTPStatus, HTTPException> promise = DELETE("http://localhost:8091/hello")
                .build(client)
                    .execute()
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

        Promise<HTTPHeaders, HTTPException> promise = HEAD("http://localhost:8091/hello")
                .build(client)
                    .execute()
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

        Promise<HTTPHeaders, HTTPException> promise = OPTIONS("http://localhost:8091/hello")
                .build(client)
                    .execute()
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

        Promise<HTTPStatus, HTTPException> promise = TRACE("http://localhost:8091/hello")
                .build(client)
                    .execute()
                    .then(HTTPResponse::status);

        HTTPStatus status = promise.join().unsafe();

        assertTrue(status.is(HTTPStatusCode.OK));

        mockServer.verify(requestDefinition);
    }
}