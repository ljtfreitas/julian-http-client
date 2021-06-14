package com.github.ljtfreitas.julian.http.codec;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.ljtfreitas.julian.http.codec.ContentType;

class ContentTypeTest {

	@Nested
	class MimeTypes {
		
		@Test
		void simple() {
			String mimeType = "application/json";

			ContentType contentType = ContentType.valueOf(mimeType);

			assertAll(() -> assertEquals(mimeType, contentType.mime()),
					  () -> assertEquals(mimeType, contentType.toString()),
					  () -> assertTrue(contentType.parameters().isEmpty()));
		}
		
		@Nested
		class WithParameters {

			@Test
			void one() {
				String mimeType = "application/json;charset=UTF-8";

				ContentType contentType = ContentType.valueOf(mimeType);

				assertAll(() -> assertEquals("application/json", contentType.mime()),
						  () -> assertEquals(mimeType, contentType.toString()),
						  () -> assertFalse(contentType.parameters().isEmpty()),
						  () -> assertEquals("UTF-8", contentType.parameter("charset").get()));

			}

			@Test
			void multiple() {
				String mimeType = "multipart/form-data;charset=UTF-8;boundary=abc1234";

				ContentType contentType = ContentType.valueOf(mimeType);

				assertAll(() -> assertEquals("multipart/form-data", contentType.mime()),
						  () -> assertEquals(mimeType, contentType.toString()),
						  () -> assertFalse(contentType.parameters().isEmpty()),
						  () -> assertEquals("UTF-8", contentType.parameter("charset").get()),
						  () -> assertEquals("abc1234", contentType.parameter("boundary").get()));
			}

			@Test
			void added() {
				ContentType contentType = ContentType.valueOf("application/json");

				ContentType withCharset = contentType.parameter("charset", "UTF-8");

				assertAll(() -> assertTrue(contentType.parameters().isEmpty()),
						  () -> assertFalse(withCharset.parameters().isEmpty()),
						  () -> assertEquals("application/json", withCharset.mime()),
						  () -> assertEquals("UTF-8", withCharset.parameter("charset").get()),
						  () -> assertEquals("application/json;charset=UTF-8", withCharset.toString()));
			}
		}
		
		@Nested
		class Wildcard {
			
			@Test
			void generic() {
				ContentType wildcard = ContentType.valueOf("*/*");

				ContentType textPlain = ContentType.valueOf("text/plain");

				assertTrue(wildcard.compatible(textPlain));
				assertTrue(textPlain.compatible(wildcard));
			}

			@Test
			void specific() {
				ContentType someTextContent = ContentType.valueOf("text/*");

				ContentType textPlain = ContentType.valueOf("text/plain");

				assertTrue(someTextContent.compatible(textPlain));
				assertTrue(textPlain.compatible(someTextContent));
			}
		}

		@Nested
		class Compatibility {

			@ParameterizedTest
			@CsvSource({"application/json,application/xml",
						"application/vnd.bla+xml,application/vnd.bla+json"})
			void incompatible(String a, String b) {
				ContentType one = ContentType.valueOf(a);

				ContentType that = ContentType.valueOf(b);

				assertFalse(one.compatible(that));
				assertFalse(that.compatible(one));
			}

			@ParameterizedTest
			@CsvSource({"application/vnd.bla+json;version=1,application/vnd.bla+json;version=2",
						"application/hal+json,application/*+json",
						"*/*,application/hal+json"})
			void compatible(String a, String b) {
				ContentType one = ContentType.valueOf(a);

				ContentType that = ContentType.valueOf(b);

				assertTrue(one.compatible(that));
				assertTrue(that.compatible(one));
			}
		}
	}
}
