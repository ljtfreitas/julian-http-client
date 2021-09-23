package com.github.ljtfreitas.julian.http;

import java.util.Optional;
import java.util.function.Function;

public class ByteArrayHTTPResponseBody implements HTTPResponseBody {

    private final byte[] bodyAsBytes;

    public ByteArrayHTTPResponseBody(byte[] bodyAsBytes) {
        this.bodyAsBytes = bodyAsBytes;
    }

    @Override
    public <T> Optional<T> deserialize(Function<byte[], T> fn) {
        return Optional.ofNullable(fn.apply(bodyAsBytes));
    }
}
