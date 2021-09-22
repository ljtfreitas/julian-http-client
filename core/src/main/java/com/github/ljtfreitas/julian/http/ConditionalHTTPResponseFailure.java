package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;

import java.util.Map;

public class ConditionalHTTPResponseFailure implements HTTPResponseFailure {

    private final Map<HTTPStatusCode, HTTPResponseFailure> failures;
    private final HTTPResponseFailure failure;

    public ConditionalHTTPResponseFailure(Map<HTTPStatusCode, HTTPResponseFailure> failures) {
        this(failures, DefaultHTTPResponseFailure.get());
    }

    public ConditionalHTTPResponseFailure(Map<HTTPStatusCode, HTTPResponseFailure> failures, HTTPResponseFailure failure) {
        this.failures = failures;
        this.failure = failure;
    }

    @Override
    public <T> HTTPResponse<T> apply(HTTPClientResponse response, JavaType javaType) {
        return HTTPStatusCode.select(response.status().code())
                .map(s -> failures.getOrDefault(s, failure).<T> apply(response, javaType))
                .orElseGet(() -> failure.apply(response, javaType));
    }
}
