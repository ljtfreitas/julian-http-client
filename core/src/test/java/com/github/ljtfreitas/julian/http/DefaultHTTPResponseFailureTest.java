package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.BadRequest;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.Conflict;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.ExpectationFailed;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.FailedDependency;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.IamATeapot;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.PreconditionRequired;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.RequestHeaderFieldsTooLarge;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.TooEarly;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.TooManyRequests;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.UnavailableForLegalReasons;
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
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.UpgradeRequired;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.BadGateway;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.BandwidthLimitExceeded;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.GatewayTimeout;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.HTTPVersionNotSupported;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.InsufficientStorage;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.LoopDetected;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.NetWorkAuthenticationRequired;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.NotExtended;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.VariantAlsoNegotiates;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.InternalServerError;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.NotImplemented;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.ServiceUnavailable;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;
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
			public HTTPResponseBody body() {
				return new PublisherHTTPResponseBody(new SimplePublisher(responseBody));
			}

			@Override
			public <T, R extends Response<T, ? extends Throwable>> Optional<R> failure(Function<? super HTTPClientResponse, R> fn) {
				return Optional.empty();
			}

			@Override
			public <T, R extends Response<T, ? extends Throwable>> Optional<R> success(Function<? super HTTPClientResponse, R> fn) {
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
				  () -> assertEquals(responseBody, httpResponseException.bodyAsString()));
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
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.I_AM_A_TEAPOT), IamATeapot.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.UNPROCESSABLE_ENTITY), UnprocessableEntity.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.LOCKED), HTTPClientFailureResponseException.Locked.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.FAILED_DEPENDENCY), FailedDependency.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.TOO_EARLY), TooEarly.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.UPGRADE_REQUIRED), UpgradeRequired.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.PRECONDITION_REQUIRED), PreconditionRequired.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.TOO_MANY_REQUESTS), TooManyRequests.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.REQUEST_HEADER_FIELDS_TOO_LARGE), RequestHeaderFieldsTooLarge.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.UNAVAILABLE_FOR_LEGAL_REASONS), UnavailableForLegalReasons.class),
						 arguments(HTTPStatus.valueOf(499), HTTPResponseException.class),

					     arguments(HTTPStatus.valueOf(HTTPStatusCode.INTERNAL_SERVER_ERROR), InternalServerError.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.NOT_IMPLEMENTED), NotImplemented.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.BAD_GATEWAY), BadGateway.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.SERVICE_UNAVAILABLE), ServiceUnavailable.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.GATEWAY_TIMEOUT), GatewayTimeout.class),
					     arguments(HTTPStatus.valueOf(HTTPStatusCode.HTTP_VERSION_NOT_SUPPORTED), HTTPVersionNotSupported.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.VARIANT_ALSO_NEGOTIATES), VariantAlsoNegotiates.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.INSUFFICIENT_STORAGE), InsufficientStorage.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.LOOP_DETECTED), LoopDetected.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.BANDWIDTH_LIMIT_EXCEEDED), BandwidthLimitExceeded.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.NOT_EXTENDED), NotExtended.class),
						 arguments(HTTPStatus.valueOf(HTTPStatusCode.NETWORK_AUTHENTICATION_REQUIRED), NetWorkAuthenticationRequired.class));

	}

	private class SimplePublisher implements Flow.Publisher<List<ByteBuffer>> {

		private final String source;

		SimplePublisher(String source) {
			this.source = source;
		}

		@Override
		public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
			subscriber.onSubscribe(new Flow.Subscription() {


				@Override
				public void request(long n) {
					subscriber.onNext(List.of(ByteBuffer.wrap(source.getBytes())));
					subscriber.onComplete();
				}

				@Override
				public void cancel() {}
			});
		}
	}
}
