package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;

import java.util.Map;

public class ConditionalHTTPResponseFailure implements HTTPResponseFailure {

    private final Map<HTTPStatusCode, HTTPResponseFailure> failures;
    private final HTTPResponseFailure fallback;

    public ConditionalHTTPResponseFailure(Map<HTTPStatusCode, HTTPResponseFailure> fallback) {
        this(fallback, DefaultHTTPResponseFailure.get());
    }

    public ConditionalHTTPResponseFailure(Map<HTTPStatusCode, HTTPResponseFailure> failures, HTTPResponseFailure fallback) {
        this.failures = failures;
        this.fallback = fallback;
    }

    @Override
    public <T> HTTPResponse<T> apply(HTTPClientResponse response, JavaType javaType) {
        return HTTPStatusCode.select(response.status().code())
                .map(s -> failures.getOrDefault(s, fallback).<T> apply(response, javaType))
                .orElseGet(() -> fallback.apply(response, javaType));
    }
}
