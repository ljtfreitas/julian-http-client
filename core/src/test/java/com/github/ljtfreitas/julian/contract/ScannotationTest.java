package com.github.ljtfreitas.julian.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Optional;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScannotationTest {

    @Test
    void scanAll() {
        Scannotation scannotation = new Scannotation(Whatever.class);

        Collection<CanRepeat> annotations = scannotation.scan(CanRepeat.class).collect(toUnmodifiableList());

        assertThat(annotations, hasSize(2));
    }

    @Test
    void scanOne() throws NoSuchMethodException {
        Scannotation scannotation = new Scannotation(Whatever.class.getMethod("method1"));

        Optional<Single> annotation = scannotation.find(Single.class);

        assertTrue(annotation.isPresent());
    }

    @Nested
    class MetaAnnotions {

        @Test
        void one() throws NoSuchMethodException {
            Scannotation scannotation = new Scannotation(Whatever.class.getMethod("method2"));

            Collection<Annotation> annotations = scannotation.meta(Single.class).collect(toUnmodifiableList());

            assertThat(annotations, contains(instanceOf(Meta.class)));
        }

        @Test
        void deep() throws NoSuchMethodException {
            Scannotation scannotation = new Scannotation(Whatever.class.getMethod("method3"));

            Collection<Annotation> annotations = scannotation.meta(Single.class).collect(toUnmodifiableList());

            assertThat(annotations, contains(instanceOf(Meta.class)));
        }
    }

    @CanRepeat
    @CanRepeat
    interface Whatever {

        @Single
        void method1();

        @Meta
        void method2();

        @DeepMeta
        void method3();
    }

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Single {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Container {

        CanRepeat[] value();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Container.class)
    @interface CanRepeat {}

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Single
    @interface Meta {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Meta
    @interface DeepMeta {}
}