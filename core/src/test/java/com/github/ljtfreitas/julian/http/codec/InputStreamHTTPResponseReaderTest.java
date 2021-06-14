package com.github.ljtfreitas.julian.http.codec;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.ljtfreitas.julian.Bracket;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.codec.ContentType;
import com.github.ljtfreitas.julian.http.codec.InputStreamHTTPResponseReader;

class InputStreamHTTPResponseReaderTest {

	InputStreamHTTPResponseReader reader = new InputStreamHTTPResponseReader();

	@Nested
	class Readable {
		
		@Test
		void supported() {
			assertTrue(reader.readable(ContentType.valueOf("text/plain"), JavaType.valueOf(InputStream.class)));
		}
	
		@Test
		void unsupported() {
			assertFalse(reader.readable(ContentType.valueOf("text/plain"), JavaType.valueOf(String.class)));
		}
	}
	
	@Test
	void read() {
		String expected = "response body";

		InputStream in = reader.read(new ByteArrayInputStream(expected.getBytes()), JavaType.valueOf(InputStream.class));

		String actual = Bracket.acquire(( ) -> new BufferedReader(new InputStreamReader(in)))
			.map(r -> r.lines().collect(joining()))
			.unsafe();
		
		assertEquals(expected, actual);
	}
}
