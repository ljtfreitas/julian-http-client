package com.github.ljtfreitas.julian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.EndpointDefinition.Parameter;
import com.github.ljtfreitas.julian.EndpointDefinition.Parameters;
import com.github.ljtfreitas.julian.EndpointDefinition.Path;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.QueryString;
import com.github.ljtfreitas.julian.contract.ParameterSerializer;
import com.github.ljtfreitas.julian.contract.QueryStringSerializer;

class EndpointDefinitionTest {

	@Nested
	class Paths {

		private ParameterSerializer<Object, String> parameterSerializer = ParameterSerializer.create();

		@Test
		void simple() {
			Path path = new Path("http://my.api.com");

			URI uri = path.expand(Arguments.empty()).unsafe();

			assertEquals("http://my.api.com", uri.toString());
		}

		@Test
		void withDynamicParameters() {
			Parameters parameters = Parameters.create(Parameter.path(0, "arg1", JavaType.valueOf(String.class), parameterSerializer), 
												 	  Parameter.path(1, "arg2", JavaType.valueOf(String.class), parameterSerializer));

			Path path = new Path("http://my.api.com/{arg1}/{arg2}", parameters);

			URI uri = path.expand(Arguments.create("whatever", "i-dont-care")).unsafe();

			assertEquals("http://my.api.com/whatever/i-dont-care", uri.toString());
		}
		
		@Test
		void rejectWrongDynamicParameters() {
			Parameters parameters = Parameters.create(Parameter.path(0, "arg1", JavaType.valueOf(String.class), parameterSerializer));
			assertThrows(IllegalArgumentException.class, () -> new Path("http://my.api.com/{arg}", parameters));
		}

		@Nested
		class WithQueryString {

			@Test
			void onPath() {
				Path path = new Path("http://my.api.com?param=value");
	
				URI uri = path.expand(Arguments.empty()).unsafe();

				assertEquals("http://my.api.com?param=value", uri.toString());
			}

			@Test
			void defindOnEndpoint() throws URISyntaxException {
				QueryString queryString = QueryString.create(Map.of("param1", "value1", "param2", "value2"));

				Path path = new Path("http://my.api.com", queryString, Parameters.empty());

				URI uri = path.expand(Arguments.empty()).unsafe();

				assertEquals("http://my.api.com?param1=value1&param2=value2", uri.toString());
			}

			@Nested
			class WithDynamicParameters {

				private QueryStringSerializer queryStringSerializer = new QueryStringSerializer();

				@Test
				void simple() {	
					Parameters parameters = Parameters.create(Parameter.query(0, "param1", JavaType.valueOf(String.class), queryStringSerializer),
															  Parameter.query(1, "param2", JavaType.valueOf(String.class), queryStringSerializer));
	
					Path path = new Path("http://my.api.com", parameters);
		
					URI uri = path.expand(Arguments.create("value1", "value2")).unsafe();
		
					assertEquals("http://my.api.com?param1=value1&param2=value2", uri.toString());
				}

				@Test
				void collection() {
					Collection<String> values = List.of("value1", "value2");

					Parameters parameters = Parameters.create(Parameter.query(0, "params", JavaType.parameterized(Collection.class, String.class), queryStringSerializer));
	
					Path path = new Path("http://my.api.com", parameters);
		
					URI uri = path.expand(Arguments.create(values)).unsafe();
		
					assertEquals("http://my.api.com?params=value1&params=value2", uri.toString());
				}

				@Test
				void queryString() throws URISyntaxException {
					QueryString queryString = QueryString.create(Map.of("param1", "value1", "param2", "value2"));

					Parameters parameters = Parameters.create(Parameter.query(0, "queryString", JavaType.valueOf(QueryString.class), queryStringSerializer));
	
					Path path = new Path("http://my.api.com", parameters);

					URI uri = path.expand(Arguments.create(queryString)).unsafe();

					assertEquals("http://my.api.com?param1=value1&param2=value2", uri.toString());
				}

				@Test
				void mapOfCollection() throws URISyntaxException {
					Map<String, Collection<String>> values = Map.of("params", List.of("value1", "value2"));

					Parameters parameters = Parameters.create(Parameter.query(0, "params", JavaType.parameterized(Map.class, String.class, JavaType.Parameterized.valueOf(Collection.class, String.class)), queryStringSerializer));
	
					Path path = new Path("http://my.api.com", parameters);

					URI uri = path.expand(Arguments.create(values)).unsafe();

					assertEquals("http://my.api.com?params=value1&params=value2", uri.toString());
				}

				@Test
				void mapOfString() throws URISyntaxException {
					Map<String, String> values = Map.of("param1", "value1", "param2", "value2");

					Parameters parameters = Parameters.create(Parameter.query(0, "params", JavaType.parameterized(Map.class, String.class, String.class), queryStringSerializer));

					Path path = new Path("http://my.api.com", parameters);
		
					URI uri = path.expand(Arguments.create(values)).unsafe();

					assertEquals("http://my.api.com?param1=value1&param2=value2", uri.toString());
				}

				@Test
				void array() throws URISyntaxException {
					String[] values = new String[] { "value1", "value2" };

					Parameters parameters = Parameters.create(Parameter.query(0, "params", JavaType.valueOf(String[].class), queryStringSerializer));

					Path path = new Path("http://my.api.com", parameters);

					URI uri = path.expand(Arguments.create(new Object[] { values })).unsafe();

					assertEquals("http://my.api.com?params=value1&params=value2", uri.toString());
				}
			}
		}
	}
}
