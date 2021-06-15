package com.github.ljtfreitas.julian.http.codec;

import java.util.Collection;
import java.util.List;

public interface JsonHTTPMessageCodec<T> extends HTTPRequestWriter<T>, HTTPResponseReader<T> {

    ContentType APPLICATION_JSON_CONTENT_TYPE = ContentType.valueOf("application/json");

    @Override
    default Collection<ContentType> contentTypes() {
        return List.of(APPLICATION_JSON_CONTENT_TYPE);
    }
}
