package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.QueryString;

class QueryStringTest {

	@Test
	void shouldSerializeAsQueryString() {
		Map<String, Collection<String>> parameters = new LinkedHashMap<>();
		parameters.put("param1", List.of("some-value-1", "other-value-1"));
		parameters.put("param2", List.of("some-value-2"));

		QueryString queryString = new QueryString(parameters);

		assertEquals("param1=some-value-1&param1=other-value-1&param2=some-value-2", queryString.serialize());
	}
	
	@Test
	void shouldEncodeParameterValues() {
		QueryString queryString = new QueryString(Map.of("name", List.of("John Doe")));

		assertEquals("name=John+Doe", queryString.serialize());
	}

	@Test
	void shouldBeAbleToAppendNewParameters() {
		QueryString queryString = new QueryString(Map.of("first-name", List.of("John")));

		QueryString newQueryString = queryString.append("last-name", "Doe");

		assertAll(() -> assertNotSame(queryString, newQueryString),
				  () -> assertEquals("first-name=John&last-name=Doe", newQueryString.serialize()));
	}
	
	@Test
	void shouldBeAbleToParseStringInTheQueryFormat() {
		String source = "param1=some-value&param1=other-value&param2=some-value";

		QueryString queryString = QueryString.parse(source);

		assertEquals(source, queryString.serialize());
	}

	@Test
	void shouldSerializeAsEmptyStringWhenParametersAreEmpty() {
		assertEquals("", QueryString.empty().serialize());
	}
}
