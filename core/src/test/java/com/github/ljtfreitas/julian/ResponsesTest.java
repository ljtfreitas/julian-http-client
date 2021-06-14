package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.CompletionStageResponseT;
import com.github.ljtfreitas.julian.EndpointDefinition;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.OptionalResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;

@ExtendWith(MockitoExtension.class)
class ResponsesTest {

	@SuppressWarnings("unchecked")
	@Test
    void test(@Mock EndpointDefinition endpoint, @Mock RequestIO<String> request, @Mock Response<String> response) {
		JavaType completableFutureType = JavaType.parameterized(CompletableFuture.class, JavaType.Parameterized.valueOf(Optional.class, String.class));

		when(endpoint.returnType()).thenReturn(completableFutureType);
		when(endpoint.returns(any())).thenCallRealMethod();

		when(response.body()).thenReturn("oi");

		when(request.execute()).then(a -> Promise.done(response));
		when(request.comp(any(), eq(Arguments.empty()))).thenCallRealMethod();
		when(response.map(any())).then(a -> a.getArgument(0, Function.class).apply(response));

		Responses requests = new Responses(List.of(new CompletionStageResponseT<>(), new OptionalResponseT<>()));

		ResponseFn<CompletableFuture<Optional<String>>, String> requestFn = requests.select(endpoint);

		assertEquals(JavaType.valueOf(String.class), requestFn.returnType());

		CompletableFuture<Optional<String>> result = requestFn.join(request, Arguments.empty());

		assertEquals("oi", result.join().get());
    }
}
