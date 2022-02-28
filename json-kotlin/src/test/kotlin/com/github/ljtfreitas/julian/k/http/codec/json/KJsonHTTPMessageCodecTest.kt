package com.github.ljtfreitas.julian.k.http.codec.json

import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.http.HTTPResponseBody
import com.github.ljtfreitas.julian.http.MediaType
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.Flow
import java.util.concurrent.Flow.Subscriber

@ExperimentalSerializationApi
class KJsonHTTPMessageCodecTest : DescribeSpec({

    val subject = KJsonHTTPMessageCodec()

    describe("encode/decode values to json with kotlinx.serialization.json") {

        it("encode") {
            val body = subject.write(Person(name = "Tiago de Freitas", age = 36), UTF_8)

            body.contentType().get() shouldBe MediaType.APPLICATION_JSON

            body.serialize().subscribe(object : Subscriber<ByteBuffer> {

                override fun onSubscribe(subscription: Flow.Subscription) {
                    subscription.request(1)
                }

                override fun onNext(item: ByteBuffer) {
                    val content = item.array().decodeToString()
                    content shouldBe """{"name":"Tiago de Freitas","age":36}"""
                }

                override fun onError(throwable: Throwable) {
                    fail(throwable.stackTraceToString())
                }

                override fun onComplete() {}
            })
        }

        it("decode") {
            val content = """{"name":"Tiago de Freitas","age":36}"""

            val person = subject.read(HTTPResponseBody.some(content.encodeToByteArray()), JavaType.valueOf(Person::class.java))
                .map { it.join() }
                .orElseThrow()

            person shouldBe Person(name = "Tiago de Freitas", age = 36)
        }
    }
}) {

    @Serializable
    data class Person(val name: String, val age: Int)
}