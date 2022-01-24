package com.github.ljtfreitas.julian;

import com.github.ljtfreitas.julian.Endpoint.Parameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import com.github.ljtfreitas.julian.Endpoint.Path;
import com.github.ljtfreitas.julian.contract.CookiesParameterSerializer;
import com.github.ljtfreitas.julian.contract.HeadersParameterSerializer;
import com.github.ljtfreitas.julian.contract.ParameterSerializer;
import com.github.ljtfreitas.julian.contract.QueryParameterSerializer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EndpointTest {

	@Nested
	class EndpointToRequest {

		@Nested
		class StaticDefinitions {

			@ParameterizedTest(name = "Endpoint: {0}")
			@ArgumentsSource(StaticEndpointProvider.class)
			void staticDefinitions(Endpoint endpoint) {
				RequestDefinition request = endpoint.request(Arguments.empty(), endpoint.returnType());

				Header[] expectedHeaders = endpoint.headers().merge(endpoint.cookies().header().stream())
						.all().toArray(Header[]::new);

				assertAll(() -> assertThat(request.path().toString(), equalTo(endpoint.path().show())),
						() -> assertThat(request.method(), equalTo(endpoint.method())),
						() -> assertThat(request.returnType(), equalTo(endpoint.returnType())),
						() -> assertThat(request.headers(), hasItems(expectedHeaders)));

			}

			@Nested
			class HasQueryParameters {

				@Test
				void definedOnPath() {
					Endpoint endpoint = new Endpoint(new Path("http://my.api.com",
							QueryParameters.create(Map.of("param1", "value1", "param2", "value2"))), "GET");

					RequestDefinition definition = endpoint.request(Arguments.empty(), endpoint.returnType());

					assertThat(definition.path(), equalTo(URI.create("http://my.api.com?param1=value1&param2=value2")));
				}
			}
		}

		@Nested
		class DynamicDefinitions {

			@Nested
			class Paths {

				@ParameterizedTest(name = "Expected request: [{2}], using this endpoint: [{0}] and these arguments: [{1}]")
				@ArgumentsSource(DynamicEndpointPathsProvider.class)
				void dynamicDefinitions(Endpoint endpoint, Arguments arguments, RequestDefinition expected) {
					RequestDefinition request = endpoint.request(arguments, endpoint.returnType());

					assertAll(() -> assertThat(request.path().toString(), equalTo(expected.path().toString())),
							() -> assertThat(request.method(), equalTo(expected.method())),
							() -> assertThat(request.returnType(), equalTo(expected.returnType())));
				}

				@Test
				void rejectWrongDynamicParameters() {
					Parameters parameters = Parameters.create(Parameter.path(0, "arg1", JavaType.valueOf(String.class), null));
					assertThrows(IllegalArgumentException.class, () -> new Path("http://my.api.com/{arg}", parameters));
				}

				@Test
				void rejectDynamicParametersWithNullValues() {
					Parameters parameters = Parameters.create(Parameter.path(0, "arg", JavaType.valueOf(String.class), null));

					Except<URI> expanded = new Path("http://my.api.com/{arg}", parameters).expand(Arguments.create(new Object[]{null}));

					expanded.onSuccess(value -> fail("a failure is expected here..."))
							.onFailure(e -> assertThat(e, isA(IllegalArgumentException.class)));
				}

				@Nested
				class HasQueryParameters {

					@ParameterizedTest(name = "Expected path: [{2}], using these parameters: [{0}] and these arguments: [{1}]")
					@ArgumentsSource(DynamicEndpointQueryParametersProvider.class)
					void dynamicDefinitions(Parameters parameters, Arguments arguments, String expected) {
						Endpoint endpoint = new Endpoint(new Path("http://my.api.com", parameters), "GET", parameters);

						RequestDefinition request = endpoint.request(arguments, endpoint.returnType());

						assertThat(request.path().toString(), equalTo(expected));
					}
				}
			}

			@Nested
			class HasHeaders {

				@ParameterizedTest(name = "Expected headers: [{2}], using these parameters: [{0}] and these arguments: [{1}]")
				@ArgumentsSource(DynamicEndpointHeadersProvider.class)
				void dynamicDefinitions(Parameters parameters, Arguments arguments, Headers expected) {
					Endpoint endpoint = new Endpoint(new Path("http://my.api.com"), "GET", parameters);

					RequestDefinition request = endpoint.request(arguments, endpoint.returnType());

					Header[] expectedHeaders = expected.all().toArray(Header[]::new);

					assertThat(request.headers(), hasItems(expectedHeaders));
				}
			}

			@Nested
			class HasCookies {

				@ParameterizedTest(name = "Expected headers: [{2}], using these parameters: [{0}] and these arguments: [{1}]")
				@ArgumentsSource(DynamicEndpointCookiesProvider.class)
				void dynamicDefinitions(Parameters parameters, Arguments arguments, Headers expected) {
					Endpoint endpoint = new Endpoint(new Path("http://my.api.com"), "GET", parameters);

					RequestDefinition request = endpoint.request(arguments, endpoint.returnType());

					Header[] expectedHeaders = expected.all().toArray(Header[]::new);

					assertThat(request.headers(), hasItems(expectedHeaders));
				}
			}

			@Nested
			class HasBody {

				@ParameterizedTest(name = "Expected body: [{1}], using this parameter: [{0}]")
				@ArgumentsSource(DynamicEndpointBodyProvider.class)
				void dynamicDefinitions(Endpoint.BodyParameter bodyParameter, Arguments arguments) {
					Endpoint endpoint = new Endpoint(new Path("http://my.api.com"), "POST", Parameters.create(bodyParameter));

					RequestDefinition request = endpoint.request(arguments, endpoint.returnType());

					assertThat(request.body().isPresent(), is(true));

					Object expectedContent = arguments.of(0).orElse(null);
					JavaType expectedBodyJavaType = bodyParameter.javaType();

					Consumer<RequestDefinition.Body> assertions = body ->
							assertAll(() -> assertThat(body.content(), equalTo(expectedContent)),
									  () -> assertThat(body.javaType(), equalTo(expectedBodyJavaType)),
									  () -> bodyParameter.contentType().ifPresent(c -> assertThat(request.headers(), hasItems(new Header("Content-Type", c)))));

					request.body().ifPresentOrElse(assertions, () -> fail("a request body was expected here..."));
				}
			}
		}

		@Nested
		class Mixing {

			@Test
			void queryParameters() {
				ParameterSerializer<? super Object, QueryParameters> queryParameterSerializer = new QueryParameterSerializer();

				Parameters parameters = Parameters.create(Parameter.query(0, "param3", JavaType.valueOf(String.class), queryParameterSerializer));

				QueryParameters queryParameters = QueryParameters.create(Map.of("param2", "value2"));

				Endpoint endpoint = new Endpoint(new Path("http://my.api.com?param1=value1", queryParameters, parameters), "GET", parameters);

				RequestDefinition request = endpoint.request(Arguments.create("value3"), endpoint.returnType());

				QueryParameters actual = QueryParameters.parse(request.path().getQuery());

				assertThat(actual.all(), allOf(hasEntry("param1", List.of("value1")),
											   hasEntry("param2", List.of("value2")),
											   hasEntry("param3", List.of("value3"))));
			}

			@Test
			void headers() {
				ParameterSerializer<? super Object, Headers> headersParameterSerializer = new HeadersParameterSerializer();

				Headers headers = Headers.create(new Header("x-my-header-1", "value1"));

				Endpoint endpoint = new Endpoint(new Path("http://my.api.com"), "GET", headers,
						Parameters.create(Parameter.header(0, "x-my-header-2", JavaType.valueOf(String.class), headersParameterSerializer)));

				RequestDefinition request = endpoint.request(Arguments.create("value2"), endpoint.returnType());

				Header[] expectedHeaders = headers.join(new Header("x-my-header-2", "value2")).all()
						.toArray(Header[]::new);

				assertThat(request.headers(), contains(expectedHeaders));
			}

			@Test
			void cookies() {
				CookiesParameterSerializer cookiesParameterSerializer = new CookiesParameterSerializer();

				Cookies cookies = Cookies.create(new Cookie("my-cookie", "value"));

				Endpoint endpoint = new Endpoint(new Path("http://my.api.com"), "GET",
						Headers.empty(), cookies,
						Parameters.create(Parameter.cookie(0, "another-cookie", JavaType.valueOf(String.class), cookiesParameterSerializer)));

				RequestDefinition request = endpoint.request(Arguments.create("another-cookie-value"), endpoint.returnType());

				Header expectedCookieHeader = cookies.join(new Cookie("another-cookie", "another-cookie-value"))
								.header().get();

				assertThat(request.headers(), contains(expectedCookieHeader));
			}
		}
	}

	static class StaticEndpointProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			return Stream.of(
					arguments(new Endpoint(new Path("http://my.api.com"), "GET")),
					arguments(new Endpoint(new Path("http://my.api.com"), "POST")),
					arguments(new Endpoint(new Path("http://my.api.com"), "PUT")),
					arguments(new Endpoint(new Path("http://my.api.com"), "DELETE")),
					arguments(new Endpoint(new Path("http://my.api.com"), "GET", JavaType.valueOf(String.class))),
					arguments(new Endpoint(new Path("http://my.api.com"), "GET", Headers.create(new Header("my-header", "whatever")))),
					arguments(new Endpoint(new Path("http://my.api.com"), "GET", Headers.empty(), Cookies.create(new Cookie("my-cookie", "whatever")))),
					arguments(new Endpoint(new Path("http://my.api.com"), "GET", Headers.create(new Header("my-header", "whatever")), Cookies.create(new Cookie("my-cookie", "whatever")))),
					arguments(new Endpoint(new Path("http://my.api.com?param=value"), "GET")));
		}
	}

	static class DynamicEndpointPathsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			ParameterSerializer<Object, String> pathParameterSerializer = ParameterSerializer.simple();

			return Stream.of(
					// path parameters => the simpler case (string arguments)
					arguments(new Endpoint(new Path("http://my.api.com/{arg1}/{arg2}",
									Parameters.create(Parameter.path(0, "arg1", JavaType.valueOf(String.class), pathParameterSerializer),
													  Parameter.path(1, "arg2", JavaType.valueOf(String.class), pathParameterSerializer))), "GET"),
							Arguments.create("value1", "value2"),
							new RequestDefinition(URI.create("http://my.api.com/value1/value2"), "GET")));
		}
	}

	static class DynamicEndpointQueryParametersProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			ParameterSerializer<Object, QueryParameters> queryParameterSerializer = new QueryParameterSerializer();

			return Stream.of(
					// query parameters => the simpler case, using String arguments
					arguments(Parameters.create(Parameter.query(0, "param1", JavaType.valueOf(String.class), queryParameterSerializer),
												Parameter.query(1, "param2", JavaType.valueOf(String.class), queryParameterSerializer)),
							Arguments.create("value1", "value2"),
							"http://my.api.com?param1=value1&param2=value2"),

					// query parameters => a list of Strings as argument
					arguments(Parameters.create(Parameter.query(0, "params", JavaType.parameterized(Collection.class, String.class), queryParameterSerializer)),
							Arguments.create(List.of("value1", "value2")),
							"http://my.api.com?params=value1&params=value2"),

					// query parameters => a QueryParameters object as argument
					arguments(Parameters.create(Parameter.query(0, "queryString", JavaType.valueOf(QueryParameters.class), queryParameterSerializer)),
							Arguments.create(QueryParameters.create(Map.of("param1", "value1", "param2", "value2"))),
							"http://my.api.com?param1=value1&param2=value2"),

					// query parameters => a Map of values as argument
					arguments(Parameters.create(Parameter.query(0, "params", JavaType.parameterized(Map.class, String.class, JavaType.Parameterized.valueOf(Collection.class, String.class)), queryParameterSerializer)),
							Arguments.create(Map.of("params", List.of("value1", "value2"))),
							"http://my.api.com?params=value1&params=value2"),

					// query parameters => an array of Strings as argument
					arguments(Parameters.create(Parameter.query(0, "params", JavaType.valueOf(String[].class), queryParameterSerializer)),
							Arguments.create(new Object[] { new String[] { "value1", "value2" } }),
							"http://my.api.com?params=value1&params=value2"));
		}
	}

	static class DynamicEndpointHeadersProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			HeadersParameterSerializer headersParameterSerializer = new HeadersParameterSerializer();

			return Stream.of(
					// header parameters => the simpler case (string arguments)
					arguments(Parameters.create(Parameter.header(0, "x-my-header-1", JavaType.valueOf(String.class), headersParameterSerializer),
											    Parameter.header(1, "x-my-header-2", JavaType.valueOf(String.class), headersParameterSerializer)),
							Arguments.create("value1", "value2"),
							Headers.create(new Header("x-my-header-1", "value1"),
										   new Header("x-my-header-2", "value2"))),

					// header parameters => a list of Strings as argument
					arguments(Parameters.create(Parameter.header(0, "x-my-header", JavaType.parameterized(Collection.class, String.class), headersParameterSerializer)),
							Arguments.create(List.of("value1", "value2")),
							Headers.create(new Header("x-my-header", "value1", "value2"))),

					// header parameters => a Headers object as argument
					arguments(Parameters.create(Parameter.header(0, "headers", JavaType.valueOf(Headers.class), headersParameterSerializer)),
							Arguments.create(Headers.create(new Header("x-my-header-1", "value1"), new Header("x-my-header-2", "value2"))),
							Headers.create(new Header("x-my-header-1", "value1"),
										   new Header("x-my-header-2", "value2"))),

					// header parameters => a Map of values as argument
					arguments(Parameters.create(Parameter.header(0, "headers", JavaType.parameterized(Map.class, String.class, String.class), headersParameterSerializer)),
							Arguments.create(Map.of("x-my-header", "value1")),
							Headers.create(new Header("x-my-header", "value1"))),

					// header parameters => an array of Strings as argument
					arguments(Parameters.create(Parameter.header(0, "x-my-header", JavaType.valueOf(String[].class), headersParameterSerializer)),
							Arguments.create(new Object[] { new String[] { "value1", "value2" } }),
							Headers.create(new Header("x-my-header", "value1", "value2"))));
		}
	}

	static class DynamicEndpointCookiesProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			CookiesParameterSerializer cookiesParameterSerializer = new CookiesParameterSerializer();

			return Stream.of(
					// cookies parameters => the simpler case (string arguments)
					arguments(Parameters.create(Parameter.cookie(0, "my-cookie", JavaType.valueOf(String.class), cookiesParameterSerializer)),
							Arguments.create("value"),
							Headers.create(new Cookie("my-cookie", "value").header())),

					// cookies parameters => a list of Strings as argument
					arguments(Parameters.create(Parameter.cookie(0, "my-cookie", JavaType.parameterized(Collection.class, String.class), cookiesParameterSerializer)),
							Arguments.create(List.of("value1", "value2")),
							Headers.create(Cookies.create(new Cookie("my-cookie", "value1"), new Cookie("my-cookie", "value2")).header().get())),

					// cookies parameters => a Cookies object as argument
					arguments(Parameters.create(Parameter.cookie(0, "cookies", JavaType.valueOf(Cookies.class), cookiesParameterSerializer)),
							Arguments.create(Cookies.create(new Cookie("my-cookie", "value"))),
							Headers.create(new Cookie("my-cookie", "value").header())),

					// cookies parameters => a Map of values as argument
					arguments(Parameters.create(Parameter.cookie(0, "cookies", JavaType.parameterized(Map.class, String.class, String.class), cookiesParameterSerializer)),
							Arguments.create(Map.of("my-cookie", "value")),
							Headers.create(new Cookie("my-cookie", "value").header())),

					// cookies parameters => an array of Strings as argument
					arguments(Parameters.create(Parameter.cookie(0, "my-cookie", JavaType.valueOf(String[].class), cookiesParameterSerializer)),
							Arguments.create(new Object[] { new String[] { "value1", "value2" } }),
							Headers.create(Cookies.create(new Cookie("my-cookie", "value1"), new Cookie("my-cookie", "value2")).header().get())));
		}
	}

	static class DynamicEndpointBodyProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context) {
			CookiesParameterSerializer bodyParameterSerializer = new CookiesParameterSerializer();

			return Stream.of(
					// body parameter => the simpler case (an object value as argument)
					arguments(Parameter.body(0, "body", JavaType.valueOf(String.class)),
							Arguments.create("hello")),

					// body parameter => using a defined content-type
					arguments(Parameter.body(0, "body", JavaType.valueOf(String.class), "text/plain"),
							Arguments.create("hello")),

					// body parameter => using a more complex java type
					arguments(Parameter.body(0, "body", JavaType.parameterized(Collection.class, String.class)),
							Arguments.create(List.of("value"))));
		}
	}
}
