package com.dslplatform.json.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dslplatform.json.Nullable;

public abstract class Generics {

    private static class GenericType implements ParameterizedType {

        private final String name;
        private final Type raw;
        private final Type[] arguments;

        GenericType(String name, Type raw, Type[] arguments) {
            this.name = name;
            this.raw = raw;
            this.arguments = arguments;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arguments) ^ raw.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) other;
                return raw.equals(pt.getRawType()) && Arrays.equals(arguments, pt.getActualTypeArguments());
            }
            return false;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return arguments;
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Nullable
        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class GenericArrayTypeImpl implements GenericArrayType {
        private final String name;
        private final Type componentType;

        private GenericArrayTypeImpl(String name, Type componentType) {
            this.name = name;
            this.componentType = componentType;
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof GenericArrayType) {
                return componentType.equals(((GenericArrayType) o).getGenericComponentType());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return componentType.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final ConcurrentMap<String, Type> typeCache = new ConcurrentHashMap<>();

    private static String getTypeNameCompat(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                try {
                    int dimensions = 0;
                    while (clazz.isArray()) {
                        dimensions++;
                        clazz = clazz.getComponentType();
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(clazz.getName());
                    for (int i = 0; i < dimensions; i++) {
                        sb.append("[]");
                    }
                    return sb.toString();
                } catch (Throwable ignore) {
                }
            }
            return clazz.getName();
        }
        return type.toString();
    }

    public static ParameterizedType makeParameterizedType(final Class<?> container, final Type... arguments) {
        if (container == null) throw new IllegalArgumentException("container can't be null");
        int containerParameterCount = container.getTypeParameters().length;
        if (containerParameterCount == 0) throw new IllegalArgumentException("container must be parameterized type");
        if (arguments == null || arguments.length != containerParameterCount)
            throw new IllegalArgumentException("arguments must have " + containerParameterCount + " elements");
        final StringBuilder sb = new StringBuilder();
        sb.append(container.getName());
        sb.append("<");
        sb.append(getTypeNameCompat(arguments[0]));
        for (int i = 1; i < arguments.length; i++) {
            sb.append(", ");
            sb.append(getTypeNameCompat(arguments[i]));
        }
        sb.append(">");
        final String name = sb.toString();
        GenericType found = (GenericType) typeCache.get(name);
        if (found == null) {
            found = new GenericType(name, container, arguments);
            typeCache.put(name, found);
        }
        return found;
    }

    public static Type makeArrayType(final Type componentType) {
        if (componentType == null) throw new IllegalArgumentException("componentType can't be null");
        final String name = componentType.toString() + "[]";
        Type found = typeCache.get(name);
        if (found == null) {
            if (componentType instanceof Class<?>) {
                found = Array.newInstance((Class<?>) componentType, 0).getClass();
            } else if (componentType instanceof ParameterizedType) {
                found = new GenericArrayTypeImpl(name, componentType);
            } else {
                throw new IllegalArgumentException("Invalid componentType provided: " + componentType + ". Only ParameterizedType or Class supported");
            }
            typeCache.put(name, found);
        }
        return found;
    }

    public static boolean isUnknownType(final Type type) {
        if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            return isUnknownType(gat.getGenericComponentType());
        }
        //This is commented out because each container should cope with generic arguments
		/*if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			for (Type t : pt.getActualTypeArguments()) {
				if (isUnknownType(t)) return true;
			}
		}*/
        return Object.class == type || type instanceof TypeVariable;
    }
}
