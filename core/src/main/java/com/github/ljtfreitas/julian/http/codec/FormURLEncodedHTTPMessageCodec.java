package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.http.MediaType;

import java.util.Collection;
import java.util.List;

public interface FormURLEncodedHTTPMessageCodec<T> extends HTTPRequestWriter<T>, HTTPResponseReader<T> {

    MediaType FORM_URL_ENCODED_MEDIA_TYPE = MediaType.valueOf("application/x-www-form-urlencoded");

    @Override
    default Collection<MediaType> contentTypes() {
        return List.of(FORM_URL_ENCODED_MEDIA_TYPE);
    }
}
