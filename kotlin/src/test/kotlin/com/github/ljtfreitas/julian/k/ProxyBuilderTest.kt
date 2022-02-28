package com.github.ljtfreitas.julian.k

import com.github.ljtfreitas.julian.ProxyBuilder
import com.github.ljtfreitas.julian.contract.Body
import com.github.ljtfreitas.julian.contract.GET
import com.github.ljtfreitas.julian.contract.JsonContent
import com.github.ljtfreitas.julian.contract.POST
import com.github.ljtfreitas.julian.http.HTTPStatusCode
import com.github.ljtfreitas.julian.http.MediaType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.extensions.mockserver.MockServerListener
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit

class ProxyBuilderTest : DescribeSpec({

    listener(MockServerListener(port = intArrayOf(8090)))

    val mockClient = MockServerClient("localhost", 8090)

    val spec : suspend (KtClient, DescribeSpecContainerScope) -> Unit = { ktClient, scope ->
        scope.apply {

            it("String should work, of course :)") {
                mockClient.`when`(request("/string")
                        .withMethod("GET"))
                    .respond(response()
                        .withBody("hello, kotlin"))

                ktClient.string() shouldBe "hello, kotlin"
            }

            it("and a nullable String?") {
                mockClient.`when`(request("/null-string")
                        .withMethod("GET"))
                    .respond(response()
                        .withStatusCode(204))

                ktClient.nullableString() shouldBe null
            }

            it("and Deferred<T>") {
                mockClient.`when`(request("/deferred")
                        .withMethod("GET"))
                    .respond(response()
                        .withBody("hello, deferred"))

                ktClient.deferredAsync().await() shouldBe "hello, deferred"
            }

            it("and Job") {
                mockClient.`when`(request("/job")
                        .withMethod("GET"))
                    .respond(response()
                        .withStatusCode(200))

                ktClient.job().join() shouldBe Unit
            }

            describe("kotlin codecs") {

                it("kotlin deserializer for json should be available") {
                    mockClient.`when`(request("/json")
                            .withMethod("GET"))
                        .respond(response()
                            .withBody("""{"name":"Tiago","age":36}""")
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))

                    ktClient.json() shouldBe Person(name = "Tiago", age = 36)
                }

                it("kotlin serializer for json should be available") {
                    mockClient.`when`(request("/json")
                            .withMethod("POST"))
                        .respond(response()
                            .withStatusCode(200))

                    ktClient.json(Person(name = "Tiago", age = 36)) shouldBe HTTPStatusCode.OK
                }
            }

            describe("coroutines") {

                val request = request("/suspend-string")
                    .withMethod("GET")

                beforeTest {
                    mockClient.clear(request)
                }

                it("running a suspend function") {
                    mockClient.`when`(request)
                        .respond(response()
                            .withBody("hello, suspend function"))

                    ktClient.suspendString() shouldBe "hello, suspend function"
                }

                it("...and a slow suspend function") {
                    mockClient.`when`(request)
                        .respond(response()
                            .withDelay(TimeUnit.MILLISECONDS, 5000)
                            .withBody("hello, suspend function"))

                    ktClient.suspendString() shouldBe "hello, suspend function"
                }
            }
        }
    }

    describe("Kotlin extensions for ProxyBuilder") {

        describe("enableKotlinExtensions should enable all Kotlin-related stuff") {
            val ktClient = ProxyBuilder()
                .enableKotlinExtensions()
                .build(KtClient::class.java, "http://localhost:8090")

            spec(ktClient, this)
        }

        describe("proxy<>() function should enable all Kotlin-related stuff") {
            val ktClient = proxy<KtClient>(endpoint = URL("http://localhost:8090"))

            spec(ktClient, this)
        }
    }
}) {

    interface KtClient {

        @GET("/string")
        fun string() : String

        @GET("/null-string")
        fun nullableString() : String?

        @GET("/suspend-string")
        suspend fun suspendString() : String

        @GET("/deferred")
        fun deferredAsync() : Deferred<String>

        @GET("/job")
        fun job() : Job

        @GET("/json")
        fun json(): Person

        @POST("/json")
        suspend fun json(@JsonContent person: Person): HTTPStatusCode
    }

    @Serializable
    data class Person(val name: String, val age: Int)
}