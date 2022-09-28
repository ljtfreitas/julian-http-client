package com.github.ljtfreitas.julian.http;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.ExpectationFailed;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.FailedDependency;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.IamATeapot;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.Locked;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.PreconditionRequired;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.RequestHeaderFieldsTooLarge;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.TooEarly;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.TooManyRequests;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.UnavailableForLegalReasons;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.UpgradeRequired;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.BandwidthLimitExceeded;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.HTTPVersionNotSupported;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.InsufficientStorage;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.LoopDetected;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.NetWorkAuthenticationRequired;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.NotExtended;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import com.github.ljtfreitas.julian.Cookie;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.BadRequest;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.Conflict;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.UnprocessableEntity;
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
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.BadGateway;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.GatewayTimeout;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.VariantAlsoNegotiates;
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
							   new HTTPMessageCodecs(List.of(new StringHTTPMessageCodec(MediaType.valueOf("text/*")), new UnprocessableHTTPMessageCodec())),
							   new HTTPRequestInterceptorChain(),
							   new DefaultHTTPResponseFailure());
	}

	@Nested
	@MockServerSettings(ports = 8090)
	class HTTPMethods {

		@ParameterizedTest(name = "HTTP Request: {0} and HTTP Response: {1}")
		@ArgumentsSource(HTTPMethodProvider.class)
		void shouldRunRequestAndReadTheResponse(HttpRequest expectedRequest, HttpResponse expectedResponse, HTTPEndpoint request) {

			mockServer.when(expectedRequest).respond(expectedResponse);

			Promise<HTTPResponse<String>> promise = http.run(request);

			HTTPResponse<String> response = promise.join().unsafe();

			assertAll(() -> assertEquals(expectedResponse.getStatusCode(), response.status().code()),
					  () -> assertEquals(expectedResponse.getBodyAsString(), response.body().unsafe()));
		}

	}

	@Nested
	@MockServerSettings(ports = 8090)
	class WithHeaders {

		@ParameterizedTest(name = "HTTP Request: {0} and HTTP Response: {1}")
		@ArgumentsSource(HTTPHeadersProvider.class)
		void shouldRunRequestAndReadTheHeaders(HttpRequest expectedRequest, HttpResponse expectedResponse, HTTPEndpoint request) {
			mockServer.when(expectedRequest).respond(expectedResponse);

			Promise<HTTPResponse<Void>> promise = http.run(request);

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

					HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.POST,
							HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
							new HTTPEndpoint.Body(requestBodyAsString),
							JavaType.valueOf(String.class));

					Promise<HTTPResponse<String>> promise = http.run(request);

					String response = promise.then(HTTPResponse::body).then(Attempt::unsafe).join().unsafe();

					assertEquals(expectedResponse, response);
				}
			}

			@Nested
			@MockServerSettings(ports = 8090)
			class Restrictions {

				@Test
				void missingContentType() {
					class MyObjectBody {}

					HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.POST,
							HTTPHeaders.empty(), new HTTPEndpoint.Body(new MyObjectBody()));

					assertThrows(HTTPRequestWriterException.class, () -> http.run(request));
				}

				@Test
				void unsupportedContentObject() {
					class MyObjectBody {}

					HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.POST,
							HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
							new HTTPEndpoint.Body(new MyObjectBody()));

					assertThrows(HTTPRequestWriterException.class, () -> http.run(request));
				}

				@Test
				void unsupportedContentType() {
					HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.POST,
							HTTPHeaders.create(new HTTPHeader("Content-Type", "application/json")),
							new HTTPEndpoint.Body("hello"));

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

					HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.GET,
							HTTPHeaders.empty(), null, JavaType.valueOf(String.class));

					Promise<HTTPResponse<String>> promise = http.run(request);

					String response = promise.then(HTTPResponse::body).then(Attempt::unsafe).join().unsafe();

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

					HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.GET,
							HTTPHeaders.empty(), null, JavaType.valueOf(MyObjectBody.class));

					Attempt<HTTPResponse<Object>> response = http.run(request).join();

					response.onSuccess(r -> fail("a HTTPResponseReaderException was expected."))
							.onFailure(e -> assertThat(e, instanceOf(HTTPResponseReaderException.class)));
				}

				@Test
				void unsupportedContentType() {
					mockServer.when(request("/hello")
								.withMethod("GET"))
							.respond(response("{\"message\":\"it works!\"}")
								.withContentType(APPLICATION_JSON));

					HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.GET,
							HTTPHeaders.empty(), null, JavaType.valueOf(String.class));

					Attempt<Object> response = http.run(request)
							.then(HTTPResponse::body)
							.then(Attempt::unsafe)
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

				HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello/" + statusCode), HTTPMethod.GET);

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

				HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8090/hello/" + statusCode), HTTPMethod.GET);

				HTTPResponse<String> response = http.<String> run(request)
						.join()
						.unsafe();

				String recovered = "recovered";

				assertAll(() -> assertThat(response, instanceOf(FailureHTTPResponse.class)),
						  () -> assertEquals(recovered, response.recover(empty -> recovered).body().unsafe()),
						  () -> assertEquals(recovered, response.recover(e -> recovered).body().unsafe()),
						  () -> assertEquals(recovered, response.recover(statusCode, (status, headers, bodyAsBytes) -> recovered).body().unsafe()),
						  () -> assertEquals(recovered, response.recover(exceptionType::isInstance, e -> recovered).body().unsafe()));
			}
		}

		@Nested
		class ConnectionFailures {

			@Test
			void unknownHost() {
				HTTPEndpoint request = new HTTPEndpoint(URI.create("http://localhost:8099/hello"), HTTPMethod.GET);

				Attempt<HTTPResponse<Void>> response = http.<Void> run(request).join();

				response.onSuccess(r -> fail("a connection error was expected..."))
						.onFailure(e -> assertThat(e, instanceOf(HTTPClientException.class)))
						.onFailure(e -> assertThat(e.getCause(), instanceOf(IOException.class)));
			}
		}
	}

	@Nested
	@MockServerSettings(ports = 8093)
	class DSL {

		private HTTP.DSL httpAsDsl;

		@BeforeEach
		void before() {
			httpAsDsl = http.asDSL();
		}

		@Test
		void shouldRunGET() {
			HttpRequest requestDefinition = request("/get").withMethod("GET");

			mockServer.clear(requestDefinition)
					.when(requestDefinition)
					.respond(response("GET is working")
							.withContentType(TEXT_PLAIN));

			Promise<String> promise = httpAsDsl.GET(URI.create("http://localhost:8093/get"))
					.run(String.class)
					.then(r -> r.body().unsafe());

			assertEquals("GET is working", promise.join().unsafe());
		}

		@Test
		void shouldRunPOST() {
			HttpRequest requestDefinition = request("/post").withMethod("POST");

			mockServer.clear(requestDefinition)
					.when(requestDefinition)
					.respond(response("POST is working")
							.withContentType(TEXT_PLAIN));

			Promise<String> promise = httpAsDsl.POST(URI.create("http://localhost:8093/post"))
					.header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
					.body("i am a body")
					.run(String.class)
					.then(r -> r.body().unsafe());

			assertEquals("POST is working", promise.join().unsafe());
		}

		@Test
		void shouldRunPUT() {
			HttpRequest requestDefinition = request("/put")
					.withMethod("PUT")
					.withBody("i am a body")
					.withContentType(TEXT_PLAIN);

			mockServer.clear(requestDefinition).when(requestDefinition)
					.respond(response("PUT is working")
							.withContentType(TEXT_PLAIN));

			Promise<String> promise = httpAsDsl.PUT(URI.create("http://localhost:8093/put"))
					.header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
					.body("i am a body")
					.run(String.class)
					.then(r -> r.body().unsafe());

			assertEquals("PUT is working", promise.join().unsafe());
		}

		@Test
		void shouldRunPATCH() {
			HttpRequest requestDefinition = request("/patch")
					.withMethod("PATCH")
					.withBody("i am a body")
					.withContentType(TEXT_PLAIN);

			mockServer.clear(requestDefinition).when(requestDefinition)
					.respond(response("PATCH is working")
							.withContentType(TEXT_PLAIN));

			Promise<String> promise = httpAsDsl.PATCH(URI.create("http://localhost:8093/patch"))
					.header(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))
					.body("i am a body")
					.run(String.class)
					.then(r -> r.body().unsafe());

			assertEquals("PATCH is working", promise.join().unsafe());
		}

		@Test
		void shouldRunDELETE() {
			mockServer.when(request("/delete")
							.withMethod("DELETE"))
					.respond(response().withStatusCode(200));

			Promise<HTTPStatus> promise = httpAsDsl.DELETE(URI.create("http://localhost:8093/delete"))
					.run()
					.then(HTTPResponse::status);

			HTTPStatus status = promise.join().unsafe();

			assertTrue(status.is(HTTPStatusCode.OK));
		}

		@DisplayName("Should run a HEAD request.")
		@Test
		void shouldRunHEAD() {
			HttpRequest requestDefinition = request("/head").withMethod("HEAD");

			mockServer.clear(requestDefinition)
					.when(requestDefinition)
					.respond(response().withHeader("x-my-header", "whatever"));

			Promise<HTTPHeaders> promise = httpAsDsl.HEAD(URI.create("http://localhost:8093/head"))
					.run()
					.then(HTTPResponse::headers);

			HTTPHeaders headers = promise.join().unsafe();

			assertThat(headers, hasItems(new HTTPHeader("x-my-header", "whatever")));

			mockServer.verify(requestDefinition);
		}

		@DisplayName("Should run a OPTIONS request.")
		@Test
		void shouldRunOPTIONS() {
			HttpRequest requestDefinition = request("/options").withMethod("OPTIONS");

			mockServer.clear(requestDefinition)
					.when(requestDefinition)
					.respond(response().withHeader("Allow", "GET, POST, PUT, DELETE"));

			Promise<HTTPHeaders> promise = httpAsDsl.OPTIONS(URI.create("http://localhost:8093/options"))
					.run()
					.then(HTTPResponse::headers);

			HTTPHeaders headers = promise.join().unsafe();

			assertThat(headers, hasItems(new HTTPHeader(HTTPHeader.ALLOW, "GET, POST, PUT, DELETE")));

			mockServer.verify(requestDefinition);
		}

		@DisplayName("Should run a TRACE request.")
		@Test
		void shouldRunTRACE() {
			HttpRequest requestDefinition = request("/trace").withMethod("TRACE");

			mockServer.clear(requestDefinition)
					.when(requestDefinition)
					.respond(response().withStatusCode(200));

			Promise<HTTPStatus> promise = httpAsDsl.TRACE(URI.create("http://localhost:8093/trace"))
					.run()
					.then(HTTPResponse::status);

			HTTPStatus status = promise.join().unsafe();

			assertTrue(status.is(HTTPStatusCode.OK));

			mockServer.verify(requestDefinition);
		}
	}

	static class HTTPMethodProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			HttpResponse expectedResponse = response("it works!").withContentType(TEXT_PLAIN);

			String requestBodyAsString = "{\"message\":\"hello\"}";

			return Stream.of(arguments(request("/hello").withMethod("GET"), 
									   expectedResponse,
									   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.GET,
											   HTTPHeaders.empty(), null, JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("POST").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.POST,
							  				   		HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
							  				   		new HTTPEndpoint.Body(requestBodyAsString),
											   		JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("PUT").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.PUT,
							  				   		HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
							  				   		new HTTPEndpoint.Body(requestBodyAsString),
											   		JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("PATCH").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.PATCH,
							  				   		HTTPHeaders.create(new HTTPHeader("Content-Type", "text/plain")),
											   		new HTTPEndpoint.Body(requestBodyAsString),
											   		JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("DELETE"), 
							  		   expectedResponse,
							  		   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.DELETE,
											   HTTPHeaders.empty(), null, JavaType.valueOf(String.class))),
							 arguments(request("/hello").withMethod("TRACE"),
							  		   response().withStatusCode(200),
							  		   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.TRACE)),
							 arguments(request("/hello").withMethod("HEAD"), 
							  		   response().withStatusCode(200),
							  		   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.HEAD)));
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
									   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.GET,
						  				   			HTTPHeaders.create(new HTTPHeader("X-Whatever-1", "whatever-header-value-1"),
						  				   						   new HTTPHeader("X-Whatever-2", "whatever-header-value-2"),
						  				   						   new HTTPHeader("X-Whatever-3", List.of("value1", "value2"))))),
							 arguments(request("/hello").withMethod("GET")
									 					.withCookie("session-id", "abc1234")
									 					.withCookie("csrftoken", "xyz9876")
									 					.withCookie("_gat", "1"),
								       expectedResponse,
						       		   new HTTPEndpoint(URI.create("http://localhost:8090/hello"), HTTPMethod.GET,
											   		HTTPHeaders.create(Headers.create(Cookies.create(
														   	new Cookie("session-id", "abc1234"),
															new Cookie("csrftoken", "xyz9876"),
															new Cookie("_gat", "1")).header().get())))));
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
						     arguments(HTTPStatusCode.I_AM_A_TEAPOT, IamATeapot.class),
							 arguments(HTTPStatusCode.UNPROCESSABLE_ENTITY, UnprocessableEntity.class),
						     arguments(HTTPStatusCode.LOCKED, Locked.class),
						     arguments(HTTPStatusCode.FAILED_DEPENDENCY, FailedDependency.class),
						     arguments(HTTPStatusCode.TOO_EARLY, TooEarly.class),
						     arguments(HTTPStatusCode.UPGRADE_REQUIRED, UpgradeRequired.class),
						     arguments(HTTPStatusCode.PRECONDITION_REQUIRED, PreconditionRequired.class),
						     arguments(HTTPStatusCode.TOO_MANY_REQUESTS, TooManyRequests.class),
						     arguments(HTTPStatusCode.REQUEST_HEADER_FIELDS_TOO_LARGE, RequestHeaderFieldsTooLarge.class),
						     arguments(HTTPStatusCode.UNAVAILABLE_FOR_LEGAL_REASONS, UnavailableForLegalReasons.class),

						     arguments(HTTPStatusCode.INTERNAL_SERVER_ERROR, InternalServerError.class),
						     arguments(HTTPStatusCode.NOT_IMPLEMENTED, NotImplemented.class),
						     arguments(HTTPStatusCode.BAD_GATEWAY, BadGateway.class),
						     arguments(HTTPStatusCode.SERVICE_UNAVAILABLE, ServiceUnavailable.class),
						     arguments(HTTPStatusCode.GATEWAY_TIMEOUT, GatewayTimeout.class),
						     arguments(HTTPStatusCode.HTTP_VERSION_NOT_SUPPORTED, HTTPVersionNotSupported.class),
						     arguments(HTTPStatusCode.VARIANT_ALSO_NEGOTIATES, VariantAlsoNegotiates.class),
						     arguments(HTTPStatusCode.INSUFFICIENT_STORAGE, InsufficientStorage.class),
						     arguments(HTTPStatusCode.LOOP_DETECTED, LoopDetected.class),
						     arguments(HTTPStatusCode.BANDWIDTH_LIMIT_EXCEEDED, BandwidthLimitExceeded.class),
						     arguments(HTTPStatusCode.NOT_EXTENDED, NotExtended.class),
						     arguments(HTTPStatusCode.NETWORK_AUTHENTICATION_REQUIRED, NetWorkAuthenticationRequired.class));
		}
	}
}
