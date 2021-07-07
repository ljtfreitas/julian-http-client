package com.github.ljtfreitas.julian;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import com.github.ljtfreitas.julian.Except.ThrowableConsumer;
import com.github.ljtfreitas.julian.contract.Callback;
import com.github.ljtfreitas.julian.contract.Get;
import com.github.ljtfreitas.julian.contract.Head;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
class ProxyBuilderTest {

    private final MockServerClient mockServer;

    ProxyBuilderTest(MockServerClient mockServer) {
        this.mockServer = mockServer;
    }

    @Nested
    @DisplayName("Should build a proxy using default configurations.")
    class Default {

        @Nested
        @DisplayName("Should be able to read a response using default codec (wildcard) and default supported types.")
        @MockServerSettings(ports = 8090)
        class Codecs {

            private SimpleApi simpleApi = new ProxyBuilder().build(SimpleApi.class, "http://localhost:8090");

            @BeforeEach
            void before() {
                mockServer.when(request("/simple").withMethod("GET")).respond(response("hello"));
            }

            @Test
            void asString() {
                assertEquals("hello", simpleApi.asString());
            }

            @Test
            void asBytes() {
                assertEquals("hello", new String(simpleApi.asBytes()));
            }

            @Test
            void asInputStream() {
                String actual = Bracket.acquire(() -> new BufferedReader(new InputStreamReader(simpleApi.asInputStream())))
                        .map(BufferedReader::readLine)
                        .unsafe();

                assertEquals("hello", actual);
            }

            @Test
            void scalar() {
                mockServer.when(request("/scalar").withMethod("GET")).respond(response("1"));

                assertEquals(1, simpleApi.scalar());
            }
        }

        @Nested
        @DisplayName("Should be able to read a response using default response types.")
        @MockServerSettings(ports = 8090)
        class Responses {

            private ResponsesApi responsesApi = new ProxyBuilder().build(ResponsesApi.class, "http://localhost:8090");

            @ParameterizedTest
            @ArgumentsSource(DefaultResponseTypesProvider.class)
            @DisplayName("Should run a request, and get the response as the desired object type.")
            void shouldRunTheRequestAndGetTheResponseAsTheDesiredObjectType(HttpRequest request, HttpResponse response, ThrowableConsumer<ResponsesApi> consumer) throws Throwable {
                mockServer.when(request).respond(response);

                consumer.accept(responsesApi);

                mockServer.verify(request);
            }
        }
    }

    static class DefaultResponseTypesProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(arguments(request("/callable").withMethod("GET"), response("hello"), run(api -> assertEquals("hello", api.callable().call()))),
                             arguments(request("/runnable").withMethod("GET"), response("hello"), run(api -> api.runnable().run())),
                             arguments(request("/callback").withMethod("GET"), response("hello"), run(api -> {
                                 api.callback(r -> assertEquals("hello", r));
                                 Thread.sleep(2000);
                             })),
                             arguments(request("/completable-future").withMethod("GET"), response("hello"), run(api -> assertEquals("hello", api.completableFuture().join()))),
                             arguments(request("/future").withMethod("GET"), response("hello"), run(api -> assertEquals("hello", api.future().get()))),
                             arguments(request("/future-task").withMethod("GET"), response("hello"), run(api -> {
                                 FutureTask<String> task = api.futureTask();
                                 Executors.newSingleThreadExecutor().submit(task);
                                 assertEquals("hello", task.get());
                             })),
                             arguments(request("/optional").withMethod("GET"), response("hello"), run(api -> assertEquals("hello", api.optional().get()))),
                             arguments(request("/response").withMethod("GET"), response("hello"), run(api -> assertEquals("hello", api.response().body()))),
                             arguments(request("/promise").withMethod("GET"), response("hello"), run(api -> assertEquals("hello", api.promise().join().unsafe()))),
                             arguments(request("/headers").withMethod("HEAD"), response().withHeader("x-response-header", "whatever"),
                                    run(api -> assertThat(api.headers(), hasItems(new Header("x-response-header", List.of("whatever")))))),
                             arguments(request("/http-response").withMethod("GET"), response("hello"), run(api -> assertEquals("hello", api.httpResponse().body()))),
                             arguments(request("/http-headers").withMethod("HEAD"), response().withHeader("x-response-header", "whatever"),
                                     run(api -> assertThat(api.httpHeaders(), hasItems(new HTTPHeader("x-response-header", List.of("whatever")))))),
                             arguments(request("/http-status").withMethod("HEAD"), response().withStatusCode(200), run(api -> assertEquals(HTTPStatusCode.OK.value(), api.httpStatus().code()))),
                             arguments(request("/http-status-code").withMethod("HEAD"), response().withStatusCode(200), run(api -> assertEquals(HTTPStatusCode.OK, api.httpStatusCode()))));
        }

        ThrowableConsumer<ResponsesApi> run(ThrowableConsumer<ResponsesApi> consumer) {
            return consumer;
        }
    }

    interface SimpleApi {

        @Get("/simple")
        String asString();

        @Get("/simple")
        byte[] asBytes();

        @Get("/simple")
        InputStream asInputStream();

        @Get("/scalar")
        int scalar();
    }

    interface ResponsesApi {

        @Get("/callable")
        Callable<String> callable();

        @Get("/callback")
        void callback(@Callback Consumer<String> callback);

        @Get("/completable-future")
        CompletableFuture<String> completableFuture();

        @Get("/response")
        Response<String> response();

        @Get("/http-response")
        HTTPResponse<String> httpResponse();

        @Get("/future")
        Future<String> future();

        @Get("/future-task")
        FutureTask<String> futureTask();

        @Head("/headers")
        Headers headers();

        @Head("/http-headers")
        HTTPHeaders httpHeaders();

        @Get("/optional")
        Optional<String> optional();

        @Get("/promise")
        Promise<String> promise();

        @Get("/runnable")
        Runnable runnable();

        @Head("/http-status")
        HTTPStatus httpStatus();

        @Head("/http-status-code")
        HTTPStatusCode httpStatusCode();
    }
}