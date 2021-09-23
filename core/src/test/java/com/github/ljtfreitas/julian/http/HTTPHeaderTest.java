package com.github.ljtfreitas.julian.http;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class HTTPHeaderTest {

    @Test
    void join() {
        HTTPHeader header = new HTTPHeader("whatever", "value1");

        HTTPHeader newHeader = header.join(new HTTPHeader("whatever", "value2"));

        assertAll(() -> assertThat(newHeader.values(), contains("value1", "value2")),
                  () -> assertThat(newHeader.toString(), equalTo("whatever: value1, value2")));

    }
}