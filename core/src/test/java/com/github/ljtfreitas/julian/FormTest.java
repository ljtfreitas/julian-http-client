package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FormTest {

	@Test
	void shouldSerializeAsQueryString() {
		Map<String, Collection<String>> parameters = new LinkedHashMap<>();
		parameters.put("param1", List.of("some-value-1", "other-value-1"));
		parameters.put("param2", List.of("some-value-2"));

		Form form = new Form(parameters);

		assertEquals("param1=some-value-1&param1=other-value-1&param2=some-value-2", form.serialize());
	}

	@Test
	void shouldEncodeParameterValues() {
		Form form = new Form(Map.of("name", List.of("John Doe")));

		assertEquals("name=John+Doe", form.serialize());
	}

	@Test
	void shouldBeAbleToAppendNewParameters() {
		Form form = new Form(Map.of("first-name", List.of("John")));

		Form newForm = form.join("last-name", "Doe");

		assertAll(() -> assertNotSame(form, newForm),
				  () -> assertEquals("first-name=John&last-name=Doe", newForm.serialize()));
	}

	@Test
	void shouldBeAbleToParseStringInTheQueryFormat() {
		String source = "param1=some-value&param1=other-value&param2=some-value";

		Form form = Form.parse(source);

		assertEquals(source, form.serialize());
	}

	@Test
	void shouldSerializeAsEmptyStringWhenParametersAreEmpty() {
		assertEquals("", Form.empty().serialize());
	}
}
