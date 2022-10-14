package com.github.ljtfreitas.julian.spring.autoconfigure;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPMethod;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptor;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.JsonHTTPMessageCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.APPLICATION_JSON;
import static org.mockserver.verify.VerificationTimes.never;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = 8090)
class JulianAutoConfigurationTest {

    private final MockServerClient mockServer;

    private final HttpRequest httpRequest = request("/sample");

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JulianAutoConfiguration.class));

    JulianAutoConfigurationTest(MockServerClient mockServer) {
        this.mockServer = mockServer;
    }

    @BeforeEach
    void setup() {
        mockServer.when(httpRequest)
                .respond(response().withBody("it works!").withStatusCode(200));

        contextRunner = contextRunner.withUserConfiguration(Main.class)
                .withPropertyValues("sample.api2.url:http://localhost:8090",
                                    "julian-http-client.sample-api-3.base-url:http://localhost:8090",
                                    "julian-http-client.julianAutoConfigurationTest.SampleApi4.base-url:http://localhost:8090");
    }

    @Test
    void sample() {
        contextRunner.run(context -> {
            SampleApi sampleApi = context.getBean(SampleApi.class);

            assertNotNull(sampleApi);

            assertEquals("it works!", sampleApi.sample());

            mockServer.verify(httpRequest);
        });
    }

    @Test
    @DisplayName("We can set the api's base url with Spring properties")
    void properties() {
        contextRunner.run(context -> {
            SampleApi2 sampleApi2 = context.getBean(SampleApi2.class);

            assertNotNull(sampleApi2);

            assertEquals("it works!", sampleApi2.sample());

            mockServer.verify(httpRequest);
        });
    }

    @Test
    @DisplayName("We can set the api's base url using Spring properties, following the convention: julian-http-client.[name].base-url")
    void annotationName() {
        contextRunner.run(context -> {
            SampleApi3 sampleApi3 = context.getBean(SampleApi3.class);

            assertNotNull(sampleApi3);

            assertEquals("it works!", sampleApi3.sample());

            mockServer.verify(httpRequest);
        });
    }

    @Test
    @DisplayName("We can set the api's base url using Spring properties, following the convention: julian-http-client.[beanName].base-url")
    void beanName() {
        contextRunner.run(context -> {
            SampleApi4 sampleApi4 = context.getBean(SampleApi4.class);

            assertNotNull(sampleApi4);

            assertEquals("it works!", sampleApi4.sample());

            mockServer.verify(httpRequest);
        });
    }

    @Test
    @DisplayName("We can customize all http client details using a JulianHTTPClientSpecification")
    void spec() {
        String jsonResponseBody = "{\"message\":\"it works!\"}";

        HttpRequest jsonRequest = request("/json")
                .withHeader("x-sample", "test");

        mockServer.when(jsonRequest)
                .respond(response().withBody(jsonResponseBody)
                        .withContentType(APPLICATION_JSON)
                        .withStatusCode(200));

        contextRunner.run(context -> {
            SampleApi5 sampleApi5 = context.getBean(SampleApi5.class);

            assertNotNull(sampleApi5);

            assertEquals(jsonResponseBody, sampleApi5.json().value);

            mockServer.verify(jsonRequest);
        });
    }

    @Test
    void beans() {
        mockServer.clear(httpRequest);

        contextRunner.withUserConfiguration(Beans.class)
                .run(context -> {
                    HTTP http = context.getBean(HTTP.class);

                    SampleApi sampleApi = context.getBean(SampleApi.class);

                    assertNotNull(sampleApi);

                    assertEquals("it works!", sampleApi.sample());

                    mockServer.verify(httpRequest, never());

                    verify(http).run(any());
                });
    }

    @Test
    @DisplayName("We can enable debug extension using Spring properties, following the convention: julian-http-client.[name].debug-enabled = true")
    void debug() {
        contextRunner.withPropertyValues("julian-http-client.julianAutoConfigurationTest.SampleApi.debug-enabled:true")
                .run(context -> {
                    SampleApi sampleApi = context.getBean(SampleApi.class);

                    assertNotNull(sampleApi);

                    assertEquals("it works!", sampleApi.sample());
                });
    }

    @EnableAutoConfiguration
    static class Main {}

    @Configuration
    static class Beans {

        @Bean
        HTTP mockHTTP() {
            HTTP http = mock(HTTP.class);
            when(http.run(any()))
                    .thenReturn(Promise.done(HTTPResponse.success(HTTPStatus.valueOf(HTTPStatusCode.OK), HTTPHeaders.empty(), "it works!")));
            return http;
        }
    }

    @JulianHTTPClient(baseURL = "http://localhost:8090")
    interface SampleApi {

        @GET("/sample")
        String sample();
    }

    @JulianHTTPClient(baseURL = "${sample.api2.url}")
    interface SampleApi2 {

        @GET("/sample")
        String sample();
    }

    @JulianHTTPClient(name = "sample-api-3")
    interface SampleApi3 {

        @GET("/sample")
        String sample();
    }

    @JulianHTTPClient
    interface SampleApi4 {

        @GET("/sample")
        String sample();
    }

    @JulianHTTPClient(spec = SampleApi5.SampleApi5Spec.class)
    interface SampleApi5 {

        @GET("/json")
        JsonContent json();

        class SampleApi5Spec implements JulianHTTPClientSpecification {

            @Override
            public URI endpoint() {
                return URI.create("http://localhost:8090");
            }

            @Override
            public Collection<HTTPMessageCodec> codecs() {
                return List.of(new SimpleJsonContentHTTPCodec());
            }

            @Override
            public Collection<HTTPRequestInterceptor> interceptors() {
                return List.of(new SimpleHTTPRequestInterceptor());
            }
        }
    }

    static class SimpleHTTPRequestInterceptor implements HTTPRequestInterceptor {

        @Override
        public <T> Promise<HTTPRequest<T>> intercepts(Promise<HTTPRequest<T>> request) {
            return request.then(SimpleHTTPRequest::new);
        }
    }

    static class SimpleHTTPRequest<T> implements HTTPRequest<T> {

        private final HTTPRequest<T> source;

        SimpleHTTPRequest(HTTPRequest<T> source) {
            this.source = source;
        }

        @Override
        public JavaType returnType() {
            return source.returnType();
        }

        @Override
        public HTTPRequest<T> path(URI path) {
            return source.path(path);
        }

        @Override
        public HTTPRequest<T> method(HTTPMethod method) {
            return source.method(method);
        }

        @Override
        public HTTPRequest<T> headers(HTTPHeaders headers) {
            return source.headers(headers);
        }

        @Override
        public HTTPRequest<T> body(HTTPRequestBody body) {
            return source.body(body);
        }

        @Override
        public URI path() {
            return source.path();
        }

        @Override
        public HTTPMethod method() {
            return source.method();
        }

        @Override
        public HTTPHeaders headers() {
            return source.headers();
        }

        @Override
        public Optional<HTTPRequestBody> body() {
            return source.body();
        }

        @Override
        public Promise<HTTPResponse<T>> execute() {
            return source.headers(source.headers().join(new HTTPHeader("x-sample", "test")))
                    .execute();
        }
    }

    static class SimpleJsonContentHTTPCodec implements JsonHTTPMessageCodec<JsonContent> {

        @Override
        public boolean writable(MediaType candidate, JavaType javaType) {
            return false;
        }

        @Override
        public HTTPRequestBody write(JsonContent body, Charset encoding) {
            throw new UnsupportedOperationException("unsupported...");
        }

        @Override
        public boolean readable(MediaType candidate, JavaType javaType) {
            return supports(candidate) && javaType.is(JsonContent.class);
        }

        @Override
        public Optional<CompletableFuture<JsonContent>> read(HTTPResponseBody body, JavaType javaType) {
            return body.readAsBytes(bodyAsBytes -> new JsonContent(new String(bodyAsBytes)));
        }
    }

    static class JsonContent {

        final String value;

        JsonContent(String value) {
            this.value = value;
        }
    }

}