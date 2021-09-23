package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Cookie;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.EndpointDefinition;
import com.github.ljtfreitas.julian.EndpointDefinition.Parameter;
import com.github.ljtfreitas.julian.EndpointDefinition.Parameters;
import com.github.ljtfreitas.julian.EndpointDefinition.Path;
import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.contract.CookiesParameterSerializer;
import com.github.ljtfreitas.julian.contract.HeadersParameterSerializer;
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
import org.junit.jupiter.api.BeforeEach;
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

import java.io.IOException;
import java.util.Collection;
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
							   new HTTPMessageCodecs(List.of(new StringHTTPMessageCodec(MediaType.valueOf("text/*")))),
							   new DefaultHTTPResponseFailure());
	}

	@Nested
	@MockServerSettings(ports = 8090)
	class HTTPMethods {

		@ParameterizedTest(name = "HTTP Request: {0} and HTTP Response: {1}")
		@ArgumentsSource(HTTPMethodProvider.class)
		void shouldRunRequestAndReadTheResponse(HttpRequest expectedRequest, HttpResponse expectedResponse, Endpoint endpoint, Arguments arguments, 
				JavaType returnType) {

			mockServer.when(expectedRequest).respond(expectedResponse);

			Promise<HTTPRequest<String>> request = http.request(endpoint, arguments, returnType);

			HTTPResponse<String> response = request.bind(HTTPRequest::execute).join().unsafe();

			assertAll(() -> assertEquals(expectedResponse.getStatusCode(), response.status().code()),
					  () -> assertEquals(expectedResponse.getBodyAsString(), response.body()));
		}

		@Test
		void shouldThrowExceptionToUnsupportedHTTPMethod() {
			Endpoint endpoint = new EndpointDefinition(new Path("http://my.api.com"), "WHATEVER");
			assertThrows(IllegalArgumentException.class, () -> http.request(endpoint, Arguments.empty(), JavaType.none()));
		}
	}

	@Nested
	@MockServerSettings(ports = 8090)
	class WithHeaders {

		@ParameterizedTest(name = "HTTP Request: {0} and HTTP Response: {1}")
		@ArgumentsSource(HTTPHeadersProvider.class)
		void shouldRunRequestAndReadTheResponse(HttpRequest expectedRequest, HttpResponse expectedResponse, Endpoint endpoint, Arguments arguments) {
			mockServer.when(expectedRequest).respond(expectedResponse);

			Promise<HTTPRequest<Void>> request = http.request(endpoint, arguments, JavaType.none());

			HTTPResponse<Void> response = request.bind(HTTPRequest::execute).join().unsafe();

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

					Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello"), "POST",
							Headers.create(new Header("Content-Type", "text/plain")), Cookies.empty(),
							Parameters.create(EndpointDefinition.Parameter.body(0, "body", JavaType.valueOf(String.class))));

					Promise<HTTPRequest<String>> request = http.request(endpoint, Arguments.create(requestBodyAsString), JavaType.valueOf(String.class));

					String response = request.bind(HTTPRequest::execute).then(HTTPResponse::body).join().unsafe();

					assertEquals(expectedResponse, response);
				}
			}

			@Nested
			@MockServerSettings(ports = 8090)
			class Restrictions {

				@Test
				void missingContentType() {
					class MyObjectBody {}

					Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello"), "POST",
							Headers.empty(), Cookies.empty(),
							Parameters.create(EndpointDefinition.Parameter.body(0, "body", JavaType.valueOf(MyObjectBody.class))));

					assertThrows(HTTPRequestWriterException.class, () -> http.request(endpoint, Arguments.create(new MyObjectBody()), JavaType.none()));
				}

				@Test
				void unsupportedContentObject() {
					class MyObjectBody {}

					Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello"), "POST",
							Headers.create(new Header("Content-Type", "text/plain")), Cookies.empty(),
							Parameters.create(EndpointDefinition.Parameter.body(0, "body", JavaType.valueOf(MyObjectBody.class))));

					assertThrows(HTTPRequestWriterException.class, () -> http.request(endpoint, Arguments.create(new MyObjectBody()), JavaType.none()));
				}

				@Test
				void unsupportedContentType() {
					Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello"), "POST",
							Headers.create(new Header("Content-Type", "application/json")), Cookies.empty(),
							Parameters.create(EndpointDefinition.Parameter.body(0, "body", JavaType.valueOf(String.class))));

					assertThrows(HTTPRequestWriterException.class, () -> http.request(endpoint, Arguments.create("body"), JavaType.none()));
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

					JavaType responseType = JavaType.valueOf(String.class);

					Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello"), "GET",
							Headers.empty(), Cookies.empty(),
							Parameters.empty(), responseType);

					Promise<HTTPRequest<String>> request = http.request(endpoint, Arguments.empty(), responseType);

					String response = request.bind(HTTPRequest::execute).then(HTTPResponse::body).join().unsafe();

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

					JavaType responseType = JavaType.valueOf(MyObjectBody.class);

					Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello"), "GET",
													Headers.empty(), Cookies.empty(),
													Parameters.empty(), responseType);

					Except<HTTPResponse<Object>> response = http.request(endpoint, Arguments.empty(), responseType)
							.bind(HTTPRequest::execute)
							.join();

					response.consumes(r -> fail("a HTTPResponseReaderException was expected."))
							.failure(e -> assertThat(e, instanceOf(HTTPResponseReaderException.class)));
				}

				@Test
				void unsupportedContentType() {
					mockServer.when(request("/hello")
								.withMethod("GET"))
							.respond(response("{\"message\":\"it works!\"}")
								.withContentType(APPLICATION_JSON));

					JavaType responseType = JavaType.valueOf(String.class);

					Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello"), "GET",
													Headers.empty(), Cookies.empty(),
													Parameters.empty(), responseType);

					Except<Object> response = http.request(endpoint, Arguments.empty(), responseType)
							.bind(HTTPRequest::execute)
							.then(HTTPResponse::body)
							.join();

					response.consumes(r -> fail("a HTTPResponseReaderException was expected, but the response is " + r))
							.failure(e -> assertThat(e, instanceOf(HTTPResponseReaderException.class)));
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

				Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello/" + statusCode), "GET",
												Headers.empty(), Cookies.empty(), Parameters.empty());

				HTTPResponse<Void> response = http.<Void> request(endpoint, Arguments.empty(), JavaType.none())
						.bind(HTTPRequest::execute)
						.join()
						.unsafe();

				assertThat(response, instanceOf(FailureHTTPResponse.class));

				FailureHTTPResponse<Void> failure = (FailureHTTPResponse<Void>) response;

				assertAll(() -> assertEquals(statusCode, failure.status().code()),
						  () -> assertThat(failure.status().message(), anyOf(equalTo(reason), nullValue())),
						  () -> assertThat(failure.headers(), hasItems(new HTTPHeader("X-Whatever", List.of("whatever")))));

				HTTPResponseException exception = assertThrows(HTTPResponseException.class, failure::body);

				assertAll(() -> assertEquals(failure.status(), exception.status()),
						  () -> assertEquals(failure.headers(), exception.headers()));
			}
			
			@ParameterizedTest(name = "{0} {1}")
			@ArgumentsSource(HTTPFailureExceptionsProvider.class)
			void recover(HTTPStatusCode statusCode, Class<? extends HTTPFailureResponseException> exceptionType) {
				mockServer.when(request("/hello/" + statusCode)
							.withMethod("GET"))
						.respond(response().withStatusCode(statusCode.value()));

				Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8090/hello/" + statusCode), "GET",
												Headers.empty(), Cookies.empty(), Parameters.empty());

				HTTPResponse<String> response = http.<String> request(endpoint, Arguments.empty(), JavaType.none())
						.bind(HTTPRequest::execute)
						.join()
						.unsafe();

				String recovered = "recovered";
				
				assertAll(() -> assertThat(response, instanceOf(FailureHTTPResponse.class)),
						  () -> assertEquals(recovered, response.recover(empty -> recovered).body()),
						  () -> assertEquals(recovered, response.recover(exceptionType, e -> recovered).body()),
						  () -> assertEquals(recovered, response.recover(statusCode, e -> recovered).body()),
						  () -> assertEquals(recovered, response.recover(exceptionType::isInstance, e -> recovered).body()));
			}
		}

		@Nested
		class ConnectionFailures {

			@Test
			void unknownHost() {
				Endpoint endpoint = new EndpointDefinition(new Path("http://localhost:8091/hello"), "GET",
						Headers.empty(), Cookies.empty(), Parameters.empty());

				Except<HTTPResponse<Void>> response = http.<Void> request(endpoint, Arguments.empty(), JavaType.none())
						.bind(HTTPRequest::execute)
						.join();

				response.consumes(r -> fail("a connection error was expected..."))
						.failure(e -> assertThat(e, instanceOf(HTTPClientException.class)))
						.failure(e -> assertThat(e.getCause(), instanceOf(IOException.class)));
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
									   new EndpointDefinition(new Path("http://localhost:8090/hello")),
									   Arguments.empty(),
									   JavaType.valueOf(String.class)),
							 arguments(request("/hello").withMethod("POST").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "POST",
							  				   		Headers.create(new Header("Content-Type", "text/plain")),
							  				   		Cookies.empty(),
							  				   		Parameters.create(EndpointDefinition.Parameter.body(0, "body", JavaType.valueOf(String.class)))),
									   Arguments.create(requestBodyAsString),
									   JavaType.valueOf(String.class)),
							 arguments(request("/hello").withMethod("PUT").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "POST",
							  				   		Headers.create(new Header("Content-Type", "text/plain")),
							  				   		Cookies.empty(),
							  				   		Parameters.create(EndpointDefinition.Parameter.body(0, "body", JavaType.valueOf(String.class)))),
									   Arguments.create(requestBodyAsString),
									   JavaType.valueOf(String.class)),
							 arguments(request("/hello").withMethod("PATCH").withBody(requestBodyAsString), 
							  		   expectedResponse,
							  		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "PATCH",
							  				   		Headers.create(new Header("Content-Type", "text/plain")),
							  				   		Cookies.empty(),
							  				   		Parameters.create(EndpointDefinition.Parameter.body(0, "body", JavaType.valueOf(String.class)))),
									   Arguments.create(requestBodyAsString),
									   JavaType.valueOf(String.class)),
							 arguments(request("/hello").withMethod("DELETE"), 
							  		   expectedResponse,
							  		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "DELETE",
							  				   		Headers.empty(),
							  				   		Cookies.empty(),
							  				   		Parameters.empty()),
									   Arguments.empty(),
									   JavaType.valueOf(String.class)),
							 arguments(request("/hello").withMethod("TRACE"), 
							  		   response().withStatusCode(200),
							  		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "TRACE",
							  				   		Headers.empty(),
							  				   		Cookies.empty(),
							  				   		Parameters.empty()),
									   Arguments.empty(),
									   JavaType.none()),
							 arguments(request("/hello").withMethod("HEAD"), 
							  		   response().withStatusCode(200),
							  		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "HEAD",
							  				   		Headers.empty(),
							  				   		Cookies.empty(),
							  				   		Parameters.empty()),
									   Arguments.empty(),
									   JavaType.none()));
		}
	}
	
	static class HTTPHeadersProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			
			HttpResponse expectedResponse = response().withStatusCode(HTTPStatusCode.OK.value())
													  .withHeader("X-Response-Header-1", "response-header-value-1")
													  .withHeader("X-Response-Header-2", "response-header-value-2")
													  .withHeader("X-Response-Header-3", "value1", "value2");

			HeadersParameterSerializer headersParameterSerializer = new HeadersParameterSerializer();
			CookiesParameterSerializer cookiesParameterSerializer = new CookiesParameterSerializer();

			return Stream.of(arguments(request("/hello").withMethod("GET")
														.withHeader("X-Whatever-1", "whatever-header-value-1")
														.withHeader("X-Whatever-2", "whatever-header-value-2")
														.withHeader("X-Whatever-3", "value1", "value2"),
									   expectedResponse,
									   new EndpointDefinition(new Path("http://localhost:8090/hello"), "GET",
						  				   			Headers.create(new Header("X-Whatever-1", "whatever-header-value-1"),
						  				   						   new Header("X-Whatever-2", "whatever-header-value-2"),
						  				   						   new Header("X-Whatever-3", "value1", "value2")),
						  				   			Cookies.empty(),
						  				   			Parameters.empty()),
									   Arguments.empty()),
							 arguments(request("/hello").withMethod("GET")
													    .withHeader("X-Whatever-1", "whatever-header-value-1")
													    .withHeader("X-Whatever-2", "whatever-header-value-2")
													    .withHeader("X-Whatever-3", "value1", "value2"),
								       expectedResponse,
						       		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "GET",
						       				   		Headers.empty(),
						       				   		Cookies.empty(),
						       				   		Parameters.create(Parameter.header(0, "X-Whatever-1",  JavaType.valueOf(String.class), headersParameterSerializer),
						       				   						  Parameter.header(1, "X-Whatever-2",  JavaType.valueOf(String.class), headersParameterSerializer),
					       				   							  Parameter.header(2, "X-Whatever-3",  JavaType.parameterized(Collection.class, String.class), headersParameterSerializer))),
						       		   Arguments.create("whatever-header-value-1", "whatever-header-value-2", List.of("value1", "value2"))),
							 arguments(request("/hello").withMethod("GET")
													    .withCookie("session-id", "abc1234"),
								       expectedResponse,
						       		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "GET",
						       				   		Headers.empty(),
						       				   		Cookies.create(new Cookie("session-id", "abc1234")),
						       				   		Parameters.empty()),
						       		   Arguments.empty()),
							 arguments(request("/hello").withMethod("GET")
									 					.withCookie("session-id", "abc1234")
									 					.withCookie("csrftoken", "xyz9876")
									 					.withCookie("_gat", "1"),
								       expectedResponse,
						       		   new EndpointDefinition(new Path("http://localhost:8090/hello"), "GET",
						       				   		Headers.empty(),
						       				   		Cookies.empty(),
						       				   		Parameters.create(Parameter.cookie(0, "session-id",  JavaType.valueOf(String.class), cookiesParameterSerializer),
						       				   						  Parameter.cookie(1, "cookies",  JavaType.valueOf(Cookies.class), cookiesParameterSerializer))),
						       		   Arguments.create("abc1234", Cookies.create(new Cookie("csrftoken", "xyz9876"), new Cookie("_gat", "1")))));
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
