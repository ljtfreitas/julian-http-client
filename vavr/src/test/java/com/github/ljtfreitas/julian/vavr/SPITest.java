package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.ResponseT;
import com.github.ljtfreitas.julian.spi.Plugins;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class SPITest {

    @SuppressWarnings({"rawtypes"})
    @Test
    void shouldRegisterAllServices() {
        Collection<Class<? extends ResponseT>> expected = List.of(ArrayResponseT.class, EitherHTTPResponseT.class,
                EitherResponseT.class, FutureResponseT.class, IndexedSeqResponseT.class, LazyResponseT.class,
                LinearSeqResponseT.class, ListResponseT.class, OptionResponseT.class, QueueResponseT.class,
                SeqResponseT.class, SetResponseT.class, TraversableResponseT.class, TryResponseT.class, VectorResponseT.class);

        Plugins plugins = new Plugins();
        Collection<? extends Class<?>> founded = plugins.all(ResponseT.class).map(Object::getClass).collect(toList());

        assertThat(founded, containsInAnyOrder(expected.toArray()));
    }
}
