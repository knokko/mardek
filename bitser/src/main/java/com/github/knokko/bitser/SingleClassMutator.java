package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class SingleClassMutator {

	final Class<?> wrappedClass;
	final Map<String, Field> fields = new HashMap<>();
	final Function<Object, Session> createSession;

	SingleClassMutator(Class<?> wrappedClass, List<SingleClassWrapper.FieldWrapper> fieldWrappers) {
		boolean hasFinalField = false;
		this.wrappedClass = wrappedClass;
		for (SingleClassWrapper.FieldWrapper fieldWrapper : fieldWrappers) {
			if (Modifier.isFinal(fieldWrapper.classField().getModifiers())) {
				hasFinalField = true;
				break;
			}
		}

		for (Field field : wrappedClass.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) continue;
			if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(wrappedClass.getModifiers())) {
				field.setAccessible(true);
			}
			fields.put(field.getName(), field);
		}

		if (ObjectFactory.MUTATE_OBJECT == null || !hasFinalField) {
			this.createSession = ReflectionSession::new;
		} else {
			try {
				MethodHandle readObjectForSerialization = (MethodHandle) ObjectFactory.MUTATE_OBJECT.invoke(
						ObjectFactory.REFLECTION_FACTORY, wrappedClass
				);
				if (readObjectForSerialization == null) {
					if (!Serializable.class.isAssignableFrom(wrappedClass)) {
						throw new InvalidBitFieldException(wrappedClass + " must implement Serializable in Java 24+");
					}
					if (Externalizable.class.isAssignableFrom(wrappedClass)) {
						throw new InvalidBitFieldException(wrappedClass + " must NOT implement Externalizable");
					}
					throw new InvalidBitFieldException(
							"ReflectionFactory.readObjectForSerialization returned null for " + wrappedClass
					);
				}
				this.createSession = target -> new SerializeSession(readObjectForSerialization, fields, target);
			} catch (IllegalAccessException | InvocationTargetException shouldNotHappen) {
				throw new UnexpectedBitserException(
						"Failed to invoke ReflectionFactory.readObjectForSerialization for " +
								wrappedClass + ": " + shouldNotHappen.getMessage()
				);
			}
		}
	}

	interface Session {

		void set(Field field, Object value) throws Throwable;

		void finish() throws Throwable;
	}

	private record ReflectionSession(Object target) implements Session {

		@Override
		public void set(Field field, Object value) throws Throwable {
			field.set(target, value);
		}

		@Override
		public void finish() {}
	}

	private static class SerializeSession implements Session {

		final MethodHandle readObjectForSerialization;
		final Map<String, Field> fields;
		final Object target;
		final Map<String, Object> values = new HashMap<>();

		SerializeSession(MethodHandle readObjectForSerialization, Map<String, Field> fields, Object target) {
			this.readObjectForSerialization = readObjectForSerialization;
			this.fields = fields;
			this.target = target;
		}

		@Override
		public void set(Field field, Object value) {
			values.put(field.getName(), value);
		}

		@Override
		public void finish() throws Throwable {
			readObjectForSerialization.invoke(target, new MutateInputStream(this));
		}
	}

	private static class MutateInputStream extends ObjectInputStream {

		final SerializeSession session;

		MutateInputStream(SerializeSession session) throws IOException {
			this.session = session;
		}

		@Override
		public ObjectInputStream.GetField readFields() {
			return new MutateGetField(session);
		}
	}

	private static class MutateGetField extends ObjectInputStream.GetField {

		final SerializeSession session;

		MutateGetField(SerializeSession session) {
			this.session = session;
		}

		@Override
		public ObjectStreamClass getObjectStreamClass() {
			return null;
		}

		@Override
		public boolean defaulted(String name) {
			return false;
		}

		@Override
		public boolean get(String name, boolean val) {
			return (Boolean) get(name, null);
		}

		@Override
		public byte get(String name, byte val) {
			return (Byte) get(name, null);
		}

		@Override
		public char get(String name, char val) {
			return (Character) get(name, null);
		}

		@Override
		public short get(String name, short val) {
			return (Short) get(name, null);
		}

		@Override
		public int get(String name, int val) {
			return (Integer) get(name, null);
		}

		@Override
		public long get(String name, long val) {
			return (Long) get(name, null);
		}

		@Override
		public float get(String name, float val) {
			return (Float) get(name, null);
		}

		@Override
		public double get(String name, double val) {
			return (Double) get(name, null);
		}

		@Override
		public Object get(String name, Object val) {
			if (session.values.containsKey(name)) {
				return session.values.get(name);
			} else {
				Field field = session.fields.get(name);
				if (field == null) {
					throw new UnexpectedBitserException(
							"Missing field " + name + " for " + session.target + " with fields " + session.fields
					);
				}
				try {
					return field.get(session.target);
				} catch (IllegalAccessException shouldNotHappen) {
					throw new UnexpectedBitserException("Field.get failed for " + field + ": " + shouldNotHappen.getMessage());
				}
			}
		}
	}
}
