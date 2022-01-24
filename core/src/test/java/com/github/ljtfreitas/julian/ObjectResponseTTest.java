package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final ObjectResponseT<String> responseT = new ObjectResponseT<>();

    @Test
    void compose(@Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        Response<String, Exception> response = Response.done("expected");

        String actual = responseT.bind(endpoint, fn).join(Promise.done(response), arguments);

        assertEquals("expected", actual);
    }
}