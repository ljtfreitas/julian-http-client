package com.github.ljtfreitas.julian.http.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.codec.ByteArrayHTTPResponseReader;
import com.github.ljtfreitas.julian.http.codec.ContentType;

class ByteArrayHTTPResponseReaderTest {

	ByteArrayHTTPResponseReader reader = new ByteArrayHTTPResponseReader();

	@Nested
	class Readable {
		
		@Test
		void supported() {
			assertTrue(reader.readable(ContentType.valueOf("text/plain"), JavaType.valueOf(byte[].class)));
		}
	
		@Test
		void unsupported() {
			assertFalse(reader.readable(ContentType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}
	
	@Test
	void read() {
		byte[] expected = "response body".getBytes();
		assertArrayEquals(expected, reader.read(new ByteArrayInputStream(expected), JavaType.valueOf(byte[].class)));
	}
}
