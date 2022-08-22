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

package com.github.ljtfreitas.julian.k

import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.MethodEndpoint
import com.github.ljtfreitas.julian.ProxyBuilder
import com.github.ljtfreitas.julian.ProxyBuilderExtension
import com.github.ljtfreitas.julian.contract.EndpointMetadata
import com.github.ljtfreitas.julian.http.HTTPResponseFailure
import com.github.ljtfreitas.julian.http.HTTPStatusCode
import com.github.ljtfreitas.julian.http.HTTPStatusGroup
import com.github.ljtfreitas.julian.k.coroutines.DeferredResponseT
import com.github.ljtfreitas.julian.k.coroutines.FlowResponseT
import com.github.ljtfreitas.julian.k.coroutines.JobResponseT
import com.github.ljtfreitas.julian.k.http.codec.json.jsonCodec
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.lang.reflect.Method
import java.net.URL
import java.util.Objects
import java.util.Optional
import kotlin.coroutines.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.kotlinFunction

class ProxyBuilderKotlinExtension : ProxyBuilderExtension {

    override fun apply(builder: ProxyBuilder): ProxyBuilder = builder.enableKotlinExtensions()
}

fun ProxyBuilder.enableKotlinExtensions(): ProxyBuilder {
    contract {
        extensions {
            apply(::KExtensions)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    codecs {
        add(jsonCodec(Json))
    }

    responses {
        add(
            DeferredResponseT,
            FlowResponseT,
            JobResponseT,
            SequenceResponseT,
            KFunctionCallbackResponseT,
            SuspendKFunctionResponseT()
        )
    }

    return this
}

fun ProxyBuilder.contract(spec: ProxyBuilder.ContractSpec.() -> Unit): ProxyBuilder = contract().apply(spec).and()

fun ProxyBuilder.ContractSpec.extensions(spec: ProxyBuilder.ContractSpec.Extensions.() -> Unit): ProxyBuilder.ContractSpec = extensions().apply(spec).and()

fun ProxyBuilder.codecs(spec: ProxyBuilder.HTTPMessageCodecs.() -> Unit): ProxyBuilder = codecs().apply(spec).and()

fun ProxyBuilder.responses(spec: ProxyBuilder.ResponsesTs.() -> Unit): ProxyBuilder = responses().apply(spec).and()

fun ProxyBuilder.async(spec: ProxyBuilder.Async.() -> Unit): ProxyBuilder = async().apply(spec).and()

fun ProxyBuilder.http(spec: ProxyBuilder.HTTPSpec.() -> Unit): ProxyBuilder = http().apply(spec).and()

fun ProxyBuilder.HTTPSpec.client(spec: ProxyBuilder.HTTPSpec.HTTPClientSpec.() -> Unit): ProxyBuilder.HTTPSpec = client().apply(spec).and()

fun ProxyBuilder.HTTPSpec.HTTPClientSpec.extensions(spec: ProxyBuilder.HTTPSpec.HTTPClientSpec.Extensions.() -> Unit): ProxyBuilder.HTTPSpec.HTTPClientSpec = extensions().apply(spec).and()

fun ProxyBuilder.HTTPSpec.HTTPClientSpec.Extensions.debug(spec: ProxyBuilder.HTTPSpec.HTTPClientSpec.Extensions.Debug.() -> Unit): ProxyBuilder.HTTPSpec.HTTPClientSpec.Extensions = debug().apply(spec).and()

fun ProxyBuilder.HTTPSpec.HTTPClientSpec.configure(spec: ProxyBuilder.HTTPSpec.HTTPClientSpec.Configuration.() -> Unit): ProxyBuilder.HTTPSpec.HTTPClientSpec = configure().apply(spec).and()

fun ProxyBuilder.HTTPSpec.HTTPClientSpec.Configuration.redirects(spec: ProxyBuilder.HTTPSpec.HTTPClientSpec.Configuration.Redirects.() -> Unit): ProxyBuilder.HTTPSpec.HTTPClientSpec.Configuration = redirects().apply(spec).and()

fun ProxyBuilder.HTTPSpec.HTTPClientSpec.Configuration.ssl(spec: ProxyBuilder.HTTPSpec.HTTPClientSpec.Configuration.SSL.() -> Unit): ProxyBuilder.HTTPSpec.HTTPClientSpec.Configuration = ssl().apply(spec).and()

fun ProxyBuilder.HTTPSpec.interceptors(spec: ProxyBuilder.HTTPSpec.HTTPRequestInterceptors.() -> Unit): ProxyBuilder.HTTPSpec = interceptors().apply(spec).and()

fun ProxyBuilder.HTTPSpec.failure(spec: ProxyBuilder.HTTPSpec.HTTPResponseFailureSpec.() -> Unit): ProxyBuilder.HTTPSpec = failure().apply(spec).and()

@JvmName("httpResponseFailureHTTPStatusCode")
fun ProxyBuilder.HTTPSpec.HTTPResponseFailureSpec.`when`(status: Pair<HTTPStatusCode, HTTPResponseFailure>): ProxyBuilder.HTTPSpec.HTTPResponseFailureSpec = status.let { (code, failure) ->
    `when`(code, failure)
}

@JvmName("httpResponseFailureHTTPStatusGroup")
fun ProxyBuilder.HTTPSpec.HTTPResponseFailureSpec.`when`(statusGroup: Pair<HTTPStatusGroup, HTTPResponseFailure>): ProxyBuilder.HTTPSpec.HTTPResponseFailureSpec = statusGroup.let { (group, failure) ->
    `when`(group, failure)
}

fun ProxyBuilder.HTTPSpec.encoding(spec: ProxyBuilder.HTTPSpec.Encoding.() -> Unit): ProxyBuilder.HTTPSpec = encoding().apply(spec).and()

inline fun <reified T> ProxyBuilder.build(endpoint: String?) : T = build(T::class.java, endpoint)

inline fun <reified T> ProxyBuilder.build(endpoint: URL?) : T = build(T::class.java, endpoint)

inline fun <reified T> proxy(block: ProxyBuilder.() -> Unit = {}) : T = proxy(endpoint = null as URL?, block)

inline fun <reified T> proxy(endpoint: String? = null, block: ProxyBuilder.() -> Unit = {}) : T = proxy(endpoint?.let(::URL), block)

inline fun <reified T> proxy(endpoint: URL? = null, block: ProxyBuilder.() -> Unit = {}) : T = ProxyBuilder()
    .enableKotlinExtensions()
    .apply(block)
    .build(T::class.java, endpoint)

internal class KExtensions(private val m: EndpointMetadata) : EndpointMetadata by m {

    override fun endpoint(javaClass: Class<*>, javaMethod: Method, unhandledParameterTypes: MutableCollection<Class<*>>, root: Optional<URL>): Endpoint {
        val source = m.endpoint(javaClass, javaMethod, unhandledParameterTypes + unhandledKotlinTypes, root)
        return if (javaMethod.kotlinFunction?.isSuspend == true)
            SuspendKFunctionEndpoint(
                kFunction = javaMethod.kotlinFunction,
                javaMethod = javaMethod,
                endpoint = source
            )
        else source
    }

    companion object {
        val unhandledKotlinTypes = listOf(Continuation::class.java)
    }
}

class SuspendKFunctionEndpoint(private val kFunction: KFunction<*>?, private val javaMethod: Method, private val endpoint: Endpoint) : MethodEndpoint(endpoint, javaMethod) {

    private val continuationParameter = javaMethod.parameters
        .firstOrNull { Continuation::class.java.isAssignableFrom(it.type) }

    fun continuationArgumentType() = continuationParameter?.let { JavaType.valueOf(javaMethod.declaringClass, it.parameterizedType) }

    override fun equals(that: Any?): Boolean {
        if (this === that) return true

        return when(that) {
            is SuspendKFunctionEndpoint ->
                this.endpoint == that.endpoint && this.kFunction == that.kFunction && this.javaMethod == that.javaMethod
            is Endpoint ->
                this.endpoint == that
            else ->
                false
        }
    }

    override fun hashCode(): Int = Objects.hash(kFunction, javaMethod, endpoint)
}

