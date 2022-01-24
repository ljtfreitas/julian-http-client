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

import com.github.ljtfreitas.julian.Preconditions.Precondition;
import com.github.ljtfreitas.julian.contract.ParameterSerializer;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.check;
import static com.github.ljtfreitas.julian.Preconditions.isTrue;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableList;

public class Endpoint {

    private final Path path;
    private final String method;
    private final Headers headers;
    private final Cookies cookies;
    private final Parameters parameters;
    private final JavaType returnType;

    Endpoint(Path path) {
        this(path, "GET");
    }

    Endpoint(Path path, String method) {
        this(path, method, Headers.empty());
    }

    Endpoint(Path path, String method, JavaType returnType) {
        this(path, method, Headers.empty(), Cookies.empty(), Parameters.empty(), returnType);
    }

    Endpoint(Path path, String method, Headers headers) {
        this(path, method, headers, Cookies.empty());
    }

    Endpoint(Path path, String method, Headers headers, Cookies cookies) {
        this(path, method, headers, cookies, Parameters.empty());
    }

    Endpoint(Path path, String method, Parameters parameters) {
        this(path, method, Headers.empty(), Cookies.empty(), parameters);
    }

    Endpoint(Path path, String method, Headers headers, Parameters parameters) {
        this(path, method, headers, Cookies.empty(), parameters, JavaType.none());
    }

    Endpoint(Path path, String method, Headers headers, Cookies cookies, Parameters parameters) {
        this(path, method, headers, cookies, parameters, JavaType.none());
    }

    public Endpoint(Path path, String method, Headers headers, Cookies cookies, Parameters parameters, JavaType returnType) {
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.cookies = cookies;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    protected Endpoint(Endpoint that) {
        this.path = that.path;
        this.method = that.method;
        this.headers = that.headers;
        this.cookies = that.cookies;
        this.parameters = that.parameters;
        this.returnType = that.returnType;
    }

    public Path path() {
        return path;
    }

    public String method() {
        return method;
    }

    public Headers headers() {
        return headers;
    }

    public Cookies cookies() {
        return cookies;
    }

    public Parameters parameters() {
        return parameters;
    }

    public JavaType returnType() {
        return returnType;
    }

    public Endpoint returns(JavaType adapted) {
        return new Endpoint(path, method, headers, cookies, parameters, adapted);
    }

    RequestDefinition request(Arguments arguments, JavaType returnType) {
        URI uri = path.expand(arguments)
                .prop(cause -> new IllegalArgumentException(path.show(), cause));

        Headers headers = headers(arguments).merge(cookies(arguments)).all().stream()
                .map(h -> new Header(h.name(), h.values()))
                .reduce(Headers.empty(), Headers::join, (a, b) -> b);

        RequestDefinition.Body body = body(arguments);

        return new RequestDefinition(uri, method, headers, body, returnType);
    }

    private RequestDefinition.Body body(Arguments arguments) {
        return parameters.body()
                .flatMap(p -> arguments.of(p.position()).map(value -> new RequestDefinition.Body(value, p.javaType())))
                .orElse(null);
    }

    private Headers headers(Arguments arguments) {
        Stream<Headers> bodyContentType = parameters.body()
                .flatMap(BodyParameter::contentType)
                .map(c -> new Header("Content-Type", c))
                .map(Headers::create)
                .stream();

        Stream<Headers> headerParameters = parameters.headers()
                .flatMap(header -> arguments.of(header.position())
                        .flatMap(header::resolve)
                        .stream());

        return headers.merge(Stream.concat(bodyContentType, headerParameters)
                .reduce(Headers::merge).orElseGet(Headers::empty));
    }

    private Stream<Header> cookies(Arguments arguments) {
        return cookies.merge(parameters.cookies()
                .flatMap(cookie -> arguments.of(cookie.position())
                        .flatMap(cookie::resolve)
                        .stream())
                .reduce(Cookies::merge).orElseGet(Cookies::empty))
                .header()
                .stream();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;

        if (that instanceof Endpoint) {
            Endpoint endpoint = (Endpoint) that;

            return Objects.equals(path, endpoint.path)
                && Objects.equals(method, endpoint.method)
                && Objects.equals(parameters, endpoint.parameters) && Objects.equals(returnType, endpoint.returnType);

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, method, parameters, returnType);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("path: ")
                    .append(path.show())
                    .append("\n")
                .append("HTTP method: ")
                    .append(method)
                    .append("\n")
                .append("headers: ")
                    .append(headers)
                    .append("\n")
                .append("cookies: ")
                    .append(cookies)
                    .append("\n")
                .append("parameters: ")
                    .append(parameters)
                    .append("\n")
                .append("return type: ")
                    .append(returnType)
                    .append("\n")
                .toString();
    }

    public static class Path implements Content {

        private final String path;
        private final QueryParameters queryParameters;
        private final Collection<Parameter> parameters;

        public Path(String path) {
            this(path, QueryParameters.empty(), Parameters.empty());
        }

        public Path(String path, Parameters parameters) {
            this(path, QueryParameters.empty(), parameters);
        }

        public Path(String path, QueryParameters queryParameters) {
            this(path, queryParameters, Parameters.empty());
        }

        public Path(String path, QueryParameters queryParameters, Parameters parameters) {
            this.path = nonNull(path);
            this.queryParameters = nonNull(queryParameters);
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
            return Except.run(() -> {
                URI source = URI.create(new DynamicParameters(path, parameters.stream().filter(PathParameter.class::isInstance).collect(toUnmodifiableList()))
                        .interpolate(arguments));

                String query = new QueryStringBuilder(source.getQuery(), queryParameters, parameters.stream().filter(QueryParameter.class::isInstance).collect(toUnmodifiableList()))
                        .interpolate(arguments);

                return new URI(source.getScheme(), source.getUserInfo(), source.getHost(), source.getPort(), source.getPath(),
                        query == null || query.isEmpty() ? null : query, source.getFragment());
            });
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;

            if (that instanceof Path) {
                return Objects.equals(path, ((Path) that).path);

            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        @Override
        public String show() {
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

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;

            if (that instanceof Parameters) {
                return Objects.equals(parameters, ((Parameters) that).parameters);

            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(parameters);
        }

        @Override
        public String toString() {
            return parameters.toString();
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
            return new PathParameter(position, name, javaType, serializer, null);
        }

        public static Parameter path(int position, String name, JavaType javaType, ParameterSerializer<? super Object, String> serializer, String defaultValue) {
            return new PathParameter(position, name, javaType, serializer, defaultValue != null && defaultValue.trim().isEmpty() ? null : defaultValue);
        }

        public static Parameter query(int position, String name, JavaType javaType, ParameterSerializer<? super Object, QueryParameters> serializer) {
            return query(position, name, javaType, serializer, null);
        }

        public static Parameter query(int position, String name, JavaType javaType, ParameterSerializer<? super Object, QueryParameters> serializer, String[] defaultValue) {
            return new QueryParameter(position, name, javaType, serializer, defaultValue != null && defaultValue.length == 0 ? null : defaultValue);
        }

        public static Parameter header(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Headers> serializer) {
            return header(position, name, javaType, serializer, null);
        }

        public static Parameter header(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Headers> serializer, String[] defaultValue) {
            return new HeaderParameter(position, name, javaType, serializer, defaultValue != null && defaultValue.length == 0 ? null : defaultValue);
        }

        public static Parameter cookie(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Cookies> serializer) {
            return cookie(position, name, javaType, serializer, null);
        }

        public static Parameter cookie(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Cookies> serializer, String defaultValue) {
            return new CookieParameter(position, name, javaType, serializer, defaultValue != null && defaultValue.trim().isEmpty() ? null : defaultValue);
        }

        public static Parameter body(int position, String name, JavaType javaType) {
            return new BodyParameter(position, name, javaType, null);
        }

        public static Parameter body(int position, String name, JavaType javaType, String contentType) {
            return new BodyParameter(position, name, javaType, contentType);
        }

        public static Parameter callback(int position, String name, JavaType javaType) {
            return new CallbackParameter(position, name, javaType);
        }
    }

    abstract static class SerializableParameter<T> extends Parameter {

        private final ParameterSerializer<? super Object, T> serializer;
        private final Object defaultValue;

        SerializableParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, T> serializer, Object defaultValue) {
            super(position, name, javaType);
            this.serializer = serializer;
            this.defaultValue = defaultValue;
        }

        public Optional<T> resolve(Object arg) {
            Supplier<Optional<T>> fallback = () -> defaultValue == null ? Optional.empty() : serializer.serialize(name(), javaType(), defaultValue);
            return serializer.serialize(name(), javaType(), arg).or(fallback);
        }
    }

    abstract static class StringSerializableParameter extends SerializableParameter<String> {

        StringSerializableParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, String> serializer, String defaultValue) {
            super(position, name, javaType, serializer, defaultValue);
        }
    }

    public static class PathParameter extends StringSerializableParameter {

        private PathParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, String> serializer, String defaultValue) {
            super(position, name, javaType, serializer, defaultValue);
        }
    }

    public static class QueryParameter extends SerializableParameter<QueryParameters> {

        private QueryParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, QueryParameters> serializer, String[] defaultValue) {
            super(position, name, javaType, serializer, defaultValue);
        }
    }

    public static class HeaderParameter extends SerializableParameter<Headers> {

        private HeaderParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Headers> serializer, String[] defaultValue) {
            super(position, name, javaType, serializer, defaultValue);
        }
    }

    public static class CookieParameter extends SerializableParameter<Cookies> {

        private CookieParameter(int position, String name, JavaType javaType, ParameterSerializer<? super Object, Cookies> serializer, String defaultValue) {
            super(position, name, javaType, serializer, defaultValue);
        }
    }

    public static class BodyParameter extends Parameter {

        private final String contentType;

        private BodyParameter(int position, String name, JavaType javaType, String contentType) {
            super(position, name, javaType);
            this.contentType = contentType == null || contentType.trim().isEmpty() ? null : contentType;
        }

        public Optional<String> contentType() {
            return Optional.ofNullable(contentType);
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o) && Objects.equals(contentType, ((BodyParameter) o).contentType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), contentType);
        }
    }

    public static class CallbackParameter extends Parameter {

        public CallbackParameter(int position, String name, JavaType javaType) {
            super(position, name, javaType);
        }
    }

    static class QueryStringBuilder {

        private final String source;
        private final QueryParameters queryParameters;
        private final Collection<Parameter> parameters;

        QueryStringBuilder(String source, QueryParameters queryParameters, Collection<Parameter> parameters) {
            this.source = source;
            this.queryParameters = queryParameters;
            this.parameters = parameters;
        }

        String interpolate(Arguments arguments) {
            return Stream.concat(Stream.of(source), dynamic(arguments))
                    .filter(not(Objects::isNull))
                    .filter(not(String::isEmpty))
                    .reduce(queryParameters, (q, s) -> q.append(QueryParameters.parse(s)), (a, b) -> b)
                    .serialize();
        }

        private Stream<String> dynamic(Arguments arguments) {
            return parameters.stream()
                    .map(QueryParameter.class::cast)
                    .flatMap(p -> arguments.of(p.position()).flatMap(p::resolve).map(QueryParameters::serialize).map(String::trim).stream());
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
            Matcher matcher = DYNAMIC_PARAMETER_PATTERN.matcher(input);

            Function<MatchResult, String> group = m -> m.group(1);

            return matcher.replaceAll(group.andThen(name -> parameters.stream()
                    .filter(p -> p.is(name))
                    .filter(StringSerializableParameter.class::isInstance)
                    .map(StringSerializableParameter.class::cast)
                    .findFirst()
                    .map(p -> arguments.of(p.position())
                            .flatMap(p::resolve)
                            .orElseThrow(() -> new IllegalArgumentException("The argument [" + name + "] cannot be null.")))
                    .orElse(name)));
        }

        static Stream<String> find(String input) {
            Matcher matcher = DYNAMIC_PARAMETER_PATTERN.matcher(input);
            return matcher.results().map(match -> match.group(1));
        }
    }

}
