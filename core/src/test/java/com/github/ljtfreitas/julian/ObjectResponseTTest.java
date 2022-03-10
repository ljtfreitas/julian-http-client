package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ObjectResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final ObjectResponseT<Object> responseT = new ObjectResponseT<>();

    @Test
    void compose(@Mock ResponseFn<String, Object> fn) {
        Arguments arguments = Arguments.empty();

        Response<String> response = Response.done("expected");

        Object actual = responseT.bind(endpoint, fn).join(Promise.done(response), arguments);

        assertEquals("expected", actual);
    }
}