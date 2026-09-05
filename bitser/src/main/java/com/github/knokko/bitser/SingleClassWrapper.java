package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static com.github.knokko.bitser.WrapperFactory.createComplexWrapper;

class SingleClassWrapper {

	private static final BitField DEFAULT_BIT_FIELD = new BitField() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return BitField.class;
		}

		@Override
		public int id() {
			return -1;
		}

		@Override
		public boolean optional() {
			return false;
		}

		@Override
		public boolean readsMethodResult() {
			return false;
		}
	};

	final Class<?> myClass;
	final List<FieldWrapper> fields = new ArrayList<>();
	final List<FieldWrapper> fieldsSortedById;
	final List<FunctionWrapper> functions = new ArrayList<>();

	SingleClassWrapper(Class<?> myClass, boolean backwardCompatible) {
		this.myClass = myClass;

		Class<?>[] otherFields = {
				ClassField.class, EnumField.class, FloatField.class, IntegerField.class, NestedFieldSetting.class,
				NestedFieldSettings.class, ReferenceField.class, ReferenceFieldTarget.class,
				StableReferenceFieldId.class, LazyReferences.class, StringField.class
		};

		Map<Integer, FunctionWrapper> functionMapping = new HashMap<>();
		for (Method classMethod : myClass.getDeclaredMethods()) {
			BitField bitField = classMethod.getAnnotation(BitField.class);
			if (bitField == null) continue;
			if (Modifier.isStatic(classMethod.getModifiers())) {
				throw new InvalidBitFieldException("BitField methods must not be static: " + classMethod);
			}
			if (bitField.readsMethodResult()) {
				throw new InvalidBitFieldException("BitField methods cannot have readsMethodResult = true: " + classMethod);
			}
			Parameter[] parameters = classMethod.getParameters();
			if (parameters.length > 1) {
				throw new InvalidBitFieldException("BitField methods can have at most 1 parameter: " + classMethod);
			}
			if (parameters.length == 1 && parameters[0].getType() != FunctionContext.class) {
				throw new InvalidBitFieldException("BitField method parameter type must be FunctionContext: " + classMethod);
			}
			if (bitField.id() < 0) {
				throw new InvalidBitFieldException("BitField method IDs must be non-negative: " + classMethod);
			}
			if (functionMapping.containsKey(bitField.id())) {
				throw new InvalidBitFieldException(myClass + " has multiple @BitField methods with id " + bitField.id());
			}
			if (!Modifier.isPublic(classMethod.getModifiers())) classMethod.setAccessible(true);
			VirtualField field = new VirtualField(
					classMethod.toString(),
					classMethod.getReturnType(),
					bitField.optional(),
					new VirtualField.MethodAnnotations(classMethod),
					null, null
			);

			BitFieldWrapper bitFieldWrapper = createComplexWrapper(
					myClass, field.annotations, field, classMethod.getGenericReturnType(), "", false
			);
			var wrapper = new FunctionWrapper(bitField.id(), classMethod, bitFieldWrapper);
			functions.add(wrapper);
			functionMapping.put(bitField.id(), wrapper);
		}

		functions.sort(Comparator.comparingInt(a -> a.id));

		Set<Integer> fieldIDs = new HashSet<>();
		for (Field classField : myClass.getDeclaredFields()) {
			if (Modifier.isStatic(classField.getModifiers())) continue;
			BitField bitField = classField.getAnnotation(BitField.class);
			if (bitField == null) {
				for (Class<?> otherField : otherFields) {
					//noinspection unchecked
					if (classField.isAnnotationPresent((Class<? extends Annotation>) otherField)) {
						bitField = DEFAULT_BIT_FIELD;
						break;
					}
				}
				if (classField.getType() == SimpleLazyBits.class || classField.getType() == ReferenceLazyBits.class) {
					bitField = DEFAULT_BIT_FIELD;
				}
				if (bitField == null) continue;
			}

			if (bitField.id() < 0 && backwardCompatible) {
				throw new InvalidBitFieldException("BitField IDs must be non-negative when backward compatible: " + classField);
			}
			if (bitField.id() >= 0) {
				if (fieldIDs.contains(bitField.id())) {
					throw new InvalidBitFieldException(myClass + " has multiple @BitField's with id " + bitField.id());
				}
				fieldIDs.add(bitField.id());
			}

			if (Modifier.isFinal(classField.getModifiers()) || !Modifier.isPublic(classField.getModifiers()) ||
					!Modifier.isPublic(classField.getDeclaringClass().getModifiers())) {
				classField.setAccessible(true);
			}

			if (bitField.readsMethodResult()) {
				if (bitField.id() < 0) throw new InvalidBitFieldException(
						"BitField's with readsMethodResult = true must have a non-negative ID: " + classField
				);
				var linkedFunction = functionMapping.get(bitField.id());
				if (linkedFunction == null) throw new InvalidBitFieldException(
						"Field " + classField + " needs a method annotated with @BitField(id = " + bitField.id() + ")"
				);
				if (classField.getType() != linkedFunction.classMethod.getReturnType()) {
					throw new InvalidBitFieldException(
							"Type of " + classField + " does not match the return type of " +
									linkedFunction.classMethod
					);
				}
				fields.add(new FieldWrapper(bitField.id(), classField, linkedFunction.bitField, true));
				continue;
			}

			var conflictingFunction = functionMapping.get(bitField.id());
			if (conflictingFunction != null) throw new InvalidBitFieldException(
					"BitField id conflict between " + classField + " and " + conflictingFunction.classMethod
			);

			VirtualField field = new VirtualField(
					classField.toString(),
					classField.getType(),
					bitField.optional(),
					new VirtualField.FieldAnnotations(classField),
					target -> {
						try {
							return classField.get(target);
						} catch (IllegalAccessException e) {
							throw new UnexpectedBitserException("Failed to access " + classField);
						}
					},
					(target, newValue) -> {
						try {
							classField.set(target, newValue);
						} catch (IllegalAccessException e) {
							throw new UnexpectedBitserException("Failed to access " + classField);
						}
					}
			);

			BitFieldWrapper bitFieldWrapper = createComplexWrapper(
					myClass, field.annotations, field, classField.getGenericType(), "", false
			);
			fields.add(new FieldWrapper(bitField.id(), classField, bitFieldWrapper, bitField.readsMethodResult()));
		}

		fields.sort(Comparator.comparing(a -> a.classField.getName()));
		this.fieldsSortedById = new ArrayList<>(fields);
		fieldsSortedById.sort(Comparator.comparingInt(a -> a.id));
	}

	List<FieldWrapper> getFields(boolean backwardCompatible) {
		return backwardCompatible ? fieldsSortedById : fields;
	}

	@Override
	public String toString() {
		return myClass.getName();
	}

	void shallowCopy(Object original, Object target) {
		for (FieldWrapper fieldWrapper : fields) {
			fieldWrapper.bitField.field.setValue.accept(target, fieldWrapper.bitField.field.getValue.apply(original));
		}
	}

	record FieldWrapper(int id, Field classField, BitFieldWrapper bitField, boolean readsMethodResult) {}

	record FunctionWrapper(int id, Method classMethod, BitFieldWrapper bitField) {

		Object computeValue(Object object, FunctionContext context) throws Throwable {
				try {
					if (classMethod.getParameterCount() == 0) return classMethod.invoke(object);
					else return classMethod.invoke(object, context);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
			}
		}
}
