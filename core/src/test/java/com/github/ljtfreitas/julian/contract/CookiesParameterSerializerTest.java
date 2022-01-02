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

import com.github.ljtfreitas.julian.Cookie;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.contract.CookiesParameterSerializer;

class CookiesParameterSerializerTest {

	private final CookiesParameterSerializer serializer = new CookiesParameterSerializer();

	@ParameterizedTest
	@MethodSource("cookies")
	void shouldSerializeAsCookies(String name, JavaType javaType, Object value, Collection<Cookie> expectedHeaders) {
		Optional<Cookies> cookies = serializer.serialize(name, javaType, value);

		assertAll(() -> assertTrue(cookies.isPresent()),
				  () -> assertThat(cookies.get(), containsInAnyOrder(expectedHeaders.toArray(Cookie[]::new))));
	}

	@ParameterizedTest
	@MethodSource("restrictions")
	void shouldNotSerializeAsCookies(String name, JavaType javaType, Object value, Class<? extends Exception> expectedException) {
		assertThrows(expectedException, () -> serializer.serialize(name, javaType, value));		
	}

	static Stream<Arguments> cookies() {
		return Stream.of(arguments("sessionid", JavaType.valueOf(String.class),
									"value1",
									List.of(new Cookie("sessionid", "value1"))),
						 arguments("sessionid", JavaType.parameterized(Collection.class, String.class),
									List.of("value1", "value2"),
									List.of(new Cookie("sessionid", "value1"), new Cookie("sessionid", "value2"))),
						 arguments("cookies", JavaType.parameterized(Collection.class, Cookie.class), 
								 	List.of(new Cookie("sessionid", "value1"), new Cookie("token", "value2")), 
								 	List.of(new Cookie("sessionid", "value1"), new Cookie("token", "value2"))),
						 arguments("cookies", JavaType.valueOf(Cookies.class), 
								 	Cookies.create(new Cookie("sessionid", "value1"), new Cookie("token", "value2")), 
								 	List.of(new Cookie("sessionid", "value1"), new Cookie("token", "value2"))),
						 arguments("cookies", JavaType.genericArrayOf(Cookie.class),
								 	new Cookie[] { new Cookie("sessionid", "value1"), new Cookie("token", "value2") }, 
								 	List.of(new Cookie("sessionid", "value1"), new Cookie("token", "value2"))),
						 arguments("sessionid", JavaType.valueOf(String[].class),
									new String[] { "value1", "value2" },
									List.of(new Cookie("sessionid", "value1"), new Cookie("sessionid", "value2"))),
						 arguments("cookies", JavaType.parameterized(Map.class, String.class, String.class),
								    Map.of("sessionid", "value1", "token", "value2"),
								 	List.of(new Cookie("sessionid", "value1"), new Cookie("token", "value2"))));
	}

	static Stream<Arguments> restrictions() {
		return Stream.of(arguments("cookies", JavaType.valueOf(Map.class),
									Map.of("sessionid", "value1", "token", "value2"),
								 	IllegalStateException.class));
	}

}
