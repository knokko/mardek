package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

class ObjectFactory<T> {

	static final Object REFLECTION_FACTORY;
	static final Method GET_CONSTRUCTOR, MUTATE_OBJECT;

	static {
		Object reflectionFactory = null;
		Method getConstructor = null, mutateObject = null;
		try {
			Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
			Method getReflectionFactory = reflectionFactoryClass.getMethod("getReflectionFactory");
			reflectionFactory = getReflectionFactory.invoke(null);
			getConstructor = reflectionFactoryClass.getMethod("newConstructorForSerialization", Class.class);
			mutateObject = reflectionFactoryClass.getMethod("defaultReadObjectForSerialization", Class.class);
		} catch (ClassNotFoundException | NoSuchMethodException notSupported) {
			// The fields will remain null
		} catch (InvocationTargetException | IllegalAccessException unexpected) {
			throw new RuntimeException(unexpected);
		}

		REFLECTION_FACTORY = reflectionFactory;
		GET_CONSTRUCTOR = getConstructor;
		MUTATE_OBJECT = mutateObject;
	}

	final Class<T> objectClass;
	final Supplier<T> createEmptyInstance;

	ObjectFactory(Class<T> objectClass) {
		this.objectClass = objectClass;
		Constructor<T> constructor = null;
		try {
			constructor = objectClass.getDeclaredConstructor();
			try {
				constructor.newInstance();
			} catch (IllegalAccessException e) {
				constructor.setAccessible(true);
			} catch (InstantiationException shouldNotHappen) {
				throw new UnexpectedBitserException(
						"Class " + objectClass + " cannot be instantiated: " + shouldNotHappen.getMessage()
				);
			} catch (InvocationTargetException failedConstruction) {
				throw new UnexpectedBitserException(
						"The constructor of " + objectClass + " failed: " + failedConstruction.getMessage()
				);
			}
		} catch (NoSuchMethodException e) {
			if (GET_CONSTRUCTOR != null) {
				try {
					//noinspection unchecked
					constructor = (Constructor<T>) GET_CONSTRUCTOR.invoke(REFLECTION_FACTORY, objectClass);
				} catch (IllegalAccessException | InvocationTargetException | ClassCastException shouldNotHappen) {
					throw new UnexpectedBitserException(
							"ReflectionHelper.newConstructorForSerializion failed for " +
									objectClass + ": " + shouldNotHappen.getMessage()
					);
				}
			}
		}

		if (constructor == null) {
			throw new InvalidBitFieldException(objectClass + " must have a constructor without parameters");
		}

		final Constructor<T> finalConstructor = constructor;
		this.createEmptyInstance = () -> {
			try {
				return finalConstructor.newInstance();
			} catch (InstantiationException e) {
				throw new UnexpectedBitserException("Failed to instantiate " + finalConstructor);
			} catch (IllegalAccessException shouldNotHappen) {
				throw new UnexpectedBitserException("Can't get access to " + finalConstructor);
			} catch (InvocationTargetException e) {
				throw new InvalidBitFieldException("Constructor " + finalConstructor + " threw an exception: " + e.getMessage());
			}
		};
	}
}
