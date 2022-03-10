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

package com.github.ljtfreitas.julian.http.codec.xml.jackson;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.XMLHTTPMessageCodec;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_XML;

public class JacksonXMLHTTPMessageCodec implements XMLHTTPMessageCodec<Object> {

    private static final JacksonXMLHTTPMessageCodec SINGLE_INSTANCE = new JacksonXMLHTTPMessageCodec();

    private final XmlMapper xmlMapper;
    private final TypeFactory typeFactory;
    private final XmlFactory xmlFactory;

    public JacksonXMLHTTPMessageCodec() {
        this(new XmlMapper());
    }

    public JacksonXMLHTTPMessageCodec(XmlMapper xmlMapper) {
        this.xmlMapper = nonNull(xmlMapper);
        this.typeFactory = xmlMapper.getTypeFactory();
        this.xmlFactory = xmlMapper.getFactory();
    }

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && xmlMapper.canSerialize(javaType.rawClassType());
    }

    @Override
    public HTTPRequestBody write(Object body, Charset encoding) {
        return new DefaultHTTPRequestBody(APPLICATION_XML, () -> serialize(body, encoding));
    }

    private BodyPublisher serialize(Object body, Charset encoding) {
        JsonEncoding jsonEncoding = Stream.of(JsonEncoding.values()).filter(e -> e.getJavaName().equalsIgnoreCase(encoding.name()))
                .findFirst()
                .orElse(JsonEncoding.UTF8);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ToXmlGenerator xmlGenerator = xmlFactory.createGenerator(output, jsonEncoding)) {

            xmlMapper.writeValue(xmlGenerator, body);

            output.flush();

            return BodyPublishers.ofByteArray(output.toByteArray());
        } catch (IOException e) {
            throw new HTTPRequestWriterException("XML serialization failed. Source: " + body, e);
        }
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && readable(javaType.get());
    }

    private boolean readable(Type type) {
        return xmlMapper.canDeserialize(typeFactory.constructType(type));
    }

    @Override
    public Optional<CompletableFuture<Object>> read(HTTPResponseBody body, JavaType javaType) {
        return body.readAsInputStream(s -> deserialize(s, javaType));
    }

    private Object deserialize(InputStream bodyAsStream, JavaType javaType) {
        try (InputStreamReader reader = new InputStreamReader(bodyAsStream);
            BufferedReader buffered = new BufferedReader(reader)) {

            return xmlMapper.readValue(buffered, typeFactory.constructType(javaType.get()));
        } catch (IOException e) {
            throw new HTTPResponseReaderException("XML deserialization failed. The target type was: " + javaType, e);
        }
    }

    public static JacksonXMLHTTPMessageCodec provider() {
        return SINGLE_INSTANCE;
    }
}
