package com.github.ljtfreitas.julian.reactor;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.ResponseT;
import com.github.ljtfreitas.julian.spi.Plugins;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class SPITest {

    @SuppressWarnings("rawtypes")
    @Test
    void shouldRegisterAllServices() {
        Collection<Class<? extends ResponseT>> expected = List.of(MonoResponseT.class, FluxResponseT.class);

        Plugins plugins = new Plugins();
        Collection<? extends Class<?>> founded = plugins.all(ResponseT.class).map(Object::getClass).collect(toList());

        assertThat(founded, containsInAnyOrder(expected.toArray()));
    }
}
