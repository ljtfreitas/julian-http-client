/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.ljtfreitas.julian;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.github.ljtfreitas.julian.Preconditions.Precondition;
import com.github.ljtfreitas.julian.contract.ParameterSerializer;

import static com.github.ljtfreitas.julian.Except.run;
import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.check;
import static com.github.ljtfreitas.julian.Preconditions.isTrue;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableList;

public class EndpointDefinition implements Endpoint {

	private final Path path;
	private final String method;
	private final Headers headers;
	private final Cookies cookies;
	private final Parameters parameters;
	private final JavaType returnType;

	public EndpointDefinition(Path path) {
		this(path, "GET");
	}

	public EndpointDefinition(Path path, String method) {
		this(path, method, Headers.empty(), Cookies.empty(), Parameters.empty());
	}

	public EndpointDefinition(Path path, String method, Headers headers, Cookies cookies, Parameters parameters) {
		this(path, method, headers, cookies, parameters, JavaType.none());
	}

	public EndpointDefinition(Path path, String method, Headers headers, Cookies cookies, Parameters parameters, JavaType returnType) {
		this.path = path;
		this.method = method;
		this.headers = headers;
		this.cookies = cookies;
		this.parameters = parameters;
		this.returnType = returnType;
	}

	@Override
	public Path path() {
		return path;
	}

	@Override
	public String method() {
		return method;
	}

	@Override
	public Headers headers() {
		return headers;
	}

	@Override
	public Cookies cookies() {
		return cookies;
	}

	@Override
	public Parameters parameters() {
		return parameters;
	}

	@Override
	public JavaType returnType() {
		return returnType;
	}

	public static class Path {

		private final String path;
		private final QueryString queryString;
		private final Collection<Parameter> parameters;

		public Path(String path) {
			this(path, QueryString.empty(), Parameters.empty());
		}

		public Path(String path, Parameters parameters) {
			this(path, QueryString.empty(), parameters);
		}
		
		public Path(String path, QueryString queryString, Parameters parameters) {
			this.path = nonNull(path);
			this.queryString = nonNull(queryString);
			this.parameters = check(parameters, bindable()).just(PathParameter.class, QueryParameter.class).collect(toUnmodifiableList());
		}

		private Precondition<Parameters, Parameters> bindable() {
			return parameters -> {
				if (parameters.all().isEmpty()) return parameters;
	
				Collection<String> unbindable = DynamicParameters.find(path)
						.filter(m -> parameters.just(PathParameter.class).noneMatch(p -> p.is(m))).collect(toUnmodifiableList());
	
				return isTrue(parameters, p -> unbindable.isEmpty(), 
						() -> format("There are path variables which does not have bindable @Path arguments: {0}", String.join(",", unbindable)));
			};
		}

		public Except<URI> expand() {
			return expand(Arguments.empty());
		}

		public Except<URI> expand(Arguments arguments) {
			return run(() -> {
				URI source = URI.create(new DynamicParameters(path, parameters.stream().filter(PathParameter.class::isInstance).collect(toUnmodifiableList()))
						.interpolate(arguments));
	
				String query = new QueryStringBuilder(source.getQuery(), queryString, parameters.stream().filter(QueryParameter.class::isInstance).collect(toUnmodifiableList()))
						.interpolate(arguments);

				return new URI(source.getScheme(), source.getUserInfo(), source.getHost(), source.getPort(), source.getPath(), 
						query == null || query.isEmpty() ? null : query, source.getFragment());
			});
		}

		@Override
		public String toString() {
			return path;
		}
	}

	public static class Parameters implements Iterable<Parameter> {

		private final Collection<Parameter> parameters;

		public Parameters(Collection<Parameter> parameters) {
			this.parameters = parameters;
		}


		@SafeVarargs
		final Stream<Parameter> just(Class<? extends Parameter>... classes) {
			Function<Predicate<? super Parameter>, Predicate<? super Parameter>> id = Function.identity();

			Predicate<? super Parameter> predicate = Arrays.stream(classes)
					.map(c -> id.apply(c::isInstance))
					.reduce(Predicate::or)
					.orElseGet(() -> (any) -> true);

			return parameters.stream().filter(predicate);
		}

		public Stream<HeaderParameter> headers() {
			return just(HeaderParameter.class).map(HeaderParameter.class::cast);
		}

		public Stream<CookieParameter> cookies() {
			return just(CookieParameter.class).map(CookieParameter.class::cast);
		}

		public Optional<BodyParameter> body() {
			return just(BodyParameter.class).map(BodyParameter.class::cast).findFirst();
		}

		public Stream<CallbackParameter> callbacks() {
			return just(CallbackParameter.class).map(CallbackParameter.class::cast);
		}

		@Override
		public Iterator<Parameter> iterator() {
			return parameters.iterator();
		}

		public Collection<Parameter> all() {
			return unmodifiableCollection(parameters);
		}
		
		public static Parameters create(Parameter... parameters) {
			return new Parameters(List.of(parameters));
		}

		public static Parameters empty() {
			return new Parameters(emptyList());
		}
	}
	
	public abstract static class Parameter {

		private final int position;
		private final String name;
		private final JavaType javaType;

		Parameter(int position, String name, JavaType javaType) {
			this.position = position;
			this.name = name;
			this.javaType = javaType;
		}

		public int position() {
			return position;
		}

		public String name() {
			return name;
		}
		
		public JavaType javaType() {
			return javaType;
		}

		boolean is(String name) {
			return this.name.equalsIgnoreCase(name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(position, name, javaType);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;

			if (obj instanceof Parameter) {
				Parameter that = (Parameter) obj;
				
				return Objects.equals(position, that.position)
					&& Objects.equals(name, that.name)
					&& Objects.equals(javaType, that.javaType);
			
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return format("{0}: {1} {2}", position, name, javaType);
		}

		public static Parameter path(int position, String name, JavaType javaType, ParameterSerializer<? super Object, String> serializer) {
			return new PathParameter(position, name, javaType, serializer);
		}

		public static Parameter query(int position, String name, JavaType javaType, ParameterSerializer<? super Object, QueryString> serializer) {
			return new QueryParameter(position, name, javaType, serializer);
		}

		public static Parameter header(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Headers> serializer) {
			return new HeaderParameter(position, name, javaType, serializer);
		}

		public static Parameter cookie(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Cookies> serializer) {
			return new CookieParameter(position, name, javaType, serializer);
		}

		public static Parameter body(int position, String name, JavaType javaType) {
			return new BodyParameter(position, name, javaType);
		}

		public static Parameter callback(int position, String name, JavaType javaType) {
			return new CallbackParameter(position, name, javaType);
		}
	}

	abstract static class SerializableParameter<T> extends Parameter {

		private final ParameterSerializer<? super Object, T> serializer;

		SerializableParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, T> serializer) {
			super(position, name, javaType);
			this.serializer = serializer;
		}

		public Optional<T> resolve(Object arg) {
			return serializer.serialize(name(), javaType(), arg);
		}
	}

	abstract static class StringSerializableParameter extends SerializableParameter<String> {

		StringSerializableParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, String> serializer) {
			super(position, name, javaType, serializer);
		}
	}

	public static class PathParameter extends StringSerializableParameter {

		public PathParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, String> serializer) {
			super(position, name, javaType, serializer);
		}
	}
	
	public static class QueryParameter extends SerializableParameter<QueryString> {
		
		public QueryParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, QueryString> serializer) {
			super(position, name, javaType, serializer);
		}
	}

	public static class HeaderParameter extends SerializableParameter<Headers> {
		
		public HeaderParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Headers> serializer) {
			super(position, name, javaType, serializer);
		}
	}

	public static class CookieParameter extends SerializableParameter<Cookies> {

		public CookieParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Cookies> serializer) {
			super(position, name, javaType, serializer);
		}
	}

	public static class BodyParameter extends Parameter {

		public BodyParameter(int position, String name, JavaType javaType) {
			super(position, name, javaType);
		}
	}

	public static class CallbackParameter extends Parameter {

		public CallbackParameter(int position, String name, JavaType javaType) {
			super(position, name, javaType);
		}
	}

	static class QueryStringBuilder {

		private final String source;
		private final QueryString queryString;
		private final Collection<Parameter> parameters;

		QueryStringBuilder(String source, QueryString queryString, Collection<Parameter> parameters) {
			this.source = source;
			this.queryString = queryString;
			this.parameters = parameters;
		}

		String interpolate(Arguments arguments) {
			return Stream.concat(Stream.of(source), dynamic(arguments))
					.filter(not(Objects::isNull))
					.filter(not(String::isEmpty))
					.reduce(queryString, (q, s) -> q.append(QueryString.parse(s)), (a, b) -> b)
					.serialize();
		}

		private Stream<String> dynamic(Arguments arguments) {
			return parameters.stream()
					.map(QueryParameter.class::cast)
					.flatMap(p -> arguments.of(p.position()).flatMap(p::resolve).map(QueryString::serialize).map(String::trim).stream());
		}
	}
	
	static class DynamicParameters {

		private static final Pattern DYNAMIC_PARAMETER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9\\-_]+)}");

		private final String input;
		private final Collection<Parameter> parameters;
		
		private DynamicParameters(String input, Collection<Parameter> parameters) {
			this.input = input;
			this.parameters = parameters;
		}

		String interpolate(Arguments arguments) {
			StringBuffer buffer = new StringBuffer();

			Matcher matcher = DYNAMIC_PARAMETER_PATTERN.matcher(input);
			
			while (matcher.find()) {
				MatchResult match = matcher.toMatchResult();

				String name = match.group(1);

				parameters.stream()
						.filter(p -> p.is(name))
						.filter(StringSerializableParameter.class::isInstance)
						.map(StringSerializableParameter.class::cast)
						.findFirst()
						.ifPresent(p -> matcher.appendReplacement(buffer, arguments.of(p.position())
									.flatMap(p::resolve)
									.orElseThrow(() -> new IllegalArgumentException("The argument [" + name + "] cannot be null."))));
			}

			matcher.appendTail(buffer);

			return buffer.toString();
		}
		
		static Stream<String> find(String input) {
			Matcher matcher = DYNAMIC_PARAMETER_PATTERN.matcher(input);
			return matcher.results().map(match -> match.group(1));
		}
	}

}
