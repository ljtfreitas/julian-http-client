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

package com.github.ljtfreitas.julian.http.codec;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.state;

public class ScalarHTTPMessageCodec implements HTTPRequestWriter<Object>, HTTPResponseReader<Object> {

	private static final ScalarHTTPMessageCodec SINGLE_INSTANCE = new ScalarHTTPMessageCodec();

	private static final Collection<MediaType> SCALAR_MEDIA_TYPES = List.of(MediaType.TEXT_PLAIN);
	private static final Set<Class<?>> SCALAR_TYPES = new HashSet<>();

	private final StringHTTPMessageCodec codec = StringHTTPMessageCodec.get();

	static {
		SCALAR_TYPES.add(byte.class);
		SCALAR_TYPES.add(short.class);
		SCALAR_TYPES.add(int.class);
		SCALAR_TYPES.add(long.class);
		SCALAR_TYPES.add(float.class);
		SCALAR_TYPES.add(double.class);
		SCALAR_TYPES.add(boolean.class);
		SCALAR_TYPES.add(char.class);
		SCALAR_TYPES.add(Byte.class);
		SCALAR_TYPES.add(Short.class);
		SCALAR_TYPES.add(Integer.class);
		SCALAR_TYPES.add(Long.class);
		SCALAR_TYPES.add(Float.class);
		SCALAR_TYPES.add(Double.class);
		SCALAR_TYPES.add(Boolean.class);
		SCALAR_TYPES.add(Character.class);
	}

	@Override
	public Collection<MediaType> contentTypes() {
		return SCALAR_MEDIA_TYPES;
	}

	@Override
	public boolean readable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && isScalarType(javaType);
	}

	private boolean isScalarType(JavaType javaType) {
		return SCALAR_TYPES.stream().anyMatch(javaType::is);
	}

	@Override
	public Object read(byte[] body, JavaType javaType) {
		String responseAsString = codec.read(body, javaType);

		return ScalarType.valueOf(javaType).convert(responseAsString);
	}

	@Override
	public boolean writable(MediaType candidate, JavaType javaType) {
		return supports(candidate) && isScalarType(javaType);
	}

	@Override
	public HTTPRequestBody write(Object body, Charset encoding) {
		return codec.write(body.toString(), encoding);
	}
	
	private enum ScalarType {
		BYTE(byte.class, Byte.class) {
			@Override
			public Object convert(String source) {
				return Byte.valueOf(source);
			}
		},
		SHORT(short.class, Short.class) {
			@Override
			public Object convert(String source) {
				return Short.valueOf(source);
			}
		},
		INTEGER(int.class, Integer.class) {
			@Override
			public Object convert(String source) {
				return Integer.valueOf(source);
			}
		},
		LONG(long.class, Long.class) {
			@Override
			public Object convert(String source) {
				return Long.valueOf(source);
			}
		},
		FLOAT(float.class, Float.class) {
			@Override
			public Object convert(String source) {
				return Float.valueOf(source);
			}
		},
		DOUBLE(double.class, Double.class) {
			@Override
			public Object convert(String source) {
				return Double.valueOf(source);
			}
		},
		BOOLEAN(boolean.class, Boolean.class) {
			@Override
			public Object convert(String source) {
				return Boolean.valueOf(source);
			}
		},
		CHARACTER(char.class, Character.class) {
			@Override
			public Object convert(String source) {
				return state(source, s -> s.length() == 1, () -> format("Character conversion requires a body response with length 1. But the current length is {0}.", source.length()))
						.charAt(0);
			}
		};

		private final Class<?> primitiveClassType;
		private final Class<?> wrapperClassType;

		ScalarType(Class<?> primitiveClassType, Class<?> wrapperClassType) {
			this.primitiveClassType = primitiveClassType;
			this.wrapperClassType = wrapperClassType;
			
		}

		boolean is(JavaType javaType) {
			return javaType.is(primitiveClassType) || javaType.is(wrapperClassType);
		}
		
		abstract Object convert(String source);

		private static ScalarType valueOf(JavaType javaType) {
			return Arrays.stream(ScalarType.values()).filter(s -> s.is(javaType))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException(format("Unsupported type: {0}", javaType)));
		}
	}

	public static ScalarHTTPMessageCodec get() {
		return SINGLE_INSTANCE;
	}
}
