package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResponsesTest {

	@Test
    void test(@Mock Endpoint endpoint, @Mock RequestIO<String> request) {
		JavaType completableFutureType = JavaType.parameterized(CompletableFuture.class, JavaType.Parameterized.valueOf(Optional.class, String.class));

		when(endpoint.returnType()).thenReturn(completableFutureType);
		when(endpoint.returns(any())).thenCallRealMethod();

		Response<String, Exception> response = Response.done("oi");

		Responses responses = new Responses(List.of(new CompletionStageResponseT<>(), new OptionalResponseT<>()));

		ResponseFn<String, CompletableFuture<Optional<String>>> responseFn = responses.select(endpoint);

		assertEquals(JavaType.valueOf(String.class), responseFn.returnType());

		CompletableFuture<Optional<String>> result = responseFn.join(Promise.done(response), Arguments.empty());

		assertEquals("oi", result.join().get());
    }
}
