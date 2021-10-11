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

package com.github.ljtfreitas.julian.http;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class MediaType {

	private static final MediaType WILDCARD_CONTENT_TYPE = MediaType.valueOf("*/*");

	private final MimeType mediaType;

	private MediaType(MimeType mediaType) {
		this.mediaType = nonNull(mediaType);
	}

	public boolean compatible(MediaType candidate) {
		return mediaType.compatible(candidate.mediaType);
	}

	public String mime() {
		return mediaType.value();
	}

	public Map<String, String> parameters() {
		return mediaType.parameters;
	}

	public Optional<String> parameter(String name) {
		return Optional.ofNullable(mediaType.parameters.get(name));
	}

	public MediaType parameter(String name, String value) {
		return new MediaType(mediaType.parameter(name, value));
	}

	@Override
	public int hashCode() {
		return Objects.hash(mediaType);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MediaType))
			return false;

		MediaType that = (MediaType) obj;

		return this.mediaType.equals(that.mediaType);
	}

	@Override
	public String toString() {
		return mediaType.toString();
	}

	public static MediaType valueOf(String value) {
		return new MediaType(MimeType.valueOf(value));
	}

	public static MediaType wildcard() {
		return WILDCARD_CONTENT_TYPE;
	}

	private static class MimeType {
		
		private final String type;
		private final String subType;
		private final Map<String, String> parameters;

		private MimeType(String type, String subType, Map<String, String> parameters) {
			this.type = nonNull(type);
			this.subType = nonNull(subType);
			this.parameters = parameters;
		}
		
		boolean compatible(MimeType mimeType) {
			return equals(mimeType)
				|| (Wildcard.is(type) || Wildcard.is(mimeType.type))
				|| (Type.compatible(this.type, mimeType.type) && SubType.compatible(this.subType, mimeType.subType));
		}

		MimeType parameter(String name, String value) {
			Map<String, String> parameters = new LinkedHashMap<>(this.parameters);
			parameters.put(name, value);
			return new MimeType(this.type, this.subType, parameters);
		}

		String value() {
			return format("{0}/{1}", type, subType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, subType);
		}

		@Override
		public boolean equals(Object obj) {
			if (! (obj instanceof MimeType)) return false;
			
			MimeType that = (MimeType) obj;

			return Objects.equals(this.type, that.type)
				&& Objects.equals(this.subType, that.subType);
		}

		@Override
		public String toString() {
			return parameters.isEmpty() ? format("{0}/{1}", type, subType) 
										: format("{0}/{1}; {2}", type, subType, parameters.entrySet().stream()
												.map(e -> e.getKey() + "=" + e.getValue())
												.collect(joining(";")));
			
		}

		static MimeType valueOf(String value) {
			String[] values = value.split(";");

			String type = values[0].substring(0, values[0].indexOf("/")).toLowerCase();
			String subtype = values[0].substring(values[0].indexOf("/") + 1).toLowerCase();

			Map<String, String> parameters = Arrays.stream(Arrays.copyOfRange(values, 1, values.length))
					.map(parameter -> parameter.split("="))
					.filter(parameter -> parameter.length == 2)
					.collect(toMap(parameter -> parameter[0].trim(), parameter -> parameter[1].trim(), (a, b) -> b, LinkedHashMap::new));

			return new MimeType(type, subtype, parameters);
		}
	}
	
	static class Wildcard {
		
		private static final String WILDCARD_SUFFIX = "+";
		private static final String WILDCARD_TYPE = "*";

		static boolean is(String value) {
			return WILDCARD_TYPE.equals(value);
		}

		static boolean subtype(String value) {
			return is(value) || value.startsWith(WILDCARD_TYPE + WILDCARD_SUFFIX);
		}

		static boolean hasSuffix(String value) {
			return value.contains(WILDCARD_SUFFIX);
		}

		static String extractSuffix(String value) {
			return value.substring(value.indexOf(WILDCARD_SUFFIX) + 1);
		}

		static String removeSuffix(String value) {
			return value.substring(0, value.indexOf(WILDCARD_SUFFIX));
		}
	}

	static class Type {

		static boolean compatible(String source, String other) {
			return source.equalsIgnoreCase(other)
				|| (Wildcard.is(source) || Wildcard.is(other));
		}
	}

	static class SubType {

		static boolean compatible(String source, String other) {
			if (source.equalsIgnoreCase(other)) {
				return true;

			} else if (Wildcard.subtype(source) || Wildcard.subtype(other)) {
				if (!Wildcard.hasSuffix(source) && !Wildcard.hasSuffix(other)) {
					return true;

				} else if (Wildcard.hasSuffix(source) && Wildcard.hasSuffix(other)) {
					boolean areSameSuffix = Wildcard.extractSuffix(source).equalsIgnoreCase(Wildcard.extractSuffix(other));

					return areSameSuffix 
						&& (Wildcard.is(Wildcard.removeSuffix(source)) || Wildcard.is(Wildcard.removeSuffix(other)));
				}
			}

			return false;
		}
	}
}
