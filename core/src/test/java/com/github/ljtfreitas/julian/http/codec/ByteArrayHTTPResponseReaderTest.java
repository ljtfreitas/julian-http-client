package com.github.ljtfreitas.julian.http.codec;

import java.io.ByteArrayInputStream;

import com.github.ljtfreitas.julian.http.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.JavaType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ByteArrayHTTPResponseReaderTest {

	private ByteArrayHTTPResponseReader reader = new ByteArrayHTTPResponseReader();

	@Nested
	class Readable {
		
		@Test
		void supported() {
			assertTrue(reader.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(byte[].class)));
		}
	
		@Test
		void unsupported() {
			assertFalse(reader.readable(MediaType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}
	
	@Test
	void read() {
		byte[] expected = "response body".getBytes();
		assertArrayEquals(expected, reader.read(expected, JavaType.valueOf(byte[].class)));
	}
}
