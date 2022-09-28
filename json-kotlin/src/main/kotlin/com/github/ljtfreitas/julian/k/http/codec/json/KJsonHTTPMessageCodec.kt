package com.github.ljtfreitas.julian.k.http.codec.json

import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody
import com.github.ljtfreitas.julian.http.HTTPRequestBody
import com.github.ljtfreitas.julian.http.HTTPResponseBody
import com.github.ljtfreitas.julian.http.MediaType
import com.github.ljtfreitas.julian.http.codec.JsonHTTPMessageCodec
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodySubscribers
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.reflect.full.createType

@ExperimentalSerializationApi
fun jsonCodec(json: Json): JsonHTTPMessageCodec<Any> = KJsonHTTPMessageCodec(json)

@ExperimentalSerializationApi
class KJsonHTTPMessageCodec(private val json: Json = Json) : JsonHTTPMessageCodec<Any> {

    override fun writable(candidate: MediaType, javaType: JavaType) = supports(candidate)

    override fun write(body: Any, encoding: Charset): HTTPRequestBody = DefaultHTTPRequestBody(MediaType.APPLICATION_JSON) {
        serializer(body::class.createType())
            .let { json.encodeToString(serializer = it, value = body) }
            .let(BodyPublishers::ofString)
    }

    override fun readable(candidate: MediaType?, javaType: JavaType?) = supports(candidate)

    override fun read(body: HTTPResponseBody, javaType: JavaType): Optional<CompletableFuture<Any>> = body.content().map { publisher ->
        BodySubscribers.mapping(BodySubscribers.ofString(UTF_8)) { bodyAsString ->
            decode(bodyAsString, javaType)
        }.apply(publisher::subscribe)
        .body
        .toCompletableFuture()
    }

    private fun decode(bodyAsString: String, javaType: JavaType) : Any = serializer(javaType.get()).let {
        json.decodeFromString(deserializer = it, string = bodyAsString)
    }
}