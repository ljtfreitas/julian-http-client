package com.github.ljtfreitas.julian.k

import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.Endpoint.Parameter
import com.github.ljtfreitas.julian.Endpoint.Parameters
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.JavaType.Wildcard
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class KFunctionCallbackResponseTTest : DescribeSpec({

    val subject = KFunctionCallbackResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for function callbacks") {

        describe("predicates") {

            describe("acceptable callbacks") {

                it("supports a Function1 as success callback") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "success", JavaType.parameterized(Function1::class.java,
                            Wildcard.lower(String::class.java)))
                    ))

                    subject.test(endpoint) shouldBe true
                }

                it("supports a Function1 as failure callback") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "failure", JavaType.parameterized(Function1::class.java,
                            Wildcard.lower(Throwable::class.java)))
                    ))

                    subject.test(endpoint) shouldBe true
                }

                it("supports a Result<T> as callback") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "result", JavaType.parameterized(Function1::class.java,
                            Wildcard.lower(JavaType.parameterized(Result::class.java, String::class.java).get())))
                    ))

                    subject.test(endpoint) shouldBe true
                }
            }

            describe("unacceptable callbacks") {

                it("doesn't support a Function0 as callback") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "callback", JavaType.valueOf(Function0::class.java))
                    ))

                    subject.test(endpoint) shouldBe false
                }

                it("doesn't support a FunctionN with more than 2 arguments") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "callback", JavaType.parameterized(Function3::class.java,
                            Wildcard.lower(JavaType.valueOf(String::class.java).get()),
                            Wildcard.lower(JavaType.valueOf(String::class.java).get()),
                            Wildcard.lower(JavaType.valueOf(Throwable::class.java).get())))
                    ))

                    subject.test(endpoint) shouldBe false
                }
            }
        }

        describe("adapt to expected type") {

            describe("acceptable function signatures") {

                it("adapts a success callback (a Function1) to argument type") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                            Parameter.callback(0, "success", JavaType.parameterized(
                                    Function1::class.java,
                                    Wildcard.lower(String::class.java)))
                    ))

                    subject.adapted(endpoint) shouldBe JavaType.valueOf(String::class.java)
                }

                it("adapts a failure callback (a Function1 parameterized with Throwable) to void") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "failure", JavaType.parameterized(
                            Function1::class.java,
                            Wildcard.lower(Throwable::class.java)))
                    ))

                    subject.adapted(endpoint) shouldBe JavaType.valueOf(Void.TYPE)
                }

                it("in case a function has two Function1 arguments, adapt using successful one") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "success", JavaType.parameterized(
                            Function1::class.java,
                            Wildcard.lower(String::class.java))),
                        Parameter.callback(0, "failure", JavaType.parameterized(
                            Function1::class.java,
                            Wildcard.lower(Throwable::class.java)))
                    ))

                    subject.adapted(endpoint) shouldBe JavaType.valueOf(String::class.java)
                }

                it("adapts a result callback to Result<T>") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "result", JavaType.parameterized(Function1::class.java,
                            Wildcard.lower(JavaType.parameterized(Result::class.java, String::class.java).get())))
                    ))

                    subject.adapted(endpoint) shouldBe JavaType.valueOf(String::class.java)
                }
            }
        }

        describe("bind") {

            describe("success") {

                val expected = "success"
                val successFn = mockk<(String) -> Unit>()

                val check: (String) -> Unit = { s ->
                    s shouldBe expected
                    successFn(s)
                }

                beforeEach{
                    clearMocks(successFn)

                    justRun { successFn(any()) }
                }

                it("a single success callback") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "success", JavaType.parameterized(Function1::class.java,
                            Wildcard.lower(String::class.java)))
                    ))

                    val fn = subject.bind<String>(endpoint, fn = ObjectResponseT<Any>().bind(endpoint, null))

                    fn.join(Promise.done(Response.done(expected)), Arguments.create(check))

                    verify { successFn(expected) }
                }

                it("when a function has two callbacks, uses the successful one") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "success", JavaType.parameterized(
                            Function1::class.java,
                            Wildcard.lower(String::class.java))),
                        Parameter.callback(1, "failure", JavaType.parameterized(
                            Function1::class.java,
                            Wildcard.lower(Throwable::class.java)))
                    ))

                    val failureFn = mockk<(Throwable) -> Unit>()

                    val fn = subject.bind<String>(endpoint, fn = ObjectResponseT<Any>().bind(endpoint, null))

                    fn.join(Promise.done(Response.done(expected)), Arguments.create(check, failureFn))

                    verify {
                        successFn(expected)
                        failureFn wasNot Called
                    }
                }
            }

            describe("failure") {

                val expected = RuntimeException("oooops")

                val failureFn = mockk<(Throwable) -> Unit>()

                val check: (Throwable) -> Unit = { e ->
                    e shouldBe expected
                    failureFn(e)
                }

                beforeEach {
                    clearMocks(failureFn)

                    justRun { failureFn(any()) }
                }

                it("a single failure callback") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "failure", JavaType.parameterized(
                            Function1::class.java,
                            Wildcard.lower(Throwable::class.java)))
                    ))

                    val fn = subject.bind<String>(endpoint, fn = ObjectResponseT<Any>().bind(endpoint, null))

                    fn.join(Promise.failed(expected), Arguments.create(check))

                    verify { failureFn(expected) }
                }

                it("when a function has two callbacks, uses the failure one") {
                    every { endpoint.parameters() } returns Parameters(listOf(
                        Parameter.callback(0, "success", JavaType.parameterized(
                            Function1::class.java,
                            Wildcard.lower(String::class.java))),
                        Parameter.callback(1, "failure", JavaType.parameterized(
                            Function1::class.java,
                            Wildcard.lower(Throwable::class.java)))
                    ))

                    val successFn = mockk<(String) -> Unit>()

                    val fn = subject.bind<String>(endpoint, fn = ObjectResponseT<Any>().bind(endpoint, null))

                    fn.join(Promise.failed(expected), Arguments.create(successFn, check))

                    verify {
                        failureFn(expected)
                        successFn wasNot Called
                    }
                }
            }

            describe("result callback (success or failure)") {

                every { endpoint.parameters() } returns Parameters(listOf(
                    Parameter.callback(0, "result", JavaType.parameterized(Function1::class.java,
                        Wildcard.lower(JavaType.parameterized(Result::class.java, String::class.java).get())))
                ))

                it("success") {
                    val expected = "success"

                    val resultFn = mockk<(Result<String>) -> Unit>()

                    val check: (Result<String>) -> Unit = { r ->
                        r shouldBeSuccess expected
                        resultFn(r)
                    }

                    justRun { resultFn(any()) }

                    val fn = subject.bind<String>(endpoint, fn = ObjectResponseT<Any>().bind(endpoint, null))

                    fn.join(Promise.done(Response.done(expected)), Arguments.create(check))

                    verify { resultFn(Result.success(expected)) }
                }

                it("failure") {
                    val expected = RuntimeException("oooops")

                    val callback = mockk<(Result<String>) -> Unit>()

                    val check: (Result<String>) -> Unit = { r ->
                        r.shouldBeFailure { it shouldBe expected }
                        callback(r)
                    }

                    justRun { callback(any()) }

                    val fn = subject.bind<String>(endpoint, fn = ObjectResponseT<Any>().bind(endpoint, null))

                    fn.join(Promise.failed(expected), Arguments.create(check))

                    verify { callback(Result.failure(expected)) }
                }
            }
        }
    }
})
