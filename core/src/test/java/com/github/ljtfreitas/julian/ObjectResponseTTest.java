package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Nested;
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

    private ObjectResponseT<String> responseT = new ObjectResponseT<>();

    @Test
    void compose(@Mock ResponseFn<String, String> fn, @Mock RequestIO<String> request) throws Exception {
        Arguments arguments = Arguments.empty();

        Response<String> response = Response.done("expected");

        when(request.execute()).then(a -> Promise.done(response));

        String actual = responseT.comp(endpoint, fn).join(request, arguments);

        assertEquals("expected", actual);
    }
}