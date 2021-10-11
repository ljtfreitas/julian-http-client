package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.http.MediaType;

import java.util.Collection;
import java.util.List;

public interface MultipartFormDataHTTPRequestWriter<T> extends HTTPRequestWriter<T> {

    MediaType MULTIPART_FORM_DATA_MEDIA_TYPE = MediaType.valueOf("multipart/form-data");

    @Override
    default Collection<MediaType> contentTypes() {
        return List.of(MULTIPART_FORM_DATA_MEDIA_TYPE);
    }
}
