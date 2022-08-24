package com.github.ljtfreitas.julian.k.arrow.fx

import com.github.ljtfreitas.julian.k.coroutines.promise
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec

class ExtensionsTest : DescribeSpec({

    describe("ArrowFx (coroutines) extensions") {

        describe("Promise extensions") {

            it("Promise<T> -> Effect<Exception, T>") {
                val effect = promise { "hello" }.effect()

                val either = effect.toEither()

                either shouldBeRight "hello"
            }

            it("a failed Promise<T> -> a failed Effect<Exception, T>") {
                val failure = RuntimeException("oops")

                val effect = promise { throw failure }.effect()

                val either = effect.toEither()

                either shouldBeLeft failure
            }

            it("Promise<T> -> an effectful Either<Exception, T>") {
                val either = promise { "hello" }.effectAsEither()

                either shouldBeRight "hello"
            }

            it("a failed Promise<T> -> a failed, effectful Either<Exception, T> with the left side") {
                val failure = RuntimeException("oops")

                val either = promise { throw failure }.effectAsEither()

                either shouldBeLeft failure
            }
        }
    }
})
