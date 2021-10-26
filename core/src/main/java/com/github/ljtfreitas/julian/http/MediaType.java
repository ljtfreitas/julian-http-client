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

import com.github.ljtfreitas.julian.Content;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class MediaType implements Content {

	public static final MediaType ALL;
	public static final String ALL_VALUE = "*/*";

	public static final MediaType APPLICATION_ATOM_XML;
	public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

	public static final MediaType APPLICATION_CBOR;
	public static final String APPLICATION_CBOR_VALUE = "application/cbor";

	public static final MediaType APPLICATION_FORM_URLENCODED;
	public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	public static final MediaType APPLICATION_JSON;
	public static final String APPLICATION_JSON_VALUE = "application/json";

	public static final MediaType APPLICATION_JSON_UTF8;
	public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

	public static final MediaType APPLICATION_OCTET_STREAM;
	public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	public static final MediaType APPLICATION_PDF;
	public static final String APPLICATION_PDF_VALUE = "application/pdf";

	public static final MediaType APPLICATION_PROBLEM_JSON;
	public static final String APPLICATION_PROBLEM_JSON_VALUE = "application/problem+json";

	public static final MediaType APPLICATION_PROBLEM_JSON_UTF8;
	public static final String APPLICATION_PROBLEM_JSON_UTF8_VALUE = "application/problem+json;charset=UTF-8";

	public static final MediaType APPLICATION_PROBLEM_XML;
	public static final String APPLICATION_PROBLEM_XML_VALUE = "application/problem+xml";

	public static final MediaType APPLICATION_RSS_XML;
	public static final String APPLICATION_RSS_XML_VALUE = "application/rss+xml";

	public static final MediaType APPLICATION_NDJSON;
	public static final String APPLICATION_NDJSON_VALUE = "application/x-ndjson";

	public static final MediaType APPLICATION_STREAM_JSON;
	public static final String APPLICATION_STREAM_JSON_VALUE = "application/stream+json";

	public static final MediaType APPLICATION_XHTML_XML;
	public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

	public static final MediaType APPLICATION_XML;
	public static final String APPLICATION_XML_VALUE = "application/xml";

	public static final MediaType IMAGE_GIF;
	public static final String IMAGE_GIF_VALUE = "image/gif";

	public static final MediaType IMAGE_JPEG;
	public static final String IMAGE_JPEG_VALUE = "image/jpeg";

	public static final MediaType IMAGE_PNG;
	public static final String IMAGE_PNG_VALUE = "image/png";

	public static final MediaType MULTIPART_FORM_DATA;
	public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	public static final MediaType MULTIPART_MIXED;
	public static final String MULTIPART_MIXED_VALUE = "multipart/mixed";

	public static final MediaType MULTIPART_RELATED;
	public static final String MULTIPART_RELATED_VALUE = "multipart/related";

	public static final MediaType TEXT_EVENT_STREAM;
	public static final String TEXT_EVENT_STREAM_VALUE = "text/event-stream";

	public static final MediaType TEXT_HTML;
	public static final String TEXT_HTML_VALUE = "text/html";

	public static final MediaType TEXT_MARKDOWN;
	public static final String TEXT_MARKDOWN_VALUE = "text/markdown";

	public static final MediaType TEXT_PLAIN;
	public static final String TEXT_PLAIN_VALUE = "text/plain";

	public static final MediaType TEXT_XML;
	public static final String TEXT_XML_VALUE = "text/xml";

	static {
		ALL = new MediaType("*", "*");
		APPLICATION_ATOM_XML = new MediaType("application", "atom+xml");
		APPLICATION_CBOR = new MediaType("application", "cbor");
		APPLICATION_FORM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");
		APPLICATION_JSON = new MediaType("application", "json");
		APPLICATION_JSON_UTF8 = new MediaType("application", "json", Map.of("charset", StandardCharsets.UTF_8.name()));
		APPLICATION_NDJSON = new MediaType("application", "x-ndjson");
		APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");
		APPLICATION_PDF = new MediaType("application", "pdf");
		APPLICATION_PROBLEM_JSON = new MediaType("application", "problem+json");
		APPLICATION_PROBLEM_JSON_UTF8 = new MediaType("application", "problem+json", Map.of("charset", StandardCharsets.UTF_8.name()));
		APPLICATION_PROBLEM_XML = new MediaType("application", "problem+xml");
		APPLICATION_RSS_XML = new MediaType("application", "rss+xml");
		APPLICATION_STREAM_JSON = new MediaType("application", "stream+json");
		APPLICATION_XHTML_XML = new MediaType("application", "xhtml+xml");
		APPLICATION_XML = new MediaType("application", "xml");
		IMAGE_GIF = new MediaType("image", "gif");
		IMAGE_JPEG = new MediaType("image", "jpeg");
		IMAGE_PNG = new MediaType("image", "png");
		MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");
		MULTIPART_MIXED = new MediaType("multipart", "mixed");
		MULTIPART_RELATED = new MediaType("multipart", "related");
		TEXT_EVENT_STREAM = new MediaType("text", "event-stream");
		TEXT_HTML = new MediaType("text", "html");
		TEXT_MARKDOWN = new MediaType("text", "markdown");
		TEXT_PLAIN = new MediaType("text", "plain");
		TEXT_XML = new MediaType("text", "xml");
	}

	private final MimeType mediaType;

	private MediaType(String type, String subType) {
		this(type, subType, emptyMap());
	}

	private MediaType(String type, String subType, Map<String, String> parameters) {
		this.mediaType = new MimeType(type, subType, parameters);
	}

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
	public String show() {
		return mediaType.toString();
	}

	@Override
	public String toString() {
		return mediaType.toString();
	}

	public static MediaType valueOf(String value) {
		return new MediaType(MimeType.valueOf(value));
	}

	public static MediaType wildcard() {
		return ALL;
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
