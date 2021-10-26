package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class QueryParametersTest {

	@Test
	void shouldSerializeAsQueryString() {
		Map<String, Collection<String>> parameters = new LinkedHashMap<>();
		parameters.put("param1", List.of("some-value-1", "other-value-1"));
		parameters.put("param2", List.of("some-value-2"));

		QueryParameters queryParameters = new QueryParameters(parameters);

		assertEquals("param1=some-value-1&param1=other-value-1&param2=some-value-2", queryParameters.serialize());
	}
	
	@Test
	void shouldEncodeParameterValues() {
		QueryParameters queryParameters = new QueryParameters(Map.of("name", List.of("John Doe")));

		assertEquals("name=John+Doe", queryParameters.serialize());
	}

	@Test
	void shouldBeAbleToAppendNewParameters() {
		QueryParameters queryParameters = new QueryParameters(Map.of("first-name", List.of("John")));

		QueryParameters newQueryParameters = queryParameters.append("last-name", "Doe");

		assertAll(() -> assertNotSame(queryParameters, newQueryParameters),
				  () -> assertEquals("first-name=John&last-name=Doe", newQueryParameters.serialize()));
	}
	
	@Test
	void shouldBeAbleToParseStringInTheQueryFormat() {
		String source = "param1=some-value&param1=other-value&param2=some-value";

		QueryParameters queryParameters = QueryParameters.parse(source);

		assertEquals(source, queryParameters.serialize());
	}

	@Test
	void shouldSerializeAsEmptyStringWhenParametersAreEmpty() {
		assertEquals("", QueryParameters.empty().serialize());
	}
}
