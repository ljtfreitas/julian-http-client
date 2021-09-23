package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.http.MediaType;

import java.util.Collection;
import java.util.List;

public interface JsonHTTPMessageCodec<T> extends HTTPRequestWriter<T>, HTTPResponseReader<T> {

    MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.valueOf("application/json");

    @Override
    default Collection<MediaType> contentTypes() {
        return List.of(APPLICATION_JSON_MEDIA_TYPE);
    }
}
