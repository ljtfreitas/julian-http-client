package com.github.ljtfreitas.julian.k.arrow

import com.github.ljtfreitas.julian.ResponseT
import com.github.ljtfreitas.julian.k.arrow.fx.EffectResponseTProxy
import com.github.ljtfreitas.julian.spi.Plugins
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.sequences.shouldContainAll
import kotlin.streams.asSequence

class SPITest : DescribeSpec({

    describe("ServiceLoader objects from Arrow extensions") {

        it("All implementations should be provided by Java's ServiceLoader") {
            val expected = listOf(
                EitherResponseTProxy::class.java,
                EffectResponseTProxy::class.java,
                EvalResponseTProxy::class.java,
                NonEmptyListResponseTProxy::class.java,
                OptionResponseTProxy::class.java,
                RecoverableEitherResponseTProxy::class.java,
            )

            val plugins = Plugins()

            val sequence = plugins.all(ResponseT::class.java).map { it::class.java }.asSequence()

            sequence shouldContainAll expected
        }
    }
})