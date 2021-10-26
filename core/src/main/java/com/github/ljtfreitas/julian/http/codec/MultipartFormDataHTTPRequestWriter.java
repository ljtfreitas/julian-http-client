package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.http.MediaType;

import java.util.Collection;
import java.util.List;

public interface MultipartFormDataHTTPRequestWriter<T> extends HTTPRequestWriter<T> {

    @Override
    default Collection<MediaType> contentTypes() {
        return List.of(MediaType.MULTIPART_FORM_DATA);
    }
}
