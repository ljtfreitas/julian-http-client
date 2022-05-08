package com.github.ljtfreitas.julian.http.codec.form.multipart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryGenTest {

    @Test
    void random() {
        String boundary = BoundaryGen.RANDOM.run();

        assertNotNull(boundary);
    }
}