package com.github.ljtfreitas.julian.k.http.client.ktor

import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody
import com.github.ljtfreitas.julian.http.HTTPHeader
import com.github.ljtfreitas.julian.http.HTTPHeaders
import com.github.ljtfreitas.julian.http.HTTPMethod
import com.github.ljtfreitas.julian.http.HTTPRequestBody
import com.github.ljtfreitas.julian.http.HTTPRequestDefinition
import com.github.ljtfreitas.julian.http.HTTPResponseBody
import com.github.ljtfreitas.julian.http.HTTPStatusCode
import com.github.ljtfreitas.julian.http.MediaType.TEXT_PLAIN
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.mockserver.MockServerListener
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.http
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.network.tls.addKeyStore
import kotlinx.coroutines.DelicateCoroutinesApi
import org.mockserver.client.MockServerClient
import org.mockserver.logging.MockServerLogger
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import org.mockserver.model.NottableString
import org.mockserver.socket.tls.KeyStoreFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

typealias CompletableString = CompletableFuture<String>

private fun HTTPResponseBody.readAsString() = readAsBytes(ByteArray::decodeToString).map(CompletableString::join).orElse("")

@OptIn(DelicateCoroutinesApi::class)
class KtorHTTPClientTest : DescribeSpec({

	listener(MockServerListener(port = intArrayOf(8090, 8094)))

	val mockServer = MockServerClient("localhost", 8090)

	val client = KtorHTTPClient()

	describe("KtorHTTPClient implementation") {

		describe("HTTP methods") {

			val expectedResponse = response("it works!").withContentType(MediaType.TEXT_PLAIN)

			val requestBodyAsString = "{\"message\":\"hello\"}"

			val requests = listOf(
				request("/get").withMethod("GET") to Request(
					path = URI("http://localhost:8090/get"),
					httpMethod = HTTPMethod.GET
				),
				request("/post").withMethod("POST").withBody(requestBodyAsString) to Request(
					path = URI("http://localhost:8090/post"),
					httpMethod = HTTPMethod.POST,
					headers = HTTPHeaders.create(HTTPHeader("Content-Type", "text/plain")),
					body = DefaultHTTPRequestBody(TEXT_PLAIN) { BodyPublishers.ofString(requestBodyAsString) },
					returnType = JavaType.valueOf(String::class.java)
				),
				request("/put").withMethod("PUT").withBody(requestBodyAsString) to Request(
					path = URI("http://localhost:8090/put"),
					httpMethod = HTTPMethod.PUT,
					headers = HTTPHeaders.create(HTTPHeader("Content-Type", "text/plain")),
					body = DefaultHTTPRequestBody(TEXT_PLAIN) { BodyPublishers.ofString(requestBodyAsString) },
					returnType = JavaType.valueOf(String::class.java)
				),
				request("/patch").withMethod("PATCH").withBody(requestBodyAsString) to Request(
					path = URI("http://localhost:8090/patch"),
					httpMethod = HTTPMethod.PATCH,
					headers = HTTPHeaders.create(HTTPHeader("Content-Type", "text/plain")),
					body = DefaultHTTPRequestBody(TEXT_PLAIN) { BodyPublishers.ofString(requestBodyAsString) },
					returnType = JavaType.valueOf(String::class.java)
				),
				request("/delete").withMethod("DELETE") to Request(
					path = URI("http://localhost:8090/delete"),
					httpMethod = HTTPMethod.DELETE
				)
			)

			requests.forEach { (expectedRequest, definition) ->

				it("HTTP Request: $expectedRequest and HTTP Response: $expectedResponse") {
					mockServer.`when`(expectedRequest).respond(expectedResponse)

					val response = client.request(definition).execute().join().unsafe()

					response.status().code() shouldBe expectedResponse.statusCode

					response.body().readAsString() shouldBe expectedResponse.bodyAsString
				}
			}
		}

		describe("HTTP headers") {
			val expectedResponse = response().withStatusCode(HTTPStatusCode.OK.value())
				.withHeader("X-Response-Header-1", "response-header-value-1")
				.withHeader("X-Response-Header-2", "response-header-value-2")
				.withHeader("X-Response-Header-3", "value1", "value2")

			val expectedHeaders = expectedResponse.headers.entries.fold(HTTPHeaders.empty()) { acc, header ->
				acc.join(HTTPHeader.create(header.name.value, header.values.map(NottableString::getValue)))
			}

			val requests = listOf(
				request("/headers").withMethod("GET")
					.withHeader("X-Whatever-1", "whatever-header-value-1")
					.withHeader("X-Whatever-2", "whatever-header-value-2")
					.withHeader("X-Whatever-3", "value1,value2") to Request(
						path = URI("http://localhost:8090/headers"),
						httpMethod = HTTPMethod.GET,
						headers = HTTPHeaders.create(
							HTTPHeader("X-Whatever-1", "whatever-header-value-1"),
							HTTPHeader("X-Whatever-2", "whatever-header-value-2"),
							HTTPHeader("X-Whatever-3", listOf("value1", "value2"))
						)
					)
			)

			requests.forEach { (expectedRequest, definition) ->

				it("HTTP Request: $expectedRequest and HTTP Response: $expectedResponse") {
					mockServer.`when`(expectedRequest).respond(expectedResponse)

					val response = client.request(definition).execute().join().unsafe()

					response.status().code() shouldBe expectedResponse.statusCode

					response.headers() shouldContainAll expectedHeaders.toList()
				}
			}

		}

		describe("HTTP body messages") {

			it("HTTP request body") {

				val requestBodyAsString = "{\"message\":\"hello\"}"
				val expectedResponse = "it works!"

				mockServer.`when`(
					request("/request-body")
						.withMethod("POST")
						.withBody(requestBodyAsString)
				).respond(
					response(expectedResponse)
						.withContentType(MediaType.TEXT_PLAIN)
				)

				val definition = Request(
					path = URI("http://localhost:8090/request-body"),
					httpMethod = HTTPMethod.POST,
					headers = HTTPHeaders.create(HTTPHeader("Content-Type", "text/plain")),
					body = DefaultHTTPRequestBody(TEXT_PLAIN) { BodyPublishers.ofString(requestBodyAsString) },
					returnType = JavaType.valueOf(String::class.java)
				)

				val response = client.request(definition).execute().join().unsafe()

				response.body().readAsString() shouldBe expectedResponse
			}

			it("HTTP response body") {
				val expectedResponse = "it works!"

				mockServer.`when`(
					request("/response-body")
						.withMethod("GET")
				).respond(
					response(expectedResponse)
						.withContentType(MediaType.TEXT_PLAIN)
				)

				val definition = Request(
					path = URI("http://localhost:8090/response-body"),
					httpMethod = HTTPMethod.GET
				)

				val response = client.request(definition).execute().join().unsafe()

				response.body().readAsString() shouldBe expectedResponse
			}
		}

		describe("Failures") {

			describe("HTTP response failures") {

				(400..599).map { code -> HTTPStatusCode.select(code).map { code to it.message() }.orElseGet { code to "unknown"} }
					.forEach { (statusCode, message) ->
						it("$statusCode $message") {

							mockServer.`when`(
								request("/status/$statusCode")
									.withMethod("GET")
							).respond(
								response().withStatusCode(statusCode)
									.withReasonPhrase(message)
									.withHeader("X-Whatever", "whatever")
							)

							val definition = Request(
								path = URI("http://localhost:8090/status/$statusCode"),
								httpMethod = HTTPMethod.GET
							)

							val response = client.request(definition).execute().join().unsafe()

							response.status().code() shouldBe statusCode
							response.status().message() shouldBe message

							response.headers() shouldContain HTTPHeader("X-Whatever", "whatever")
						}
					}
			}

			it("connection failures") {
				val definition = Request(path = URI("http://localhost:8099/hello"), httpMethod = HTTPMethod.GET)

				val response = client.request(definition).execute().join()

				response.onSuccess { fail("a connection error was expected here...") }
					.onFailure { it.shouldBeInstanceOf<IOException>() }
			}
		}

		describe("Customizations") {

			it("request timeout") {
				val clientWithTimeout = KtorHTTPClient {
					install(HttpTimeout) {
						requestTimeoutMillis = 2000
					}
				}

				mockServer.`when`(
					request("/timeout").withMethod("GET")
				).respond(
					response("it works!")
						.withDelay(TimeUnit.MILLISECONDS, 5000)
				)

				val definition = Request(path = URI("http://localhost:8090/timeout"), httpMethod = HTTPMethod.GET)

				val response = clientWithTimeout.request(definition).execute().join()

				response.onSuccess { fail("a timeout was expected here...") }
					.onFailure { it.shouldBeInstanceOf<HttpRequestTimeoutException>() }
			}

			it("SSL") {
				val httpsMockServer = MockServerClient("localhost", 8094)

				httpsMockServer.withSecure(true)
					.`when`(
						request("/secure").withMethod("GET")
					).respond(
						response("hello")
					)

				val keyStore = KeyStoreFactory(MockServerLogger()).loadOrCreateKeyStore()

				val clientWithSSL = KtorHTTPClient(CIO) {
					engine {
						https {
							this.addKeyStore(
								store = keyStore,
								password = KeyStoreFactory.KEY_STORE_PASSWORD.toCharArray(),
								alias = KeyStoreFactory.KEY_STORE_CERT_ALIAS
							)
						}
					}
				}

				val definition = Request(path = URI("http://localhost:8090/secure"), httpMethod = HTTPMethod.GET)

				val response = clientWithSSL.request(definition).execute().join().unsafe()

				response.body().readAsString() shouldBe "hello"
			}

			it("network proxy") {
				val clientWithProxy = KtorHTTPClient {
					install(Logging) {
						level = LogLevel.ALL
					}
					engine {
						proxy = ProxyBuilder.http("http://localhost:8090/")
					}
				}

				mockServer.`when`(
					request("/").withMethod("GET")
				).respond(
					response("proxy is working!")
				)

				val definition = Request(path = URI("http://www.google.com.br"), httpMethod = HTTPMethod.GET)

				val response = clientWithProxy.request(definition).execute().join().unsafe()

				response.body().readAsString() shouldBe "proxy is working!"
			}
		}
	}

}) {

	private data class Request(
		val path: URI,
		val httpMethod: HTTPMethod,
		val headers: HTTPHeaders = HTTPHeaders.empty(),
		val body: HTTPRequestBody? = null,
		val returnType: JavaType = JavaType.none()) : HTTPRequestDefinition {

		override fun returnType(): JavaType = returnType

		override fun path(): URI = path

		override fun method(): HTTPMethod = httpMethod

		override fun headers(): HTTPHeaders = headers

		override fun body(): Optional<HTTPRequestBody> = Optional.ofNullable(body)
	}
}