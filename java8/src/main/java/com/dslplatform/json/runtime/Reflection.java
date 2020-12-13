package com.dslplatform.json.runtime;

import com.dslplatform.json.ConfigurationException;
import com.dslplatform.json.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

abstract class Reflection {

	static final class ReadField implements Settings.Function {
		private final Field field;

		ReadField(Field field) {
			this.field = field;
		}

		@Override
		public Object apply(@Nullable Object instance) {
			try {
				return field.get(instance);
			} catch (IllegalAccessException e) {
				throw new ConfigurationException("Unable to read field " + field.getName() + " of " + field.getDeclaringClass(), e);
			}
		}
	}

	static final class ReadMethod implements Settings.Function {
		private final Method method;

		ReadMethod(Method method) {
			this.method = method;
		}

		@Override
		public Object apply(@Nullable Object instance) {
			try {
				return method.invoke(instance);
			}catch (IllegalArgumentException e){
				throw new ConfigurationException("Unable to call method " + method.getName() + " of " + method.getDeclaringClass() + " setting value of type " + (instance == null ? "Void" : instance.getClass().getCanonicalName()), e);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new ConfigurationException("Unable to call method " + method.getName() + " of " + method.getDeclaringClass(), e);
			}
		}
	}

	static final class SetField implements Settings.BiConsumer {
		private final Field field;

		SetField(Field field) {
			this.field = field;
		}

		@Override
		public void accept(Object instance, @Nullable Object value) {
			try {
				field.set(instance, value);
			} catch (IllegalAccessException e) {
				throw new ConfigurationException("Unable to set field " + field.getName() + " of " + field.getDeclaringClass(), e);
			}
		}
	}

	static final class SetMethod implements Settings.BiConsumer {
		private final Method method;

		SetMethod(Method method) {
			this.method = method;
		}

		@Override
		public void accept(Object instance, @Nullable Object value) {
			try {
				method.invoke(instance, value);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new ConfigurationException("Unable to call method " + method.getName() + " of " + method.getDeclaringClass(), e);
			}
		}
	}

	private static boolean canUseTypeName = true;

	static String typeDescription(Type manifest) {
		if (manifest instanceof Class<?>) {
			return ((Class<?>)manifest).getName();
		} else if (canUseTypeName) {
			try {
				return tryTypeName(manifest);
			} catch (NoSuchMethodError ignore) {
				canUseTypeName = false;
			}
		}
		return manifest.toString();
	}

	private static String tryTypeName(Type manifest) {
		return manifest.getTypeName();
	}
}
