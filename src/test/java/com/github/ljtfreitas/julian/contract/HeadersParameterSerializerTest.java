package com.github.ljtfreitas.julian.contract;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.contract.HeadersParameterSerializer;

class HeadersParameterSerializerTest {

	private HeadersParameterSerializer serializer = new HeadersParameterSerializer();

	@ParameterizedTest
	@MethodSource("headers")
	void shouldSerializeAsHeaders(String name, JavaType javaType, Object value, Collection<Header> expectedHeaders) {
		Optional<Headers> headers = serializer.serialize(name, javaType, value);

		assertAll(() -> assertTrue(headers.isPresent()),
				  () -> assertThat(headers.get(), containsInAnyOrder(expectedHeaders.toArray(Header[]::new))));
	}

	@ParameterizedTest
	@MethodSource("restrictions")
	void shouldNotSerializeAsHeaders(String name, JavaType javaType, Object value, Class<? extends Exception> expectedException) {
		assertThrows(expectedException, () -> serializer.serialize(name, javaType, value));		
	}

	static Stream<Arguments> headers() {
		return Stream.of(arguments("X-Header-Name", JavaType.valueOf(String.class), 
									"value1", 
									List.of(new Header("X-Header-Name", "value1"))),
						 arguments("X-Header-Name", JavaType.parameterized(Collection.class, String.class),
								 	List.of("value1", "value2"), 
								 	List.of(new Header("X-Header-Name", List.of("value1", "value2")))),
						 arguments("headers", JavaType.parameterized(Collection.class, Header.class), 
								 	List.of(new Header("X-Header-1", "value1"), new Header("X-Header-2", "value2")), 
								 	List.of(new Header("X-Header-1", "value1"), new Header("X-Header-2", "value2"))),
						 arguments("X-Header-Name", JavaType.valueOf(Collection.class), 
								 	List.of("value1", "value2"), 
								 	List.of(new Header("X-Header-Name", List.of("value1", "value2")))),
						 arguments("headers", JavaType.valueOf(Headers.class), 
								 	Headers.create(new Header("X-Header-1", "value1"), new Header("X-Header-2", "value2")), 
								 	List.of(new Header("X-Header-1", "value1"), new Header("X-Header-2", "value2"))),
						 arguments("X-Header-Name", JavaType.genericArrayOf(String.class),
								 	new String[] { "value1", "value2" }, 
								 	List.of(new Header("X-Header-Name", "value1", "value2"))),
						 arguments("headers", JavaType.genericArrayOf(Header.class),
								 	new Header[] { new Header("X-Header-1", "value1"), new Header("X-Header-2", "value2") }, 
								 	List.of(new Header("X-Header-1", "value1"), new Header("X-Header-2", "value2"))),
						 arguments("headers", JavaType.parameterized(Map.class, String.class, String.class),
								    Map.of("X-Header-1", "value1", "X-Header-2", "value2"),
								 	List.of(new Header("X-Header-1", "value1"), new Header("X-Header-2", "value2"))));
	}

	static Stream<Arguments> restrictions() {
		return Stream.of(arguments("headers", JavaType.valueOf(Map.class),
								    Map.of("X-Header-1", "value1", "X-Header-2", "value2"),
								 	IllegalStateException.class));
	}
}
