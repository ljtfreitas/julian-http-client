package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Response;
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
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DefaultHTTPResponseFailureTest {

	private final HTTPResponseFailure failure = new DefaultHTTPResponseFailure();

	@ParameterizedTest(name = "HTTP Status: {0}")
	@MethodSource("failureHTTPStatuses")
	void shouldCreateTheFailureResponse(HTTPStatus status, Class<? extends HTTPResponseException> exception) {
		HTTPHeaders headers = HTTPHeaders.empty();
		String responseBody = "response body";

		HTTPClientResponse source = new HTTPClientResponse() {

			@Override
			public HTTPStatus status() {
				return status;
			}

			@Override
			public HTTPHeaders headers() {
				return headers;
			}

			@Override
			public DefaultHTTPResponseBody body() {
				return new DefaultHTTPResponseBody(responseBody.getBytes());
			}

			@Override
			public <T, R extends Response<T, HTTPResponseException>> Optional<R> failure(Function<? super HTTPClientResponse, R> fn) {
				return Optional.empty();
			}

			@Override
			public <T, R extends Response<T, HTTPResponseException>> Optional<R> success(Function<? super HTTPClientResponse, R> fn) {
				return Optional.empty();
			}
		};
		
		HTTPResponse<Object> failed = failure.apply(source, JavaType.valueOf(String.class));

		assertAll(()-> assertNotNull(failed), 
				  () -> assertEquals(status, failed.status()),
				  () -> assertEquals(headers, failed.headers()));

		HTTPResponseException httpResponseException = assertThrows(exception, failed.body()::unsafe);

		assertAll(() -> assertEquals(status, httpResponseException.status()),
				  () -> assertEquals(headers, httpResponseException.headers()),
				  () -> assertEquals(responseBody, httpResponseException.body()));
	}

	static Stream<Arguments> failureHTTPStatuses() {
		return Stream.of(arguments(HTTPStatus.valueOf(HTTPStatusCode.BAD_REQUEST), BadRequest.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.UNAUTHORIZED), Unauthorized.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.FORBIDDEN), Forbidden.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.NOT_FOUND), NotFound.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.METHOD_NOT_ALLOWED), MethodNotAllowed.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.NOT_ACCEPTABLE), NotAcceptable.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.PROXY_AUTHENTATION_REQUIRED), ProxyAuthenticationRequired.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.REQUEST_TIMEOUT), RequestTimeout.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.CONFLICT), Conflict.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.GONE), Gone.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.LENGTH_REQUIRED), LengthRequired.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.PRECONDITION_FAILED), PreconditionFailed.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.REQUEST_ENTITY_TOO_LARGE), RequestEntityTooLarge.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.REQUEST_URI_TOO_LONG), RequestURITooLong.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.UNSUPPORTED_MEDIA_TYPE), UnsupportedMediaType.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.REQUESTED_RANGE_NOT_SATISFIABLE), RequestedRangeNotSatisfiable.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.EXPECTATION_FAILED), ExpectationFailed.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.INTERNAL_SERVER_ERROR), InternalServerError.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.NOT_IMPLEMENTED), NotImplemented.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.BAD_GATEWAY), BadGateway.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.SERVICE_UNAVAILABLE), ServiceUnavailable.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.GATEWAY_TIMEOUT), GatewayTimeout.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.HTTP_VERSION_NOT_SUPPORTED), HTTPVersionNotSupported.class),
					     arguments(HTTPStatus.valueOf(499), HTTPResponseException.class));
	}
}
