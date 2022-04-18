package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BracketTest {

    @Mock(answer = CALLS_REAL_METHODS)
    private MyResource resource;

    @Test
    void shouldCloseTheOpenedResource() throws Exception {
        String output = Bracket.acquire(() -> resource).map(MyResource::doSomething).unsafe();

        assertEquals("whatever", output);

        verify(resource).close();
    }

    @Test
    void shouldComposeWithAnotherCloseableResource() throws Exception {
        MyResource other = mock(MyResource.class, CALLS_REAL_METHODS);

        String output = Bracket.acquire(() -> resource).and(r -> other).map(MyResource::doSomething).unsafe();

        assertEquals("whatever", output);

        verify(resource).close();
        verify(other).close();
    }

    @Test
    void shouldCloseTheOpenedResourceEvenWhenAnExceptionIsThrowed() throws Exception {
        MyResource resource = mock(MyResource.class, CALLS_REAL_METHODS);

        RuntimeException expected = new RuntimeException("oops");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> Bracket.acquire(() -> resource).<String>map(r -> { throw expected; }).unsafe());

        assertSame(expected, exception);

        verify(resource).close();
    }

    private interface MyResource extends AutoCloseable {

        default String doSomething() { return "whatever"; }

        void destroy();
    }
}