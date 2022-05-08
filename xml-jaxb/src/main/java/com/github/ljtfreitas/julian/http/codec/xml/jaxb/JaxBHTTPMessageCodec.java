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

package com.github.ljtfreitas.julian.http.codec.xml.jaxb;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPResponseBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.XMLHTTPMessageCodec;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_XML;
import static jakarta.xml.bind.Marshaller.JAXB_ENCODING;

public class JaxBHTTPMessageCodec implements XMLHTTPMessageCodec<Object> {

    private static final JaxBHTTPMessageCodec SINGLE_INSTANCE = new JaxBHTTPMessageCodec();

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private final Map<Class<?>, JAXBContext> xmlContexts = new HashMap<>();

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && javaType.classType()
                .map(c -> c.isAnnotationPresent(XmlRootElement.class))
                .orElse(false);
    }

    @Override
    public HTTPRequestBody write(Object body, Charset encoding) {
        return new DefaultHTTPRequestBody(APPLICATION_XML, () -> serialize(body, encoding));
    }

    private Publisher<ByteBuffer> serialize(Object body, Charset encoding) {
        JAXBContext context = context(body.getClass());

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(output, encoding)){

            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(JAXB_ENCODING, encoding.name());

            marshaller.marshal(body, new StreamResult(writer));

            writer.flush();
            output.flush();

            return HttpRequest.BodyPublishers.ofByteArray(output.toByteArray());

        } catch (IOException | JAXBException e) {
            throw new HTTPRequestWriterException("XML serialization failed. Source: " + body, e);
        }
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && javaType.classType()
                .map(c -> c.isAnnotationPresent(XmlRootElement.class) || c.isAnnotationPresent(XmlType.class))
                .orElse(false);
    }

    @Override
    public Optional<CompletableFuture<Object>> read(HTTPResponseBody body, JavaType javaType) {
        return body.readAsInputStream(stream -> deserialize(stream, javaType));
    }

    private Object deserialize(InputStream bodyAsStream, JavaType javaType) {
        Class<?> expectedClassType = javaType.rawClassType();

        JAXBContext context = context(expectedClassType);

        try (InputStreamReader reader = new InputStreamReader(bodyAsStream);
             BufferedReader buffered = new BufferedReader(reader)) {

            Unmarshaller unmarshaller = context.createUnmarshaller();

            StreamSource source = new StreamSource(buffered);

            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(source);

            boolean isXmlRoot = expectedClassType.isAnnotationPresent(XmlRootElement.class);

            Object deserialized = isXmlRoot ? unmarshaller.unmarshal(xmlReader)
                    : unmarshaller.unmarshal(xmlReader, expectedClassType).getValue();

            return deserialized;

        } catch (IOException | JAXBException | XMLStreamException e) {
            throw new HTTPResponseReaderException("JSON deserialization failed. The target type was: " + javaType, e);
        }
    }

    private JAXBContext context(Class<?> classType) {
        return xmlContexts.compute(classType, (t, context) -> context == null ? newContext(t) : context);
    }

    private JAXBContext newContext(Class<?> classType) {
        return Attempt.run(() -> JAXBContext.newInstance(classType)).unsafe();
    }

    public static JaxBHTTPMessageCodec provider() {
        return SINGLE_INSTANCE;
    }
}
