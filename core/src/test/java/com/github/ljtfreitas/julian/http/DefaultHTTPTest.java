package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Cookie;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestDefinition;
import com.github.ljtfreitas.julian.Subscriber;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.BadRequest;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.Conflict;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.ExpectationFailed;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.Forbidden;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.Gone;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.LengthRequired;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.MethodNotAllowed;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.NotAcceptable;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.NotFound;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.PreconditionFailed;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.ProxyAuthenticationRequired;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.RequestEntityTooLarge;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.RequestTimeout;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.RequestURITooLong;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.RequestedRangeNotSatisfiable;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.Unauthorized;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.UnsupportedMediaType;
import com.github.ljtfreitas.julian.http.HTTPResponse.HTTPResponseSubscriber;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.BadGateway;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.GatewayTimeout;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.HTTPVersionNotSupported;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.InternalServerError;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.NotImplemented;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.ServiceUnavailable;
import com.github.ljtfreitas.julian.http.client.DefaultHTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClientException;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodecs;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.UnprocessableHTTPMessageCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.APPLICATION_JSON;
import static org.mockserver.model.MediaType.TEXT_PLAIN;

@ExtendWith(MockServerExtension.class)
class DefaultHTTPTest {

	private HTTP http;

	private final MockServerClient mockServer;

	DefaultHTTPTest(MockServerClient mockServer) {
		this.mockServer = mockServer;
	}

	@BeforeEach
	void setup() {
		http = new DefaultHTTP(new DefaultHTTPClient(),
							   new HTTPRequestInterceptorChain(),
							   new HTTPMessageCodecs(List.of(new StringHTTPMessageCodec(MediaType.valueOf("text/*")), new UnprocessableHTTPMessageCodec())),
							   new DefaultHTTPResponseFailure());
	}

	@Nested
	@MockServerSettings(ports = 8090)
	class HTTPMethods {

		@ParameterizedTest(name = "HTTP Request: {0} and HTTP Response: {1}")
		@ArgumentsSource(HTTPMethodProvider.class)
		void shouldRunRequestAndReadTheResponse(HttpRequest expectedRequest, HttpResponse expectedResponse, RequestDefinition definition) {

			mockServer.when(expectedRequest).respond(expectedResponse);

			Promise<HTTPResponse<String>> promise = http.run(definition);

			HTTPResponse<String> response = promise.join().unsafe();

			assertAll(() -> assertEquals(expectedResponse.getStatusCode(), response.status().code()),
					  () -> assertEquals(expectedResponse.getBodyAsString(), response.body().unsafe()));
		}

		@Test
		void shouldThrowExceptionToUnsupportedHTTPMethod() {
			assertThrows(IllegalArgumentException.class, () -> http.run(new RequestDefinition(URI.create("http://my.api.com"), "whatever")));
		}
	}

	@Nested
	@MockServerSettings(ports = 8090)
	class WithHeaders {

		@ParameterizedTest(name = "HTTP Request: {0} and HTTP Response: {1}")
		@ArgumentsSource(HTTPHeadersProvider.class)
		void shouldRunRequestAndReadTheHeaders(HttpRequest expectedRequest, HttpResponse expectedResponse, RequestDefinition definition) {
			mockServer.when(expectedRequest).respond(expectedResponse);

			Promise<HTTPResponse<Void>> promise = http.run(definition);

			HTTPResponse<Void> response = promise.join().unsafe();

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
									.withBody(requestBodyAsString)
									.withContentType(TEXT_PLAIN))
							.respond(response(expectedResponse)
									.withContentType(TEXT_PLAIN));

					RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello"), "POST",
							Headers.create(new Header("Content-Type", "text/plain")), new RequestDefinition.Body(requestBodyAsString),
							JavaType.valueOf(String.class));

					Promise<HTTPResponse<String>> promise = http.run(request);

					String response = promise.then(HTTPResponse::body).then(Except::unsafe).join().unsafe();

					assertEquals(expectedResponse, response);
				}
			}

			@Nested
			@MockServerSettings(ports = 8090)
			class Restrictions {

				@Test
				void missingContentType() {
					class MyObjectBody {}

					RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello"), "POST",
							Headers.empty(), new RequestDefinition.Body(new MyObjectBody()));

					assertThrows(HTTPRequestWriterException.class, () -> http.run(request));
				}

				@Test
				void unsupportedContentObject() {
					class MyObjectBody {}

					RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello"), "POST",
							Headers.create(new Header("Content-Type", "text/plain")), new RequestDefinition.Body(new MyObjectBody()));

					assertThrows(HTTPRequestWriterException.class, () -> http.run(request));
				}

				@Test
				void unsupportedContentType() {
					RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello"), "POST",
							Headers.create(new Header("Content-Type", "application/json")), new RequestDefinition.Body("hello"));

					assertThrows(HTTPRequestWriterException.class, () -> http.run(request));
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
					String expectedResponse = "promise";

					HttpRequest requestSpec = request("/hello").withMethod("GET");

					mockServer.clear(requestSpec)
							.when(requestSpec)
							.respond(response(expectedResponse)
									.withContentType(TEXT_PLAIN));

					RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello"), "GET",
							Headers.empty(), JavaType.valueOf(String.class));

					Promise<HTTPResponse<String>> promise = http.run(request);

					String response = promise.then(HTTPResponse::body).then(Except::unsafe).join().unsafe();

					assertEquals(expectedResponse, response);
				}
			}

			@Nested
			@MockServerSettings(ports = 8090)
			class Restrictions {

				@Test
				void unsupportedExpectedType() {
					class MyObjectBody {}

					mockServer.when(request("/hello")
									.withMethod("GET"))
							.respond(response("{\"message\":\"it works!\"}")
									.withContentType(APPLICATION_JSON));

					RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello"), "GET",
							Headers.empty(), JavaType.valueOf(MyObjectBody.class));

					Except<HTTPResponse<Object>> response = http.run(request).join();

					response.onSuccess(r -> fail("a HTTPResponseReaderException was expected."))
							.onFailure(e -> assertThat(e, instanceOf(HTTPResponseReaderException.class)));
				}

				@Test
				void unsupportedContentType() {
					mockServer.when(request("/hello")
								.withMethod("GET"))
							.respond(response("{\"message\":\"it works!\"}")
								.withContentType(APPLICATION_JSON));

					RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello"), "GET",
							Headers.empty(), JavaType.valueOf(String.class));

					Except<Object> response = http.run(request)
							.then(HTTPResponse::body)
							.then(Except::unsafe)
							.join();

					response.onSuccess(r -> fail("a HTTPResponseReaderException was expected, but the response is " + r))
							.onFailure(e -> assertThat(e, instanceOf(HTTPResponseReaderException.class)));
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

				RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello/" + statusCode), "GET",
						Headers.empty());

				HTTPResponse<Void> response = http.<Void> run(request)
						.join()
						.unsafe();

				assertThat(response, instanceOf(FailureHTTPResponse.class));

				FailureHTTPResponse<Void> failure = (FailureHTTPResponse<Void>) response;

				assertAll(() -> assertEquals(statusCode, failure.status().code()),
						  () -> assertThat(failure.status().message(), anyOf(equalTo(reason), nullValue())),
						  () -> assertThat(failure.headers(), hasItems(new HTTPHeader("X-Whatever", List.of("whatever")))));

				HTTPResponseException exception = assertThrows(HTTPResponseException.class, failure.body()::unsafe);

				assertAll(() -> assertEquals(failure.status(), exception.status()),
						  () -> assertEquals(failure.headers(), exception.headers()));
			}

			@ParameterizedTest(name = "{0} {1}")
			@ArgumentsSource(HTTPFailureExceptionsProvider.class)
			void recover(HTTPStatusCode statusCode, Class<? extends HTTPFailureResponseException> exceptionType) {
				mockServer.when(request("/hello/" + statusCode)
							.withMethod("GET"))
						.respond(response().withStatusCode(statusCode.value()));

				RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8090/hello/" + statusCode), "GET",
						Headers.empty());

				HTTPResponse<String> response = http.<String> run(request)
						.join()
						.unsafe();

				String recovered = "recovered";

				assertAll(() -> assertThat(response, instanceOf(FailureHTTPResponse.class)),
						  () -> assertEquals(recovered, response.recover(empty -> recovered).body().unsafe()),
						  () -> assertEquals(recovered, response.recover(exceptionType, e -> recovered).body().unsafe()),
						  () -> assertEquals(recovered, response.recover(statusCode, (status, headers, bodyAsBytes) -> recovered).body().unsafe()),
						  () -> assertEquals(recovered, response.recover(exceptionType::isInstance, e -> recovered).body().unsafe()));
			}
		}

		@Nested
		class ConnectionFailures {

			@Test
			void unknownHost() {
				RequestDefinition request = new RequestDefinition(URI.create("http://localhost:8099/hello"), "GET",
						Headers.empty());

				Except<HTTPResponse<Void>> response = http.<Void> run(request).join();

				response.onSuccess(r -> fail("a connection error was expected..."))
						.onFailure(e -> assertThat(e, instanceOf(HTTPClientException.class)))
						.onFailure(e -> assertThat(e.getCause(), instanceOf(IOException.class)));
			}
		}
	}

	static class HTTPMethodProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			HttpResponse expectedResponse = response("it works!").withContentType(TEXT_PLAIN);

			String requestBodyAsString = "{\"message\":\"hello\"}";

			return Stream.of(arguments(request("/hello").withMethod("GET"), 
									   expectedResponse,
									   new RequestDefinition(URI.create("http://localhost:8090/hello"), "GET", JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("POST").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new RequestDefinition(URI.create("http://localhost:8090/hello"), "POST",
							  				   		Headers.create(new Header("Content-Type", "text/plain")),
							  				   		new RequestDefinition.Body(requestBodyAsString),
											   		JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("PUT").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new RequestDefinition(URI.create("http://localhost:8090/hello"), "POST",
							  				   		Headers.create(new Header("Content-Type", "text/plain")),
							  				   		new RequestDefinition.Body(requestBodyAsString),
											   		JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("PATCH").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new RequestDefinition(URI.create("http://localhost:8090/hello"), "PATCH",
							  				   		Headers.create(new Header("Content-Type", "text/plain")),
											   		new RequestDefinition.Body(requestBodyAsString),
											   		JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("DELETE"), 
							  		   expectedResponse,
							  		   new RequestDefinition(URI.create("http://localhost:8090/hello"), "DELETE", JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("TRACE"),
							  		   response().withStatusCode(200),
							  		   new RequestDefinition(URI.create("http://localhost:8090/hello"), "TRACE")),
							 arguments(request("/hello").withMethod("HEAD"), 
							  		   response().withStatusCode(200),
							  		   new RequestDefinition(URI.create("http://localhost:8090/hello"), "HEAD")));
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
									   expectedResponse,
									   new RequestDefinition(URI.create("http://localhost:8090/hello"), "GET",
						  				   			Headers.create(new Header("X-Whatever-1", "whatever-header-value-1"),
						  				   						   new Header("X-Whatever-2", "whatever-header-value-2"),
						  				   						   new Header("X-Whatever-3", "value1", "value2")))),
							 arguments(request("/hello").withMethod("GET")
									 					.withCookie("session-id", "abc1234")
									 					.withCookie("csrftoken", "xyz9876")
									 					.withCookie("_gat", "1"),
								       expectedResponse,
						       		   new RequestDefinition(URI.create("http://localhost:8090/hello"), "GET",
											   		Headers.create(Cookies.create(new Cookie("session-id", "abc1234"),
																				  new Cookie("csrftoken", "xyz9876"),
																				  new Cookie("_gat", "1")).header().get()))));
		}
	}

	static class HTTPFailureStatusCodesProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			return IntStream.range(400, 600)
					.mapToObj(code -> org.junit.jupiter.params.provider.Arguments.of(code, HTTPStatusCode.select(code).map(HTTPStatusCode::message).orElse("unknown")));
		}
	}

	static class HTTPFailureExceptionsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			return Stream.of(arguments(HTTPStatusCode.BAD_REQUEST, BadRequest.class),
						     arguments(HTTPStatusCode.UNAUTHORIZED, Unauthorized.class),
						     arguments(HTTPStatusCode.FORBIDDEN, Forbidden.class),
						     arguments(HTTPStatusCode.NOT_FOUND, NotFound.class),
						     arguments(HTTPStatusCode.METHOD_NOT_ALLOWED, MethodNotAllowed.class),
						     arguments(HTTPStatusCode.NOT_ACCEPTABLE, NotAcceptable.class),
						     arguments(HTTPStatusCode.PROXY_AUTHENTATION_REQUIRED, ProxyAuthenticationRequired.class),
						     arguments(HTTPStatusCode.REQUEST_TIMEOUT, RequestTimeout.class),
						     arguments(HTTPStatusCode.CONFLICT, Conflict.class),
						     arguments(HTTPStatusCode.GONE, Gone.class),
						     arguments(HTTPStatusCode.LENGTH_REQUIRED, LengthRequired.class),
						     arguments(HTTPStatusCode.PRECONDITION_FAILED, PreconditionFailed.class),
						     arguments(HTTPStatusCode.REQUEST_ENTITY_TOO_LARGE, RequestEntityTooLarge.class),
						     arguments(HTTPStatusCode.REQUEST_URI_TOO_LONG, RequestURITooLong.class),
						     arguments(HTTPStatusCode.UNSUPPORTED_MEDIA_TYPE, UnsupportedMediaType.class),
						     arguments(HTTPStatusCode.REQUESTED_RANGE_NOT_SATISFIABLE, RequestedRangeNotSatisfiable.class),
						     arguments(HTTPStatusCode.EXPECTATION_FAILED, ExpectationFailed.class),
						     arguments(HTTPStatusCode.INTERNAL_SERVER_ERROR, InternalServerError.class),
						     arguments(HTTPStatusCode.NOT_IMPLEMENTED, NotImplemented.class),
						     arguments(HTTPStatusCode.BAD_GATEWAY, BadGateway.class),
						     arguments(HTTPStatusCode.SERVICE_UNAVAILABLE, ServiceUnavailable.class),
						     arguments(HTTPStatusCode.GATEWAY_TIMEOUT, GatewayTimeout.class),
						     arguments(HTTPStatusCode.HTTP_VERSION_NOT_SUPPORTED, HTTPVersionNotSupported.class));
		}
	}
}
