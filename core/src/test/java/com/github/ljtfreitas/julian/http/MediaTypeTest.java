package com.github.ljtfreitas.julian.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaTypeTest {

	@Nested
	class MimeTypes {
		
		@Test
		void simple() {
			String mimeType = "application/json";

			MediaType mediaType = MediaType.valueOf(mimeType);

			assertAll(() -> assertEquals(mimeType, mediaType.mime()),
					  () -> assertEquals(mimeType, mediaType.toString()),
					  () -> assertTrue(mediaType.parameters().isEmpty()));
		}
		
		@Nested
		class WithParameters {

			@Test
			void one() {
				String mimeType = "application/json;charset=UTF-8";

				MediaType mediaType = MediaType.valueOf(mimeType);

				assertAll(() -> assertEquals("application/json", mediaType.mime()),
						  () -> assertEquals(mimeType, mediaType.toString()),
						  () -> assertFalse(mediaType.parameters().isEmpty()),
						  () -> assertEquals("UTF-8", mediaType.parameter("charset").get()));

			}

			@Test
			void multiple() {
				String mimeType = "multipart/form-data;charset=UTF-8;boundary=abc1234";

				MediaType mediaType = MediaType.valueOf(mimeType);

				assertAll(() -> assertEquals("multipart/form-data", mediaType.mime()),
						  () -> assertEquals(mimeType, mediaType.toString()),
						  () -> assertFalse(mediaType.parameters().isEmpty()),
						  () -> assertEquals("UTF-8", mediaType.parameter("charset").get()),
						  () -> assertEquals("abc1234", mediaType.parameter("boundary").get()));
			}

			@Test
			void added() {
				MediaType mediaType = MediaType.valueOf("application/json");

				MediaType withCharset = mediaType.parameter("charset", "UTF-8");

				assertAll(() -> assertTrue(mediaType.parameters().isEmpty()),
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
				MediaType wildcard = MediaType.valueOf("*/*");

				MediaType textPlain = MediaType.valueOf("text/plain");

				assertTrue(wildcard.compatible(textPlain));
				assertTrue(textPlain.compatible(wildcard));
			}

			@Test
			void specific() {
				MediaType someTextContent = MediaType.valueOf("text/*");

				MediaType textPlain = MediaType.valueOf("text/plain");

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
				MediaType one = MediaType.valueOf(a);

				MediaType that = MediaType.valueOf(b);

				assertFalse(one.compatible(that));
				assertFalse(that.compatible(one));
			}

			@ParameterizedTest
			@CsvSource({"application/vnd.bla+json;version=1,application/vnd.bla+json;version=2",
						"application/hal+json,application/*+json",
						"*/*,application/hal+json"})
			void compatible(String a, String b) {
				MediaType one = MediaType.valueOf(a);

				MediaType that = MediaType.valueOf(b);

				assertTrue(one.compatible(that));
				assertTrue(that.compatible(one));
			}
		}
	}
}
