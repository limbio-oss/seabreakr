/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.limb.seabreakr;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Based on the original code in Google GSON
 * Represents a generic type {@code T}.
 * <p>
 * You can use this class to get the generic type for a class. For example,
 * to get the generic type for <code>Collection&lt;Foo&gt;</code>, you can use:
 * <p>
 * <code>Type typeOfCollectionOfFoo = new ServiceType&lt;Collection&lt;Foo&gt;&gt;(){}.getType()
 * </code>
 * <p>
 * <p>Assumes {@code Type} implements {@code equals()} and {@code hashCode()}
 * as a value (as opposed to identity) comparison.
 * <p>
 * Also implements {@link #isAssignableFrom(Type)} to check type-safe
 * assignability.
 *
 * @author Bob Lee
 * @author Sven Mawson
 */
public abstract class ServiceType<T> {

    private final Class<? super T> rawType;
    private final Type type;

    /**
     * Constructs a new type token. Derives represented class from type
     * parameter.
     * <p>
     * <p>Clients create an empty anonymous subclass. Doing so embeds the type
     * parameter in the anonymous class's type hierarchy so we can reconstitute
     * it at runtime despite erasure.
     * <p>
     * <p>For example:
     * <code>
     * {@literal ServiceType<List<String>> t = new ServiceType<List<String>>}(){}
     * </code>
     */
    @SuppressWarnings({"unchecked"})
    protected ServiceType() {
        this.type = getSuperclassTypeParameter(getClass());
        this.rawType = (Class<? super T>) getRawType(type);
    }


    /**
     * Unsafe. Constructs a type token manually.
     */
    @SuppressWarnings({"unchecked"})
    private ServiceType(Type type) {
        this.rawType = (Class<? super T>) getRawType(type);
        this.type = type;
    }

    /**
     * Gets the raw type.
     */
    public Class<? super T> getRawType() {
        return rawType;
    }

    /**
     * Gets underlying {@code Type} instance.
     */
    public Type getType() {
        return type;
    }

    /**
     * Check if this type is assignable from the given class object.
     */
    public boolean isAssignableFrom(Class<?> type) {
        return isAssignableFrom((Type) type);
    }

    /**
     * Check if this type is assignable from the given Type.
     */
    public boolean isAssignableFrom(Type from) {
        if (from == null) {
            return false;
        }

        if (type.equals(from)) {
            return true;
        }

        if (type instanceof Class) {
            return rawType.isAssignableFrom(getRawType(from));

        } else if (type instanceof ParameterizedType) {
            return isAssignableFrom(from, (ParameterizedType) type, new HashMap<>());

        } else if (type instanceof GenericArrayType) {
            return rawType.isAssignableFrom(getRawType(from)) && isAssignableFrom(from, (GenericArrayType) type);

        } else {
            throw buildUnexpectedTypeError(type, Class.class, ParameterizedType.class, GenericArrayType.class);
        }
    }

    /**
     * Check if this type is assignable from the given type token.
     */
    public boolean isAssignableFrom(ServiceType<?> token) {
        return isAssignableFrom(token.getType());
    }

    /**
     * Hashcode for this object.
     *
     * @return hashcode for this object.
     */
    @Override
    public int hashCode() {
        return type.hashCode();
    }

    /**
     * Method to test equality.
     *
     * @return true if this object is logically equal to the specified object, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ServiceType<?>)) {
            return false;
        }
        ServiceType<?> t = (ServiceType<?>) o;
        return type.equals(t.type);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        return type instanceof Class<?> ? ((Class<?>) type).getName() : type.toString();
    }

    /**
     * Checks if two parameterized types are exactly equal, under the variable
     * replacement described in the typeVarMap.
     */
    private static boolean typeEquals(ParameterizedType from, ParameterizedType to, Map<String, Type> typeVarMap) {
        if (from.getRawType().equals(to.getRawType())) {
            Type[] fromArgs = from.getActualTypeArguments();
            Type[] toArgs = to.getActualTypeArguments();
            for (int i = 0; i < fromArgs.length; i++) {
                if (!matches(fromArgs[i], toArgs[i], typeVarMap)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if two types are the same or are equivalent under a variable mapping
     * given in the type map that was provided.
     */
    private static boolean matches(Type from, Type to, Map<String, Type> typeMap) {
        if (to.equals(from)) {
            return true;
        }

        return from instanceof TypeVariable && to.equals(typeMap.get(((TypeVariable<?>) from).getName()));
    }

    /**
     * Private helper function that performs some assignability checks for
     * the provided GenericArrayType.
     */
    private static boolean isAssignableFrom(Type from, GenericArrayType to) {
        Type toGenericComponentType = to.getGenericComponentType();
        if (toGenericComponentType instanceof ParameterizedType) {
            Type t = from;
            if (from instanceof GenericArrayType) {
                t = ((GenericArrayType) from).getGenericComponentType();
            } else if (from instanceof Class) {
                Class<?> classType = (Class<?>) from;
                while (classType.isArray()) {
                    classType = classType.getComponentType();
                }
                t = classType;
            }
            return isAssignableFrom(t, (ParameterizedType) toGenericComponentType, new HashMap<>());
        }
        // No generic defined on "to"; therefore, return true and let other
        // checks determine assignability
        return true;
    }

    /**
     * Private recursive helper function to actually do the type-safe checking
     * of assignability.
     */
    private static boolean isAssignableFrom(Type from, ParameterizedType to, Map<String, Type> typeVarMap) {
        if (from == null) {
            return false;
        }

        if (to.equals(from)) {
            return true;
        }

        // First figure out the class and any type information.
        Class<?> clazz = getRawType(from);
        ParameterizedType ptype = null;
        if (from instanceof ParameterizedType) {
            ptype = (ParameterizedType) from;
        }

        // Load up parameterized variable info if it was parameterized.
        if (ptype != null) {
            Type[] tArgs = ptype.getActualTypeArguments();
            TypeVariable<?>[] tParams = clazz.getTypeParameters();
            for (int i = 0; i < tArgs.length; i++) {
                Type arg = tArgs[i];
                TypeVariable<?> var = tParams[i];
                while (arg instanceof TypeVariable) {
                    TypeVariable<?> v = (TypeVariable<?>) arg;
                    arg = typeVarMap.get(v.getName());
                }
                typeVarMap.put(var.getName(), arg);
            }

            // check if they are equivalent under our current mapping.
            if (typeEquals(ptype, to, typeVarMap)) {
                return true;
            }
        }

        for (Type itype : clazz.getGenericInterfaces()) {
            if (isAssignableFrom(itype, to, new HashMap<>(typeVarMap))) {
                return true;
            }
        }

        // Interfaces didn't work, try the superclass.
        Type superType = clazz.getGenericSuperclass();
        return isAssignableFrom(superType, to, new HashMap<>(typeVarMap));
    }

    private static Type getSuperclassTypeParameter(Class<?> type) {
        Type superclass = type.getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new RuntimeException("Missing type parameter information");
        }
        return ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }

    private static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class.
            // Neal isn't either but suspects some pathological case related
            // to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
            throw buildUnexpectedTypeError(rawType, Class.class);

        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;

            // TODO(jleitch): This is not the most efficient way to handle generic
            // arrays, but is there another way to extract the array class in a
            // non-hacky way (i.e. using String value class names- "[L...")?
            Object rawArrayType = Array.newInstance(getRawType(genericArrayType.getGenericComponentType()), 0);
            return rawArrayType.getClass();
        } else {
            throw buildUnexpectedTypeError(type, ParameterizedType.class, GenericArrayType.class);
        }
    }

    private static AssertionError buildUnexpectedTypeError(Type token, Class<?>... expected) {
        // Build exception message
        StringBuilder exceptionMessage = new StringBuilder("Unexpected type. Expected one of: ");
        for (Class<?> clazz : expected) {
            exceptionMessage.append(clazz.getName()).append(", ");
        }
        exceptionMessage.append("but got: ").append(token.getClass().getName()) //
                .append(", for type token: ").append(token.toString()).append('.');

        return new AssertionError(exceptionMessage.toString());
    }

    static <T> ServiceType<T> serviceType(Class<T> type) {
        return new SimpleServiceType<>(type);
    }

    private static final class SimpleServiceType<T> extends ServiceType<T> {
        private SimpleServiceType(Type type) {
            super(type);
        }
    }
}
