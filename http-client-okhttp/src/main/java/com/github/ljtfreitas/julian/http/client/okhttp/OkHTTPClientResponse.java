package com.github.ljtfreitas.julian.http.client.okhttp;

import com.github.ljtfreitas.julian.Bracket;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import okhttp3.ResponseBody;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

class OkHTTPClientResponse implements HTTPClientResponse {

    private final HTTPClientResponse response;

    public OkHTTPClientResponse(HTTPClientResponse response) {
        this.response = response;
    }

    @Override
    public HTTPStatus status() {
        return response.status();
    }

    @Override
    public HTTPHeaders headers() {
        return response.headers();
    }

    @Override
    public HTTPResponseBody body() {
        return response.body();
    }

    @Override
    public <T, R extends Response<T, ? extends Throwable>> Optional<R> success(Function<? super HTTPClientResponse, R> fn) {
        return response.success(fn);
    }

    @Override
    public <T, R extends Response<T, ? extends Throwable>> Optional<R> failure(Function<? super HTTPClientResponse, R> fn) {
        return response.failure(fn);
    }

    static OkHTTPClientResponse valueOf(okhttp3.Response response) {
        HTTPStatus status = HTTPStatusCode.select(response.code()).map(HTTPStatus::new)
                .orElseGet(() -> new HTTPStatus(response.code(), response.message()));

        HTTPHeaders headers = response.headers().names().stream()
                .map(name -> HTTPHeader.create(name, response.headers().values(name)))
                .reduce(HTTPHeaders.empty(), HTTPHeaders::join, (a, b) -> b);

        Supplier<HTTPResponseBody> responseBody = () -> Bracket.acquire(response::body)
                .map(ResponseBody::bytes)
                .map(HTTPResponseBody::some)
                .prop(HTTPResponseReaderException::new);

        return new OkHTTPClientResponse(HTTPClientResponse.create(status, headers,
                HTTPResponseBody.optional(status, headers, responseBody)));
    }
}
