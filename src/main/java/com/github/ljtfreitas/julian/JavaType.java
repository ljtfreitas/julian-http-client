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

package com.github.ljtfreitas.julian;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.stream.Collectors.joining;

public class JavaType {

	private static final JavaType VOID_TYPE = JavaType.valueOf(void.class);

	private static final JavaType OBJECT_TYPE = JavaType.valueOf(Object.class);

	private final Type javaType;
	private final Class<?> rawClass;

	private JavaType(Type javaType) {
		this.javaType = nonNull(javaType);
		this.rawClass = Kind.of(javaType);
	}

	public boolean is(Class<?> candidate) {
		return rawClass.equals(candidate);
	}

	public boolean compatible(Class<?> candidate) {
		return candidate.isAssignableFrom(rawClass);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public Optional<Class<?>> classType() {
		return javaType instanceof Class ? Optional.of((Class) javaType) : Optional.empty();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public Optional<Class<?>> array() {
		return javaType instanceof Class && ((Class) javaType).isArray() ? Optional.of((Class) javaType) : Optional.empty();
	}

	public Optional<ParameterizedType> parameterized() {
		return javaType instanceof ParameterizedType ? Optional.of((ParameterizedType) javaType) : Optional.empty();
	}

	public Optional<GenericArrayType> genericArray() {
		return javaType instanceof GenericArrayType ? Optional.of((GenericArrayType) javaType) : Optional.empty();
	}

	public Optional<WildcardType> wildcard() {
		return javaType instanceof WildcardType ? Optional.of((WildcardType) javaType) : Optional.empty();
	}

	@SuppressWarnings("unchecked")
	public Optional<TypeVariable<GenericDeclaration>> typeVariable() {
		return javaType instanceof TypeVariable ? Optional.of((TypeVariable<GenericDeclaration>) javaType) : Optional.empty();
	}

	public <R> Optional<R> when(Class<?> candidate, Supplier<R> fn) {
		return is(candidate) ? Optional.ofNullable(fn.get()) : Optional.empty();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(javaType);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JavaType)) return false;
		if (obj == this) return true;

		JavaType that = (JavaType) obj;
		
		return this.javaType.equals(that.javaType);
	}

	@Override
	public String toString() {
		return javaType.toString();
	}
	
	public static JavaType valueOf(Type javaType) {
		return new JavaType(javaType);
	}

	public static JavaType none() {
		return VOID_TYPE;
	}
	
	public static JavaType object() {
		return OBJECT_TYPE;
	}

	public static JavaType valueOf(Class<?> context, Type javaType) {
		return new JavaType(Kind.resolve(context, javaType));
	}
	
	public static JavaType parameterized(Type rawType, Type... arguments) {
		return new JavaType(Parameterized.valueOf(rawType, arguments));
	}

	public static JavaType parameterized(Type rawType, JavaType... arguments) {
		return new JavaType(Parameterized.valueOf(rawType, Arrays.stream(arguments).map(a -> a.javaType).toArray(Type[]::new)));
	}
	
	public static JavaType genericArrayOf(Type arrayType) {
		return new JavaType(GenericArray.valueOf(arrayType));
	}

	static class Kind {

		static Class<?> of(Type javaType) {
			if (javaType instanceof Class) {
				return (Class<?>) javaType;

			} else if (javaType instanceof ParameterizedType) {
				ParameterizedType parameterizedType = (ParameterizedType) javaType;
				return of(parameterizedType.getRawType());

			} else if (javaType instanceof GenericArrayType) {
				GenericArrayType genericArrayType = (GenericArrayType) javaType;
				return Array.newInstance(of(genericArrayType.getGenericComponentType()), 0).getClass();

			} else if (javaType instanceof WildcardType) {
				WildcardType wildcardType = (WildcardType) javaType;
				return of(wildcardType.getUpperBounds()[0]);

			} else if (javaType instanceof TypeVariable) {
				return Object.class;

			} else {
				throw new IllegalArgumentException(javaType.toString());

			}
		}

		static Type resolve(Class<?> classType, Type javaType) {
			if (javaType instanceof TypeVariable) {
	            return typeVariable(classType, (TypeVariable<?>) javaType);

	        } else if (javaType instanceof ParameterizedType) {
	            return parameterizedType(classType, (ParameterizedType) javaType);

	        } else if (javaType instanceof Class && ((Class<?>) javaType).isArray()) {
	            return arrayClassType(classType, (Class<?>) javaType);

	        } else if (javaType instanceof GenericArrayType) {
	            return genericArrayType(classType, (GenericArrayType) javaType);

	        } else if (javaType instanceof WildcardType) {
	            return wildcardType(classType, (WildcardType) javaType);

	        } else {
	            return javaType;
	        }
		}
		
		private static Type wildcardType(Class<?> classType, WildcardType wildcardType) {
	        Type[] lowerBounds = Arrays.stream(wildcardType.getLowerBounds())
	                .map(t -> resolve(classType, t))
	                .toArray(Type[]::new);

	        Type[] upperBounds = Arrays.stream(wildcardType.getUpperBounds())
	        		.map(t -> resolve(classType, t))
	                .toArray(Type[]::new);

	        return new Wildcard(upperBounds, lowerBounds);
	    }

	    private static Type genericArrayType(Class<?> classType, GenericArrayType genericArrayType) {
	        Type componentType = genericArrayType.getGenericComponentType();
	        Type newComponentType = resolve(classType, componentType);
	        return componentType == newComponentType ? genericArrayType : new GenericArray(newComponentType);
	    }

	    private static Type arrayClassType(Class<?> classType, Class<?> arrayClassType) {
	        Type componentType = arrayClassType.getComponentType();
	        Type newComponentType = resolve(classType, componentType);
	        return componentType == newComponentType ? arrayClassType : new GenericArray(newComponentType);
	    }

	    private static Type parameterizedType(Class<?> classType, ParameterizedType parameterizedType) {
	        Type ownerType = parameterizedType.getOwnerType();
	        Type newOwnerType = resolve(classType, ownerType);

	        boolean changed = (ownerType != newOwnerType);

	        Type[] typeArguments = parameterizedType.getActualTypeArguments();

	        for (int position = 0; position < typeArguments.length; position++) {
	            Type resolvedTypeArgument = resolve(classType, typeArguments[position]);

	            if (resolvedTypeArgument != typeArguments[position]) {
	                if (!changed) {
	                    typeArguments = typeArguments.clone();
	                    changed = true;
	                }
	                typeArguments[position] = resolvedTypeArgument;
	            }
	        }

	        return changed ? new Parameterized(parameterizedType.getRawType(), newOwnerType, typeArguments) : parameterizedType;
	    }

	    @SuppressWarnings("unchecked")
	    private static Type typeVariable(Class<?> classType, TypeVariable<?> typeVariable) {
	        GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();

	        if (genericDeclaration instanceof Class) {
	            Class<?> declaredClassType = (Class<?>) genericDeclaration;
	            Type declaredType = doResolveGenericSuperType(classType, classType, declaredClassType);

	            if (declaredType instanceof ParameterizedType) {
	                return resolve(classType, ((ParameterizedType) declaredType)
	                								.getActualTypeArguments()[Arrays.asList(declaredClassType.getTypeParameters())
						                          	.indexOf(typeVariable)]);
	            }
	        } else if (genericDeclaration instanceof Method) {

	            Type[] newBounds = Arrays.stream(typeVariable.getBounds())
	                    .map(t -> resolve(classType, t))
	                    .toArray(Type[]::new);

	            return new MethodTypeVariable((TypeVariable<Method>) typeVariable, newBounds);
	        }

	        return typeVariable;
	    }

	    private static Type doResolveGenericSuperType(Type context, Class<?> contextClassType, Class<?> classType) {
	        if (context == classType) {
	            return context;
	        }

	        if (classType.isInterface()) {
	            for (int position = 0; position < contextClassType.getInterfaces().length; position++) {
	                Class<?> interfaceType = contextClassType.getInterfaces()[position];

	                if (interfaceType == classType) {
	                    return contextClassType.getGenericInterfaces()[position];

	                } else if (classType.isAssignableFrom(contextClassType.getInterfaces()[position])) {
	                    return doResolveGenericSuperType(contextClassType.getGenericInterfaces()[position], interfaceType, classType);
	                }
	            }
	        }

	        return classType;
	    }
	}

	public static class Parameterized implements ParameterizedType {

		private final Type rawType;
		private final Type ownerType;
		private final Type[] arguments;

		private Parameterized(Type rawType, Type ownerType, Type[] arguments) {
			this.rawType = rawType;
			this.ownerType = ownerType;
			this.arguments = arguments;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return arguments;
		}

		@Override
		public Type getRawType() {
			return rawType;
		}

		@Override
		public Type getOwnerType() {
			return ownerType;
		}
		
	    @Override
	    public int hashCode() {
	        return Objects.hash(rawType, ownerType, arguments);
	    }

	    @Override
	    public boolean equals(Object obj) {
	    	if (obj == this) return true;

	        if (obj instanceof ParameterizedType) {
	            ParameterizedType that = (ParameterizedType) obj;

	            return Objects.equals(rawType, that.getRawType())
	                && Objects.equals(ownerType, that.getOwnerType())
	                && Arrays.equals(arguments, that.getActualTypeArguments());

	        } else {
	            return false;
	        }
	    }
	    
	    @Override
	    public String toString() {
	    	return Message.format("{0}<{1}> (owned by {2})", rawType, Arrays.toString(arguments), rawType);
	    }

	    public static Type firstArg(ParameterizedType parameterizedType) {
	    	return parameterizedType.getActualTypeArguments()[0];
	    }

	    static ParameterizedType valueOf(Type rawType, Type... arguments) {
			return new Parameterized(rawType, null, arguments);
		}
		
	}

	public static class Wildcard implements WildcardType {

		private final Type[] upperBounds;
		private final Type[] lowerBounds;

		public Wildcard(Type[] upperBounds, Type[] lowerBounds) {
			this.upperBounds = upperBounds;
			this.lowerBounds = lowerBounds;
		}

		@Override
		public Type[] getUpperBounds() {
			return upperBounds;
		}

		@Override
		public Type[] getLowerBounds() {
			return lowerBounds;
		}

		@Override
		public int hashCode() {
			return Objects.hash(upperBounds, lowerBounds);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;

			if (obj instanceof WildcardType) {
				WildcardType that = (WildcardType) obj;

				return Arrays.equals(upperBounds, that.getUpperBounds())
					&& Arrays.equals(lowerBounds, that.getLowerBounds());

			} else {
				return false;
			}
		}
		
		@Override
		public String toString() {
			return Message.format("? extends {0}, ? super {1}", Arrays.stream(upperBounds).map(Type::toString).collect(joining(",")), 
																Arrays.stream(lowerBounds).map(Type::toString).collect(joining(",")));
		}

		public static WildcardType upper(Type...bounds) {
			return new Wildcard(bounds, new Type[0]);
		}
	}
	
	static class GenericArray implements GenericArrayType {

	    private final Type componentType;

	    public GenericArray(Type componentType) {
	        this.componentType = componentType;
	    }

		@Override
	    public Type getGenericComponentType() {
	        return componentType;
	    }

	    @Override
	    public int hashCode() {
	        return Objects.hash(componentType);
	    }

	    @Override
	    public boolean equals(Object obj) {
	    	if (obj == this) return true;

	        if (obj instanceof GenericArrayType) {
	            return Objects.equals(componentType, ((GenericArrayType) obj).getGenericComponentType());

	        } else {
	            return super.equals(obj);
	        }
	    }
	    
	    @Override
	    public String toString() {
	    	return componentType + "[]";
	    }
	    
	    static GenericArrayType valueOf(Type arrayType) {
			return new GenericArray(arrayType);
		}
	}
	
	static class MethodTypeVariable implements TypeVariable<Method> {

	    private final TypeVariable<Method> source;
	    private final Type[] bounds;

	    MethodTypeVariable(TypeVariable<Method> source, Type[] bounds) {
	        this.source = source;
	        this.bounds = bounds;
	    }

	    @Override
	    public Type[] getBounds() {
	        return bounds;
	    }

	    @Override
	    public Method getGenericDeclaration() {
	        return source.getGenericDeclaration();
	    }

	    @Override
	    public String getName() {
	        return source.getName();
	    }

	    @Override
	    public AnnotatedType[] getAnnotatedBounds() {
	        return source.getAnnotatedBounds();
	    }

	    @Override
	    public <T extends Annotation> T getAnnotation(Class<T> aClass) {
	        return source.getAnnotation(aClass);
	    }

	    @Override
	    public Annotation[] getAnnotations() {
	        return source.getAnnotations();
	    }

	    @Override
	    public Annotation[] getDeclaredAnnotations() {
	        return source.getDeclaredAnnotations();
	    }
	}
}
