package com.github.ljtfreitas.julian;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.Collection;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KindTest {

	@Test
	void simple() {
		Kind<String> kind = new Kind<>() {};

		JavaType javaType = kind.javaType();

		assertTrue(javaType.classType().isPresent());

		Class<?> classType = javaType.classType().get();

		assertEquals(String.class, classType);
	}

	@Test
	void array() {
		Kind<String[]> kind = new Kind<>() {};

		JavaType javaType = kind.javaType();

		assertTrue(javaType.classType().isPresent());

		Class<?> arrayType = javaType.classType().get();

		assertEquals(String[].class, arrayType);
	}

	@Nested
	class Parameterized {

		@Test
		void simple() {
			Kind<Collection<String>> kind = new Kind<>() {};

			JavaType javaType = kind.javaType();

			assertTrue(javaType.parameterized().isPresent());

			ParameterizedType parameterized = javaType.parameterized().get();

			assertAll(() -> assertEquals(Collection.class, parameterized.getRawType()),
					  () -> assertThat(parameterized.getActualTypeArguments(), arrayWithSize(1)),
					  () -> assertThat(parameterized.getActualTypeArguments(), hasItemInArray(String.class)));
		}

		@Nested
		class Bounds {

			@Test
			void upper() {
				Kind<Collection<? extends String>> kind = new Kind<>() {};

				JavaType javaType = kind.javaType();

				assertTrue(javaType.parameterized().isPresent());

				ParameterizedType parameterized = javaType.parameterized().get();

				assertAll(() -> assertEquals(Collection.class, parameterized.getRawType()),
						  () -> assertThat(parameterized.getActualTypeArguments(), arrayWithSize(1)),
						  () -> assertThat(asList(parameterized.getActualTypeArguments()), everyItem(instanceOf(WildcardType.class))));

				WildcardType argument = (WildcardType) parameterized.getActualTypeArguments()[0];

				assertAll(() -> assertThat(argument.getLowerBounds(), emptyArray()),
						  () -> assertThat(argument.getUpperBounds(), arrayWithSize(1)),
						  () -> assertThat(argument.getUpperBounds(), hasItemInArray(String.class)));
			}

			@Test
			void lower() {
				Kind<Collection<? super String>> kind = new Kind<>() {};

				JavaType javaType = kind.javaType();

				assertTrue(javaType.parameterized().isPresent());

				ParameterizedType parameterized = javaType.parameterized().get();

				assertAll(() -> assertEquals(Collection.class, parameterized.getRawType()),
						  () -> assertThat(parameterized.getActualTypeArguments(), arrayWithSize(1)),
						  () -> assertThat(asList(parameterized.getActualTypeArguments()), everyItem(instanceOf(WildcardType.class))));

				WildcardType argument = (WildcardType) parameterized.getActualTypeArguments()[0];

				assertAll(() -> assertThat(argument.getUpperBounds(), arrayWithSize(1)),
						  () -> assertThat(argument.getUpperBounds(), hasItemInArray(Object.class)),
						  () -> assertThat(argument.getLowerBounds(), arrayWithSize(1)),
						  () -> assertThat(argument.getLowerBounds(), hasItemInArray(String.class)));
			}
		}
	}
}
