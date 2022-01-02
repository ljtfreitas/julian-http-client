package com.github.ljtfreitas.julian.http.codec.xml.jaxb;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import static com.github.ljtfreitas.julian.http.MediaType.APPLICATION_XML;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JaxBXMLHTTPMessageCodecTest {

    private final JaxBHTTPMessageCodec<Person> codec = new JaxBHTTPMessageCodec<>();

    @Nested
    class Readable {

        @Test
        void unsupported() {
            assertFalse(codec.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void supported() {
            assertTrue(codec.readable(MediaType.valueOf("application/xml"), JavaType.valueOf(Person.class)));
        }

        @Nested
        class Read {

            @Test
            void read() {
                String value = "<person><name>Tiago</name><age>35</age></person>";

                Person person = codec.read(value.getBytes(), JavaType.valueOf(Person.class));

                assertAll(() -> assertEquals("Tiago", person.name),
                          () -> assertEquals(35, person.age));
            }
        }
    }

    @Nested
    class Writable {

        @Test
        void unsupported() {
            assertFalse(codec.writable(MediaType.valueOf("text/plain"), JavaType.valueOf(Object.class)));
        }

        @Test
        void supported() {
            assertTrue(codec.writable(MediaType.valueOf("application/xml"), JavaType.valueOf(Person.class)));
        }

        @Nested
        class Write {

            @Test
            void write() {
                HTTPRequestBody output = codec.write(new Person("Tiago", 35), StandardCharsets.UTF_8);

                assertEquals(APPLICATION_XML, output.contentType().orElseThrow());

                output.serialize().subscribe(new Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(ByteBuffer item) {
                        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                                              "<person><name>Tiago</name><age>35</age></person>", new String(item.array()));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        fail(throwable);
                    }

                    @Override
                    public void onComplete() {
                    }
                });
            }
        }
    }

    @XmlRootElement(name = "person")
    private static class Person {

        @XmlElement
        String name;

        @XmlElement
        int age;

        Person() {}

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}