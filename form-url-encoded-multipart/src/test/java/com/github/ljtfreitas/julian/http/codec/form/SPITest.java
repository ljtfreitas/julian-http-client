package com.github.ljtfreitas.julian.http.codec.form;

import com.github.ljtfreitas.julian.ResponseT;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.form.multipart.MapMultipartFormHTTPRequestWriter;
import com.github.ljtfreitas.julian.http.codec.form.multipart.MultipartFormObjectHTTPRequestWriter;
import com.github.ljtfreitas.julian.spi.Plugins;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class SPITest {

    @Test
    void shouldRegisterAllServices() {
        Collection<Class<? extends HTTPMessageCodec>> expected = List.of(FormObjectURLEncodedHTTPMessageCodec.class,
                SimpleMapFormURLEncodedHTTPMessageCodec.class, MultiMapFormURLEncodedHTTPMessageCodec.class,
                MultipartFormObjectHTTPRequestWriter.class, MapMultipartFormHTTPRequestWriter.class);

        Plugins plugins = new Plugins();
        Collection<? extends Class<?>> founded = plugins.all(HTTPMessageCodec.class).map(Object::getClass).collect(toList());

        assertThat(founded, containsInAnyOrder(expected.toArray()));
    }
}
