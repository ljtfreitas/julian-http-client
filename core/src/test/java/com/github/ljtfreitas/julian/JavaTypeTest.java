package com.github.ljtfreitas.julian;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaTypeTest {

	@Nested
	class GenericDeclarations {

		@Nested
		class WithUnknownType {
			
			@Test
			void typeVariable() throws Exception {
				Method method = SimpleParameterizedType.class.getMethod("get");

				JavaType javaType = JavaType.valueOf(SimpleParameterizedType.class, method.getGenericReturnType());

				assertTrue(javaType.typeVariable().isPresent());
				
				TypeVariable<GenericDeclaration> typeVariable = javaType.typeVariable().get();

				assertAll(() -> assertSame(SimpleParameterizedType.class, typeVariable.getGenericDeclaration()),
						  () -> assertThat(typeVariable.getBounds(), arrayContaining(Object.class)));
			}
			
			@SuppressWarnings("unchecked")
			@Test
			void array() throws Exception {
				Method method = SimpleParameterizedType.class.getMethod("array");
				
				JavaType javaType = JavaType.valueOf(SimpleParameterizedType.class, method.getGenericReturnType());

				assertTrue(javaType.genericArray().isPresent());
				assertTrue(javaType.genericArray().get().getGenericComponentType() instanceof TypeVariable);

				TypeVariable<GenericDeclaration> typeVariable = (TypeVariable<GenericDeclaration>) javaType.genericArray().get().getGenericComponentType();

				assertAll(() -> assertSame(SimpleParameterizedType.class, typeVariable.getGenericDeclaration()),
						  () -> assertThat(typeVariable.getBounds(), arrayContaining(Object.class)));
			}

			@SuppressWarnings("unchecked")
			@Test
			void wildcard() throws Exception {
				Method method = SimpleParameterizedType.class.getMethod("wildcard");
				
				JavaType javaType = JavaType.valueOf(SimpleParameterizedType.class, method.getGenericReturnType());
				
				assertTrue(javaType.typeVariable().isPresent());
				
				TypeVariable<GenericDeclaration> typeVariable = javaType.typeVariable().get();
				
				assertAll(() -> assertSame(method, typeVariable.getGenericDeclaration()),
						  () -> assertThat(typeVariable.getBounds(), arrayWithSize(1)),
						  () -> assertThat(asList(typeVariable.getBounds()), everyItem(instanceOf(TypeVariable.class))));

				TypeVariable<GenericDeclaration> bound = (TypeVariable<GenericDeclaration>) typeVariable.getBounds()[0];
				
				assertAll(() -> assertSame(SimpleParameterizedType.class, bound.getGenericDeclaration()),
						() -> assertThat(bound.getBounds(), arrayContaining(Object.class)));
			}

			@Nested
			class Parameterized {
				
				@SuppressWarnings("unchecked")
				@Test
				void simple() throws Exception {
					Method method = SimpleParameterizedType.class.getMethod("all");
	
					JavaType javaType = JavaType.valueOf(SimpleParameterizedType.class, method.getGenericReturnType());
	
					assertTrue(javaType.parameterized().isPresent());
					
					ParameterizedType parameterized = javaType.parameterized().get();
	
					assertAll(() -> assertEquals(Collection.class, parameterized.getRawType()),
							  () -> assertThat(parameterized.getActualTypeArguments(), arrayWithSize(1)),
							  () -> assertThat(asList(parameterized.getActualTypeArguments()), everyItem(instanceOf(TypeVariable.class))));
	
					TypeVariable<GenericDeclaration> argument = (TypeVariable<GenericDeclaration>) parameterized.getActualTypeArguments()[0];
					
					assertAll(() -> assertSame(SimpleParameterizedType.class, argument.getGenericDeclaration()),
							  () -> assertThat(argument.getBounds(), arrayContaining(Object.class)));
				}

				@Nested
				class Bounds {

					@SuppressWarnings("unchecked")
					@Test
					void upper() throws Exception {
						Method method = SimpleParameterizedType.class.getMethod("upperBound");

						JavaType javaType = JavaType.valueOf(SimpleParameterizedType.class, method.getGenericReturnType());

						assertTrue(javaType.parameterized().isPresent());

						ParameterizedType parameterized = javaType.parameterized().get();

						assertAll(() -> assertEquals(Collection.class, parameterized.getRawType()),
								  () -> assertThat(parameterized.getActualTypeArguments(), arrayWithSize(1)),
								  () -> assertThat(asList(parameterized.getActualTypeArguments()), everyItem(instanceOf(WildcardType.class))));

						WildcardType argument = (WildcardType) parameterized.getActualTypeArguments()[0];

						assertAll(() -> assertThat(argument.getLowerBounds(), emptyArray()),
								  () -> assertThat(argument.getUpperBounds(), arrayWithSize(1)),
								  () -> assertThat(asList(argument.getUpperBounds()), everyItem(instanceOf(TypeVariable.class))));
						
						TypeVariable<GenericDeclaration> upperBound = (TypeVariable<GenericDeclaration>) argument.getUpperBounds()[0];
						
						assertAll(() -> assertSame(SimpleParameterizedType.class, upperBound.getGenericDeclaration()),
								  () -> assertThat(upperBound.getBounds(), arrayContaining(Object.class)));
					}

					@SuppressWarnings("unchecked")
					@Test
					void lower() throws Exception {
						Method method = SimpleParameterizedType.class.getMethod("lowerBound");

						JavaType javaType = JavaType.valueOf(SimpleParameterizedType.class, method.getGenericReturnType());

						assertTrue(javaType.parameterized().isPresent());

						ParameterizedType parameterized = javaType.parameterized().get();

						assertAll(() -> assertEquals(Collection.class, parameterized.getRawType()),
								  () -> assertThat(parameterized.getActualTypeArguments(), arrayWithSize(1)),
								  () -> assertThat(asList(parameterized.getActualTypeArguments()), everyItem(instanceOf(WildcardType.class))));

						WildcardType argument = (WildcardType) parameterized.getActualTypeArguments()[0];

						assertAll(() -> assertThat(argument.getUpperBounds(), arrayWithSize(1)),
								  () -> assertThat(argument.getUpperBounds(), hasItemInArray(Object.class)),
								  () -> assertThat(argument.getLowerBounds(), arrayWithSize(1)),
								  () -> assertThat(asList(argument.getLowerBounds()), everyItem(instanceOf(TypeVariable.class))));
						
						TypeVariable<GenericDeclaration> upperBound = (TypeVariable<GenericDeclaration>) argument.getLowerBounds()[0];
						
						assertAll(() -> assertSame(SimpleParameterizedType.class, upperBound.getGenericDeclaration()),
								  () -> assertThat(upperBound.getBounds(), arrayContaining(Object.class)));
					}
				}
			}
		}
		
		@Nested
		class WithKnownType {
			
			@Test
			void typeVariable() throws Exception {
				Method method = SpecificType.class.getMethod("get");

				JavaType javaType = JavaType.valueOf(SpecificType.class, method.getGenericReturnType());

				assertTrue(javaType.classType().isPresent());

				Class<?> classType = javaType.classType().get();

				assertEquals(String.class, classType);
			}

			@Test
			void array() throws Exception {
				Method method = SpecificType.class.getMethod("array");
				
				JavaType javaType = JavaType.valueOf(SpecificType.class, method.getGenericReturnType());

				assertTrue(javaType.genericArray().isPresent());

				GenericArrayType arrayType = javaType.genericArray().get();

				assertEquals(String.class, arrayType.getGenericComponentType());
			}

			@Test
			void wildcard() throws Exception {
				Method method = SpecificType.class.getMethod("wildcard");

				JavaType javaType = JavaType.valueOf(SpecificType.class, method.getGenericReturnType());

				assertTrue(javaType.typeVariable().isPresent());

				TypeVariable<GenericDeclaration> typeVariable = javaType.typeVariable().get();

				assertAll(() -> assertSame(method, typeVariable.getGenericDeclaration()),
						  () -> assertThat(typeVariable.getBounds(), arrayWithSize(1)),
						  () -> assertThat(typeVariable.getBounds(), hasItemInArray(String.class)));
			}

			@Nested
			class Parameterized {
			
				@Test
				void simple() throws Exception {
					Method method = SpecificType.class.getMethod("all");
	
					JavaType javaType = JavaType.valueOf(SpecificType.class, method.getGenericReturnType());
	
					assertTrue(javaType.parameterized().isPresent());
					
					ParameterizedType parameterized = javaType.parameterized().get();
	
					assertAll(() -> assertEquals(Collection.class, parameterized.getRawType()),
							  () -> assertThat(parameterized.getActualTypeArguments(), arrayWithSize(1)),
							  () -> assertThat(parameterized.getActualTypeArguments(), hasItemInArray(String.class)));
				}

				@Nested
				class Bounds {

					@Test
					void upper() throws Exception {
						Method method = SpecificType.class.getMethod("upperBound");

						JavaType javaType = JavaType.valueOf(SpecificType.class, method.getGenericReturnType());

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
					void lower() throws Exception {
						Method method = SpecificType.class.getMethod("lowerBound");

						JavaType javaType = JavaType.valueOf(SpecificType.class, method.getGenericReturnType());

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
	}

	@Nested
	class Creation {
		
		@Test
		void simple() {
			JavaType javaType = JavaType.valueOf(String.class);

			assertAll(() -> assertTrue(javaType.is(String.class)),
					  () -> assertTrue(javaType.classType().isPresent()));
		}

		@Test
		void parameterized() {
			JavaType javaType = JavaType.valueOf(JavaType.Parameterized.valueOf(Collection.class, String.class));

			assertAll(() -> assertTrue(javaType.is(Collection.class)),
					  () -> assertTrue(javaType.parameterized().isPresent()));

			ParameterizedType parameterized = javaType.parameterized().get();

			assertAll(() -> assertEquals(Collection.class, parameterized.getRawType()),
					  () -> assertThat(parameterized.getActualTypeArguments(), arrayContaining(String.class)));
		}

		@Test
		void array() {
			JavaType javaType = JavaType.valueOf(String[].class);

			assertAll(() ->  assertTrue(javaType.is(String[].class)),
					  () ->  assertTrue(javaType.classType().isPresent()));

			Class<?> arrayType = javaType.classType().get();

			assertTrue(arrayType.isArray());
		}
	}

	interface SimpleParameterizedType<T> {
		T get();

		Collection<T> all();
		
		T[] array();

		Collection<? extends T> upperBound();
		
		Collection<? super T> lowerBound();
		
		<E extends T> E wildcard();
	}

	interface SpecificType extends SimpleParameterizedType<String> {
	}
}
