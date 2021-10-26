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

package com.github.ljtfreitas.julian.samples

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ljtfreitas.julian.ProxyBuilder
import com.github.ljtfreitas.julian.contract.Body
import com.github.ljtfreitas.julian.contract.Delete
import com.github.ljtfreitas.julian.contract.Get
import com.github.ljtfreitas.julian.contract.Path
import com.github.ljtfreitas.julian.contract.Post
import com.github.ljtfreitas.julian.contract.Put
import com.github.ljtfreitas.julian.http.HTTPResponse
import com.github.ljtfreitas.julian.http.HTTPStatus
import com.github.ljtfreitas.julian.http.codec.json.jackson.JacksonJsonHTTPMessageCodec
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import java.net.URL

data class Person(val name: String, val age: Int, val pets: List<String>)

@Path("/person")
interface PersonAPI {

    @Post
    fun create(@Body("application/json") person: Person): HTTPStatus

    @Get("/{id}")
    fun read(@Path(name = "id") id: Int): Person

    @Put("/{id}")
    fun update(@Path(name = "id") id: Int, @Body("application/json") person: Person): HTTPResponse<Person>

    @Delete("/{id}")
    fun delete(@Path(name = "id") id: Int): HTTPStatus

    companion object {

        fun build() = ProxyBuilder()
            .codecs()
                .add(JacksonJsonHTTPMessageCodec<Any>(jacksonObjectMapper()))
                .and()
            .build(PersonAPI::class.java, URL("http://localhost:8090"))
    }
}

fun main() {
    val mockServer = ClientAndServer(8090)
    val personAPI = PersonAPI.build();

    mockServer.use { api ->
        api.`when`(request("/person")
                        .withMethod("POST"))
                .respond(response()
                        .withStatusCode(201))

        println("POST: ${personAPI.create(Person(name = "Tiago de Freitas Lima", age = 36, pets = listOf("Puka", "Hugo")))}")

        api.`when`(request("/person/1")
                .withMethod("GET"))
            .respond(response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("""{"name":"Tiago de Freitas Lima","age":36,"pets":["Puka","Hugo"]}"""))

        println("GET: ${personAPI.read(1)}")

        api.`when`(request("/person/1")
                .withMethod("PUT"))
            .respond(response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("""{"name":"Tiago de Freitas Lima","age":36,"pets":["Puka","Hugo"]}"""))

        println("PUT: ${personAPI.update(1, Person(name = "Tiago de Freitas Lima", age = 36, pets = listOf("Puka", "Hugo")))
            .let { "${it.status()} - ${it.body()}" }}")

        api.`when`(request("/person/1")
            .withMethod("DELETE"))
            .respond(response()
                .withStatusCode(200))

        println("DELETE: ${personAPI.delete(1)}")
    }
}
