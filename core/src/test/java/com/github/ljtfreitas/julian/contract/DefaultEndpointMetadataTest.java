package com.github.ljtfreitas.julian.contract;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Cookies;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.Endpoint.BodyParameter;
import com.github.ljtfreitas.julian.Endpoint.Parameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.JavaType.Wildcard;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_JSON_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DefaultEndpointMetadataTest {

	private final EndpointMetadata endpointMetadata = new DefaultEndpointMetadata();

	@Nested
	class HTTPMethods {

		@ParameterizedTest(name = "HTTP Method: {0}")
		@ArgumentsSource(HTTPMethodProvider.class)
		void httpMethods(String httpMethod, URI path, Method javaMethod) {
			Endpoint endpoint = endpointMetadata.endpoint(MyType.class, javaMethod);

			assertAll(() -> assertNotNull(endpoint),
					() -> assertEquals(path, endpoint.path().expand().unsafe()),
					() -> assertEquals(httpMethod, endpoint.method()),
					() -> assertThat(endpoint.headers(), emptyIterable()),
					() -> assertThat(endpoint.cookies(), emptyIterable()),
					() -> assertThat(endpoint.parameters(), emptyIterable()),
					() -> assertEquals(JavaType.valueOf(void.class), endpoint.returnType()));
		}
	}

	@Nested
	class ParameterDefinitions {

		@ParameterizedTest(name = "@ParameterDefinition: {0}")
		@ArgumentsSource(ParameterDefinitionsProvider.class)
		void parameterDefinitions(String parameterDefinition, String httpMethod, URI path, Parameter parameter, Method javaMethod, Arguments arguments) {
			Endpoint endpoint = endpointMetadata.endpoint(WithParameters.class, javaMethod);

			assertAll(() -> assertNotNull(endpoint),
					() -> assertEquals(path, endpoint.path().expand(arguments).unsafe()),
					() -> assertThat(endpoint.headers(), emptyIterable()),
					() -> assertThat(endpoint.cookies(), emptyIterable()),
					() -> assertThat(endpoint.parameters(), contains(parameter)),
					() -> assertEquals(httpMethod, endpoint.method()));
		}
	}

	@Test
	void headers() throws Exception {
		Endpoint endpoint = endpointMetadata.endpoint(WithHeaders.class, WithHeaders.class.getMethod("headers"));

		assertAll(() -> assertNotNull(endpoint),
				  () -> assertEquals(URI.create("http://my.api.com/headers"), endpoint.path().expand().unsafe()),
				  () -> assertThat(endpoint.headers(), containsInAnyOrder(new com.github.ljtfreitas.julian.Header("X-Header-1", "x-header-1"),
					  													  new com.github.ljtfreitas.julian.Header("X-Header-2", "x-header-2"),
					  													  new com.github.ljtfreitas.julian.Header("X-Header-3", "x-header-3"),
					  													  new com.github.ljtfreitas.julian.Header("X-Header-4", "x-header-4"))),
				  () -> assertThat(endpoint.cookies(), emptyIterable()),
				  () -> assertThat(endpoint.parameters(), emptyIterable()),
				  () -> assertEquals("GET", endpoint.method()));
	}

	@Nested
	class MetaHeaders {

		@Test
		void authorization() throws Exception {
			Endpoint endpoint = endpointMetadata.endpoint(WithMetaHeaders.class, WithMetaHeaders.class.getDeclaredMethod("authorization", String.class));

			assertAll(() -> assertNotNull(endpoint),
					() -> assertEquals(URI.create("http://my.api.com/headers/authorization"), endpoint.path().expand().unsafe()),
					() -> assertThat(endpoint.headers(), emptyIterable()),
					() -> assertThat(endpoint.cookies(), emptyIterable()),
					() -> assertThat(endpoint.parameters(), contains(Parameter.header(0, "Authorization", JavaType.valueOf(String.class), new HeadersParameterSerializer()))),
					() -> assertEquals("GET", endpoint.method()));
		}

		@Test
		void contentType() throws Exception {
			Endpoint endpoint = endpointMetadata.endpoint(WithMetaHeaders.class, WithMetaHeaders.class.getDeclaredMethod("contentType", String.class));

			assertAll(() -> assertNotNull(endpoint),
					() -> assertEquals(URI.create("http://my.api.com/headers/content-type"), endpoint.path().expand().unsafe()),
					() -> assertThat(endpoint.headers(), emptyIterable()),
					() -> assertThat(endpoint.cookies(), emptyIterable()),
					() -> assertThat(endpoint.parameters(), contains(Parameter.header(0, "Content-Type", JavaType.valueOf(String.class), new HeadersParameterSerializer()))),
					() -> assertEquals("GET", endpoint.method()));
		}
	}

	@Test
	void cookies() throws Exception {
		Endpoint endpoint = endpointMetadata.endpoint(WithCookies.class, WithCookies.class.getMethod("cookies"));

		assertAll(() -> assertNotNull(endpoint),
				  () -> assertEquals(URI.create("http://my.api.com/headers"), endpoint.path().expand().unsafe()),
				  () -> assertThat(endpoint.headers(), emptyIterable()),
				  () -> assertThat(endpoint.cookies(), containsInAnyOrder(new com.github.ljtfreitas.julian.Cookie("some-cookie-1", "cookie-1"),
																		  new com.github.ljtfreitas.julian.Cookie("some-cookie-2", "cookie-2"),
																		  new com.github.ljtfreitas.julian.Cookie("some-cookie-3", "cookie-3"),
																		  new com.github.ljtfreitas.julian.Cookie("some-cookie-4", "cookie-4"))),
				  () -> assertThat(endpoint.parameters(), emptyIterable()),
				  () -> assertEquals("GET", endpoint.method()));
	}

	@Test
	void queryParameters() throws Exception {
		Endpoint endpoint = endpointMetadata.endpoint(WithQueryParameters.class, WithQueryParameters.class.getMethod("query"));

		URI path = endpoint.path().expand().unsafe();

		assertAll(() -> assertNotNull(endpoint),
				  () -> assertThat(path.toString(), startsWith("http://my.api.com/query")),
				  () -> assertThat(path.getQuery(), allOf(containsString("param1=value1"), containsString("param2=value2"), containsString("param3=value3"), containsString("param4=value4"))),
				  () -> assertThat(endpoint.headers(), emptyIterable()),
				  () -> assertThat(endpoint.cookies(), emptyIterable()),
				  () -> assertThat(endpoint.parameters(), emptyIterable()),
				  () -> assertEquals("GET", endpoint.method()));
	}

	@Nested
	class MetaAnnotations {
		
		@ParameterizedTest(name = "Meta-annotation: {0}")
		@ArgumentsSource(MetaAnnotationsProvider.class)
		void metaAnnotations(String annotation, String httpMethod, URI path, com.github.ljtfreitas.julian.Header header, Method javaMethod) {
			Endpoint endpoint = endpointMetadata.endpoint(WithMetaAnnotations.class, javaMethod);

			assertAll(() -> assertNotNull(endpoint),
					() -> assertEquals(path, endpoint.path().expand().unsafe()),
					() -> assertEquals(httpMethod, endpoint.method()),
					() -> assertThat(endpoint.headers(), contains(header)),
					() -> assertThat(endpoint.cookies(), emptyIterable()),
					() -> assertThat(endpoint.parameters(), emptyIterable()),
					() -> assertEquals(JavaType.valueOf(String.class), endpoint.returnType()));
		}

		@ParameterizedTest(name = "Meta-annotation: {0}")
		@ArgumentsSource(MetaAnnotationsOnBodyProvider.class)
		void metaAnnotationsOnBodyParameter(String annotation, String httpMethod, URI path, String contentType, Method javaMethod) {
			Endpoint endpoint = endpointMetadata.endpoint(WithMetaAnnotations.class, javaMethod);

			assertAll(() -> assertNotNull(endpoint),
					() -> assertEquals(path, endpoint.path().expand().unsafe()),
					() -> assertEquals(httpMethod, endpoint.method()),
					() -> assertThat(endpoint.headers(), emptyIterable()),
					() -> assertThat(endpoint.cookies(), emptyIterable()),
					() -> assertThat(endpoint.parameters(), contains(instanceOf(BodyParameter.class))),
					() -> assertEquals(JavaType.valueOf(String.class), endpoint.returnType()));

			BodyParameter bodyParameter = endpoint.parameters().body().get();
			assertEquals(contentType, bodyParameter.contentType().get());
		}

	}

	@Nested
	class InheritedDefinitions {

		@ParameterizedTest(name = "Inherited operation: {7}")
		@ArgumentsSource(InheritedDefinitionsProvider.class)
		void inheritedDefinitions(String httpMethod, URI path, Headers headers, Cookies cookies, Parameters parameters, JavaType returnType, Arguments arguments, Method javaMethod) {
			Endpoint endpoint = endpointMetadata.endpoint(SomeExtendedApi.class, javaMethod);

			assertAll(() -> assertNotNull(endpoint),
					() -> assertEquals(path, endpoint.path().expand(arguments).unsafe()),
					() -> assertEquals(httpMethod, endpoint.method()),
					() -> assertThat(endpoint.headers(), containsInAnyOrder(headers.all().toArray())),
					() -> assertThat(endpoint.cookies(), containsInAnyOrder(cookies.all().toArray())),
					() -> assertThat(endpoint.parameters(), containsInAnyOrder(parameters.all().toArray())),
					() -> assertEquals(returnType, endpoint.returnType()));
		}
	}
	
	@Nested
	class Restrictions {
		
		@Test
		void rejectMethodWithMoreThanOneBodyParameter() {
			assertThrows(IllegalStateException.class, () -> endpointMetadata.endpoint(Wrong.class, Wrong.class.getMethod("moreThanOneBody", Object.class, Object.class)));
		}

		@Test
		void rejectMethodWithoutHTTPMethod() {
			assertThrows(IllegalStateException.class, () -> endpointMetadata.endpoint(Wrong.class, Wrong.class.getMethod("withoutHTTPMethod")));
		}

		@Test
		void rejectMethodWithMoreThanOneHTTPMethod() {
			assertThrows(IllegalStateException.class, () -> endpointMetadata.endpoint(Wrong.class, Wrong.class.getMethod("moreThanOneHTTPMethod")));
		}
	}

	static class ParameterDefinitionsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context)
				throws Exception {

			Arguments arguments = Arguments.create("whatever");

			ParameterSerializer<Object, String> defaultSerializer = ParameterSerializer.simple();

			return Stream.of(arguments("@PathParameter", "GET", URI.create("http://my.api.com/whatever"), Parameter.path(0, "name", JavaType.valueOf(String.class), defaultSerializer), 
									WithParameters.class.getMethod("path", String.class), arguments),
							 arguments("@PathParameter", "GET", URI.create("http://my.api.com/whatever"), Parameter.path(0, "another-name", JavaType.valueOf(String.class), defaultSerializer),
									 WithParameters.class.getMethod("pathWithAnotherName", String.class), arguments),
							 arguments("@HeaderParameter", "GET", URI.create("http://my.api.com/header"), Parameter.header(0, "name", JavaType.valueOf(String.class), new HeadersParameterSerializer()),
									 WithParameters.class.getMethod("header", String.class), arguments),
						     arguments("@HeaderParameter", "GET", URI.create("http://my.api.com/header"), Parameter.header(0, "X-Whatever", JavaType.valueOf(String.class), new HeadersParameterSerializer()),
						    		 WithParameters.class.getMethod("headerWithAnotherName", String.class), arguments),
							 arguments("@CookieParameter", "GET", URI.create("http://my.api.com/cookie"), Parameter.cookie(0, "sessionId", JavaType.valueOf(String.class), new CookiesParameterSerializer()),
									 WithParameters.class.getMethod("cookie", String.class), arguments),
						 	 arguments("@CookieParameter", "GET", URI.create("http://my.api.com/cookie"), Parameter.cookie(0, "session-id", JavaType.valueOf(String.class), new CookiesParameterSerializer()),
						 			 WithParameters.class.getMethod("cookieWithAnotherName", String.class), arguments),
						 	 arguments("@QueryParameter", "GET", URI.create("http://my.api.com/query?name=whatever"), Parameter.query(0, "name", JavaType.valueOf(String.class), new QueryParameterSerializer()),
						 			 WithParameters.class.getMethod("query", String.class), arguments),
						 	 arguments("@QueryParameter", "GET", URI.create("http://my.api.com/query?another-name=whatever"), Parameter.query(0, "another-name", JavaType.valueOf(String.class), new QueryParameterSerializer()),
						 			 WithParameters.class.getMethod("queryWithAnotherName", String.class), arguments),
						 	 arguments("@QueryParameter", "GET", URI.create("http://my.api.com/query?parameters=whatever"), Parameter.query(0, "parameters", JavaType.parameterized(Map.class, String.class, String.class), new QueryParameterSerializer()),
						 			 WithParameters.class.getMethod("queryParameters", Map.class), Arguments.create(Map.of("parameters", "whatever"))),
							 arguments("@BodyParameter", "POST", URI.create("http://my.api.com/body"), Parameter.body(0, "body", JavaType.object()),
									 WithParameters.class.getMethod("body", Object.class), arguments),
							arguments("@CallbackParameter", "GET", URI.create("http://my.api.com/callback"), Parameter.callback(0, "callback", JavaType.parameterized(Consumer.class, String.class)),
									WithParameters.class.getMethod("successCallback", Consumer.class), null),
							arguments("@CallbackParameter", "GET", URI.create("http://my.api.com/callback"), Parameter.callback(0, "callback", JavaType.parameterized(Consumer.class, Throwable.class)),
									WithParameters.class.getMethod("failureCallback", Consumer.class), null),
							arguments("@CallbackParameter", "GET", URI.create("http://my.api.com/callback"), Parameter.callback(0, "callback", JavaType.parameterized(BiConsumer.class, String.class, Throwable.class)),
									WithParameters.class.getMethod("subscribe", BiConsumer.class), null));
		}
	}

	static class HTTPMethodProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context)
				throws Exception {

			return Stream.of(arguments("GET", URI.create("http://my.api.com/get"), MyType.class.getMethod("get")),
							 arguments("POST", URI.create("http://my.api.com/post"), MyType.class.getMethod("post")),
							 arguments("PUT", URI.create("http://my.api.com/put"), MyType.class.getMethod("put")),
							 arguments("DELETE", URI.create("http://my.api.com/delete"), MyType.class.getMethod("delete")),
							 arguments("PATCH", URI.create("http://my.api.com/patch"), MyType.class.getMethod("patch")),
							 arguments("HEAD", URI.create("http://my.api.com/head"), MyType.class.getMethod("head")),
							 arguments("OPTIONS", URI.create("http://my.api.com/options"), MyType.class.getMethod("options")),
							 arguments("TRACE", URI.create("http://my.api.com/trace"), MyType.class.getMethod("trace")));
		}
	}

	static class MetaAnnotationsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context)
				throws Exception {

			return Stream.of(arguments("@AcceptAll", "GET", URI.create("http://my.api.com/accept-all"), new com.github.ljtfreitas.julian.Header("Accept", "*/*"),
									WithMetaAnnotations.class.getMethod("acceptAll")),
					         arguments("@AcceptJson", "GET", URI.create("http://my.api.com/accept-json"), new com.github.ljtfreitas.julian.Header("Accept", "application/json"),
					        		 WithMetaAnnotations.class.getMethod("acceptJson")),
					         arguments("@AcceptXml", "GET", URI.create("http://my.api.com/accept-xml"), new com.github.ljtfreitas.julian.Header("Accept", "application/xml"),
					        		 WithMetaAnnotations.class.getMethod("acceptXml")));
		}
	}

	static class MetaAnnotationsOnBodyProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context)
				throws Exception {

			return Stream.of(arguments("@FormUrlEncoded", "POST", URI.create("http://my.api.com/form-url-encoded"), "application/x-www-form-urlencoded",
								WithMetaAnnotations.class.getMethod("formUrlEncoded", Object.class)),
							 arguments("@JsonContent", "POST", URI.create("http://my.api.com/json-content"), "application/json",
								WithMetaAnnotations.class.getMethod("jsonContent", Object.class)),
							 arguments("@MultipartFormData", "POST", URI.create("http://my.api.com/multipart-form-data"), "multipart/form-data",
								WithMetaAnnotations.class.getMethod("multipartFormData", Object.class)),
							 arguments("@SerializableContent", "POST", URI.create("http://my.api.com/serializable-content"),  "application/octet-stream",
								WithMetaAnnotations.class.getMethod("serializableContent", Object.class)),
							 arguments("@XmlContent", "POST", URI.create("http://my.api.com/xml-content"),  "application/xml",
								WithMetaAnnotations.class.getMethod("xmlContent", Object.class)));
		}
	}

	static class InheritedDefinitionsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends org.junit.jupiter.params.provider.Arguments> provideArguments(ExtensionContext context)
				throws Exception {

			Headers headers = Headers.create(new com.github.ljtfreitas.julian.Header("X-Base-Header", "some-header-value"), new com.github.ljtfreitas.julian.Header("X-Other-Header", "other-header-value"));
			Cookies cookies = Cookies.create(new com.github.ljtfreitas.julian.Cookie("some-cookie", "some-cookie-value"), new com.github.ljtfreitas.julian.Cookie("other-cookie", "other-cookie-value"));

			return Stream.of(arguments("POST", URI.create("http://my.api.com/some"),
									headers,
									cookies, 
									Parameters.create(Parameter.body(0, "body", JavaType.valueOf(SomePojo.class), "application/json")),
									JavaType.valueOf(void.class),
									Arguments.empty(),
									SomeExtendedApi.class.getMethod("create", Object.class)),
							 arguments("POST", URI.create("http://my.api.com/some"),
									headers,
									cookies,
									Parameters.create(Parameter.body(0, "body", JavaType.valueOf(SomePojo.class), "application/json")),
									JavaType.valueOf(void.class),
									Arguments.empty(),
									SomeExtendedApi.class.getMethod("createSomething", Object.class)),
							arguments("GET", URI.create("http://my.api.com/some/1"), 
									headers.join(new com.github.ljtfreitas.julian.Header("Accept", "application/json")),
									cookies, 
									Parameters.create(Parameter.path(0, "id", JavaType.valueOf(String.class), new DefaultParameterSerializer())),
									JavaType.valueOf(SomePojo.class),
									Arguments.create("1"),
									SomeExtendedApi.class.getMethod("read", String.class)),
							arguments("PUT", URI.create("http://my.api.com/some/1"), 
									headers,
									cookies, 
									Parameters.create(Parameter.path(0, "id", JavaType.valueOf(String.class), new DefaultParameterSerializer()), Parameter.body(1, "body", JavaType.valueOf(SomePojo.class), "application/json")),
									JavaType.valueOf(void.class),
									Arguments.create("1"),
									SomeExtendedApi.class.getMethod("update", String.class, Object.class)),
							arguments("DELETE", URI.create("http://my.api.com/some/1"), 
									headers,
									cookies, 
									Parameters.create(Parameter.path(0, "id", JavaType.valueOf(String.class), new DefaultParameterSerializer())), 
									JavaType.valueOf(void.class),
									Arguments.create("1"),
									SomeExtendedApi.class.getMethod("delete", String.class)),
							arguments("GET", URI.create("http://my.api.com/some"), 
									headers.join(new com.github.ljtfreitas.julian.Header("Accept", "application/json")),
									cookies,
									Parameters.empty(), 
									JavaType.parameterized(Collection.class, Wildcard.upper(SomePojo.class)),
									Arguments.empty(),
									SomeExtendedApi.class.getMethod("all")),
							arguments("GET", URI.create("http://my.api.com/some"), 
									headers.join(new com.github.ljtfreitas.julian.Header("Accept", "application/json")),
									cookies,
									Parameters.empty(), 
									JavaType.genericArrayOf(SomePojo.class),
									Arguments.empty(),
									SomeExtendedApi.class.getMethod("allAsArray")),
							arguments("POST", URI.create("http://my.api.com/some"), 
									Headers.create(new com.github.ljtfreitas.julian.Header("X-Base-Header", "some-header-value")),
									Cookies.create(new com.github.ljtfreitas.julian.Cookie("some-cookie", "some-cookie-value")),
									Parameters.create(Parameter.body(0, "some", JavaType.valueOf(SomePojo.class))),
									JavaType.valueOf(SomePojo.class),
									Arguments.empty(),
									SomeExtendedApi.class.getMethod("whatever", SomePojo.class)));
		}
	}

	@Path("http://my.api.com")
	private interface MyType {

		@GET("/get")
		void get();

		@POST("/post")
		void post();

		@PUT("/put")
		void put();

		@PATCH("/patch")
		void patch();

		@DELETE("/delete")
		void delete();

		@HEAD("/head")
		void head();

		@TRACE("/trace")
		void trace();

		@OPTIONS("/options")
		void options();
	}

	@Path("http://my.api.com")
	private interface WithParameters {

		@GET("/{name}")
		String path(@Path String name);

		@GET("/{another-name}")
		String pathWithAnotherName(@Path(name = "another-name") String name);

		@GET("/header")
		String header(@Header String name);

		@GET("/header")
		String headerWithAnotherName(@Header(name = "X-Whatever") String name);

		@GET("/cookie")
		String cookie(@Cookie String sessionId);

		@GET("/cookie")
		String cookieWithAnotherName(@Cookie(name = "session-id") String sessionId);

		@GET("/query")
		String query();
		
		@GET("/query")
		String query(@QueryParameter String name);

		@GET("/query")
		String queryWithAnotherName(@QueryParameter(name = "another-name") String name);

		@GET("/query")
		String queryParameters(@QueryParameter Map<String, String> parameters);

		@POST("/body")
		String body(@Body Object body);

		@GET("/callback")
		void successCallback(@Callback Consumer<String> callback);

		@GET("/callback")
		void failureCallback(@Callback Consumer<Throwable> callback);

		@GET("/callback")
		void subscribe(@Callback BiConsumer<String, Throwable> callback);
	}

	@Path("http://my.api.com")
	@QueryParameter(name = "param1", value = "value1")
	@QueryParameter(name = "param2", value = "value2")
	private interface WithQueryParameters {
		
		@GET("/query?param3=value3")
		@QueryParameter(name = "param4", value = "value4")
		String query();
	}

	@Path("http://my.api.com")
	@Header(name = "X-Header-1", value = "x-header-1")
	@Header(name = "X-Header-2", value = "x-header-2")
	private interface WithHeaders {

		@GET("/headers")
		@Header(name = "X-Header-3", value = "x-header-3")
		@Header(name = "X-Header-4", value = "x-header-4")
		void headers();
	}

	@Path("http://my.api.com")
	private interface WithMetaHeaders {

		@GET("/headers/authorization")
		String authorization(@Authorization String content);

		@GET("/headers/content-type")
		String contentType(@ContentType String mediaType);
	}

	@Path("http://my.api.com")
	@Cookie(name = "some-cookie-1", value = "cookie-1")
	@Cookie(name = "some-cookie-2", value = "cookie-2")
	private interface WithCookies {

		@GET("/headers")
		@Cookie(name = "some-cookie-3", value = "cookie-3")
		@Cookie(name = "some-cookie-4", value = "cookie-4")
		void cookies();
	}

	@Path("http://my.api.com")
	private interface WithMetaAnnotations {
		
		@GET("/accept-all")
		@AcceptAll
		String acceptAll();
		
		@GET("/accept-json")
		@AcceptJson
		String acceptJson();
		
		@GET("/accept-xml")
		@AcceptXml
		String acceptXml();

		@POST("/form-url-encoded")
		String formUrlEncoded(@FormUrlEncoded Object body);

		@POST("/json-content")
		String jsonContent(@JsonContent Object body);

		@POST("/multipart-form-data")
		String multipartFormData(@MultipartFormData Object body);

		@POST("/serializable-content")
		String serializableContent(@SerializableContent Object body);

		@POST("/xml-content")
		String xmlContent(@XmlContent Object body);
	}

	@Path("http://my.api.com")
	@Header(name = "X-Base-Header", value = "some-header-value")
	@Cookie(name = "some-cookie", value = "some-cookie-value")
	interface CrudOperations<T> {

		@POST
		@Header(name = "X-Other-Header", value = "other-header-value")
		@Cookie(name = "other-cookie", value = "other-cookie-value")
		void create(@JsonContent T body);
		
		@POST
		@Header(name = "X-Other-Header", value = "other-header-value")
		@Cookie(name = "other-cookie", value = "other-cookie-value")
		void createSomething(@Body(APPLICATION_JSON_VALUE) T body);
		
		@GET("/{id}")
		@AcceptJson
		@Header(name = "X-Other-Header", value = "other-header-value")
		@Cookie(name = "other-cookie", value = "other-cookie-value")
		T read(@Path String id);

		@PUT("/{id}")
		@Header(name = "X-Other-Header", value = "other-header-value")
		@Cookie(name = "other-cookie", value = "other-cookie-value")
		void update(@Path String id, @JsonContent T body);

		@DELETE("/{id}")
		@Header(name = "X-Other-Header", value = "other-header-value")
		@Cookie(name = "other-cookie", value = "other-cookie-value")
		void delete(@Path String id);

		@GET
		@AcceptJson
		@Header(name = "X-Other-Header", value = "other-header-value")
		@Cookie(name = "other-cookie", value = "other-cookie-value")
		Collection<? extends T> all();

		@GET
		@AcceptJson
		@Header(name = "X-Other-Header", value = "other-header-value")
		@Cookie(name = "other-cookie", value = "other-cookie-value")
		T[] allAsArray();
 	}

	@Path("/some")
	interface SomeExtendedApi extends CrudOperations<SomePojo> {

		@POST
		SomePojo whatever(@Body SomePojo some);
	}

	class SomePojo {
	}
	
	@Path("http://my.api.com")
	interface Wrong {

		@POST
		void moreThanOneBody(@Body Object body1, @Body Object body2);

		@POST
		void invalidConsumerCallback(@Callback Consumer<Exception> callback);
		
		@POST
		void invalidBiConsumerCallback(@Callback BiConsumer<String, String> callback);

		void withoutHTTPMethod();

		@GET
		@PUT
		void moreThanOneHTTPMethod();
	}
}
