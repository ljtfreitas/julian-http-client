package com.github.ljtfreitas.julian.contract;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.QueryString;
import com.github.ljtfreitas.julian.contract.QueryStringSerializer;

class QueryStringSerializerTest {

	private QueryStringSerializer serializer = new QueryStringSerializer();

	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@MethodSource("queryParameters")
	void shouldSerializeAsQueryString(String name, JavaType javaType, Object value, Collection<String> expectedParameters) {
		Optional<QueryString> q = serializer.serialize(name, javaType, value);

		Matcher<String> matches = Matchers.allOf(expectedParameters.stream().map(Matchers::containsString).toArray(Matcher[]::new));
		
		assertAll(() -> assertTrue(q.isPresent()),
				  () -> assertThat(q.get().serialize(), matches));
	}

	@ParameterizedTest
	@MethodSource("restrictions")
	void shouldNotSerializeAsQueryString(String name, JavaType javaType, Object value, Class<? extends Exception> expectedException) {
		assertThrows(expectedException, () -> serializer.serialize(name, javaType, value));		
	}
	
	static Stream<Arguments> queryParameters() {
		return Stream.of(arguments("name", JavaType.valueOf(String.class), "JohnDoe", List.of("name=JohnDoe")),
						 arguments("name", JavaType.parameterized(Collection.class, String.class), List.of("John", "Doe"), List.of("name=John", "name=Doe")),
						 arguments("name", JavaType.valueOf(Collection.class), List.of("John", "Doe"), List.of("name=John", "name=Doe")),
						 arguments("name", JavaType.valueOf(QueryString.class), QueryString.create("name", "JohnDoe"), List.of("name=JohnDoe")),
						 arguments("name", JavaType.valueOf(String[].class), new String[] { "John", "Doe" }, List.of("name=John", "name=Doe")),
						 arguments("name", JavaType.genericArrayOf(String[].class), new String[] { "John", "Doe" }, List.of("name=John", "name=Doe")),
						 arguments("name", JavaType.parameterized(Map.class, String.class, String.class), Map.of("name", "JohnDoe"), List.of("name=JohnDoe")),
						 arguments("name", JavaType.parameterized(Map.class, String.class, Collection.class), Map.of("name", List.of("John", "Doe")), List.of("name=John", "name=Doe")));
	}

	static Stream<Arguments> restrictions() {
		return Stream.of(arguments("headers", JavaType.valueOf(Map.class), Map.of("name", "whatever"), IllegalStateException.class));
	}

}
