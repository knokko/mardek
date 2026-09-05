package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

class WrapperFactory {

	private static final IntegerField DEFAULT_SIZE_FIELD = new IntegerField() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return IntegerField.class;
		}

		@Override
		public boolean expectUniform() {
			return false;
		}

		@Override
		public long minValue() {
			return 0;
		}

		@Override
		public long maxValue() {
			return Integer.MAX_VALUE;
		}

		@Override
		public int digitSize() {
			return IntegerField.DIGIT_SIZE_TERMINATORS;
		}

		@Override
		public long[] commonValues() {
			return new long[0];
		}
	};

	private static NestedFieldSetting getSetting(
			VirtualField.AnnotationHolder annotations, String path, VirtualField field
	) {
		NestedFieldSetting parentSettings = null;

		NestedFieldSetting[] nestedSettings = annotations.getMultiple(NestedFieldSetting.class);
		if (nestedSettings != null) {
			for (NestedFieldSetting setting : nestedSettings) {
				if (path.equals(setting.path())) {
					if (parentSettings != null) {
						throw new InvalidBitFieldException("Multiple NestedFieldSetting's for path " + path + " of " + field);
					}
					parentSettings = setting;
				}
			}
		}

		return parentSettings;
	}

	private static VirtualField.AnnotationHolder getSettingAnnotations(
			VirtualField.AnnotationHolder original, NestedFieldSetting setting, Class<?> objectClass
	) {
		if (setting == null || setting.fieldName().isEmpty()) return original;

		try {
			return new VirtualField.FieldAnnotations(objectClass.getDeclaredField(setting.fieldName()));
		} catch (NoSuchFieldException nope) {
			throw new InvalidBitFieldException(nope.getMessage());
		}
	}

	private static BitFieldWrapper createChildWrapper(
			Class<?> declaringClass, VirtualField.AnnotationHolder rootAnnotations, VirtualField field,
			String path, boolean propagateExpectNothing, String pathSuffix, ChildType child
	) {
		String childPath = path + pathSuffix;

		NestedFieldSetting childSettings = getSetting(rootAnnotations, childPath, field);
		child.annotations = getSettingAnnotations(child.annotations, childSettings, declaringClass);

		VirtualField childField = new VirtualField(
				pathSuffix + " " + field,
				child.fieldType,
				childSettings != null && childSettings.optional(),
				child.annotations,
				null,
				null
		);

		if (propagateExpectNothing && childSettings != null) {
			throw new InvalidBitFieldException("NestedFieldSetting's on writeAsBytes targets is forbidden: " + field);
		}

		return createComplexWrapper(
				declaringClass, rootAnnotations, childField, child.genericType, childPath, propagateExpectNothing
		);
	}

	private static class ChildType {

		final Class<?> fieldType;
		VirtualField.AnnotationHolder annotations;
		final Type genericType;

		ChildType(Class<?> fieldType, VirtualField.AnnotationHolder annotations, Type genericType) {
			this.fieldType = fieldType;
			this.annotations = annotations;
			this.genericType = genericType;
		}
	}

	private static ChildType childFromTypeArgument(
			VirtualField field, VirtualField.AnnotationHolder rootAnnotations, Type actualTypeArgument, String path
	) {
		if (actualTypeArgument instanceof Class<?>) {
			return new ChildType(
					(Class<?>) actualTypeArgument,
					maybeRootAnnotations(rootAnnotations, path),
					null
			);
		} else if (actualTypeArgument instanceof ParameterizedType parType) {
			Type rawChildType = parType.getRawType();
			if (rawChildType instanceof Class<?> rawChildClass) {
				VirtualField.AnnotationHolder childAnnotations;
				if (rawChildClass.isArray() || Collection.class.isAssignableFrom(rawChildClass)) {
					childAnnotations = new VirtualField.NoAnnotations();
				} else {
					childAnnotations = maybeRootAnnotations(rootAnnotations, path);
				}
				return new ChildType(rawChildClass, childAnnotations, parType);
			} else throw new InvalidBitFieldException("Unexpected raw actual type argument for " + field);
		} else if (actualTypeArgument instanceof GenericArrayType arrayType) {
			Type elementType = arrayType.getGenericComponentType();
			if (elementType instanceof ParameterizedType) {
				return new ChildType(Array.newInstance(
						(Class<?>) ((ParameterizedType) elementType).getRawType(), 0
				).getClass(), new VirtualField.NoAnnotations(), arrayType);
			} else throw new UnexpectedBitserException("element type is " + elementType + " and array type is " + arrayType);
		} else {
			throw new InvalidBitFieldException(
					"Unexpected generic type for " + field + ": " + actualTypeArgument.getClass()
			);
		}
	}

	private static VirtualField.AnnotationHolder maybeRootAnnotations(
			VirtualField.AnnotationHolder rootAnnotations, String path
	) {
		if (path.contains("k")) return new VirtualField.NoAnnotations();
		return rootAnnotations;
	}

	private static BitFieldWrapper[] createChildWrappers(
			Class<?> declaringClass, VirtualField.AnnotationHolder rootAnnotations, VirtualField field,
			Type genericType, String path, boolean propagateExpectNothing
	) {
		ChildType child;
		if (field.type.isArray() && (genericType == null || genericType instanceof Class<?>)) {
			child = new ChildType(
					field.type.getComponentType(),
					maybeRootAnnotations(rootAnnotations, path),
					null
			);
		} else if (Map.class.isAssignableFrom(field.type)) {
			Type[] actualTypeArguments = getActualTypeArguments(field, genericType, 2);
			return new BitFieldWrapper[] {
					createChildWrapper(
							declaringClass, rootAnnotations, field, path, propagateExpectNothing, "k",
							childFromTypeArgument(field, rootAnnotations, actualTypeArguments[0], path + "k")
					),
					createChildWrapper(
							declaringClass, rootAnnotations, field, path, propagateExpectNothing, "v",
							childFromTypeArgument(field, rootAnnotations, actualTypeArguments[1], path + "v")
					)
			};
		} else {
			Type actualTypeArgument = getActualTypeArguments(field, genericType, 1)[0];
			child = childFromTypeArgument(field, rootAnnotations, actualTypeArgument, path);
		}

		BitFieldWrapper childWrapper = createChildWrapper(
				declaringClass, rootAnnotations, field, path, propagateExpectNothing, "c", child
		);
		return new BitFieldWrapper[] { childWrapper };
	}

	static BitFieldWrapper createComplexWrapper(
			Class<?> declaringClass, VirtualField.AnnotationHolder rootAnnotations, VirtualField field,
			Type genericType, String path, boolean expectNothing
	) {
		if (Collection.class.isAssignableFrom(field.type) || field.type.isArray() || Map.class.isAssignableFrom(field.type)) {
			BitField rootBitField = rootAnnotations.get(BitField.class);
			if (rootBitField != null && rootBitField.optional()) {
				throw new InvalidBitFieldException("optional BitField is not allowed on collection field " +
						field + ": use @NestedFieldSetting instead");
			}
			NestedFieldSetting parentSettings = getSetting(rootAnnotations, path, field);
			VirtualField.AnnotationHolder parentAnnotations = getSettingAnnotations(
					new VirtualField.NoAnnotations(), parentSettings, declaringClass
			);

			ReferenceField referenceField = parentAnnotations.get(ReferenceField.class);
			if (referenceField != null) {
				if (parentAnnotations.has(ReferenceFieldTarget.class)) throw new InvalidBitFieldException(
						field + " is both a reference field and a reference target, which is forbidden"
				);
				return new ReferenceFieldWrapper(field, referenceField.label(), referenceField.stable());
			}

			BitFieldWrapper[] childWrappers = createChildWrappers(
					declaringClass, rootAnnotations, field, genericType, path,
					parentSettings != null && parentSettings.writeAsBytes()
			);

			VirtualField parentField = new VirtualField(
					field.toString(),
					field.type,
					parentSettings != null && parentSettings.optional(),
					parentAnnotations,
					field.getValue,
					field.setValue
			);
			if (parentSettings != null && parentSettings.writeAsBytes()) {
				if (Map.class.isAssignableFrom(field.type)) {
					throw new InvalidBitFieldException("writeAsBytes is not allowed on Maps: field is " + field);
				}
				if (childWrappers.length != 1) {
					throw new UnexpectedBitserException("Expected exactly 1 child wrapper, but got " + Arrays.toString(childWrappers));
				}
				if (childWrappers[0] != null) {
					throw new InvalidBitFieldException("Value annotations are forbidden when writeAsBytes is true: " + field);
				}
				return new ByteCollectionFieldWrapper(parentField, parentSettings.sizeField());
			} else if (Map.class.isAssignableFrom(field.type)) {
				if (childWrappers.length != 2) {
					throw new UnexpectedBitserException("Expected exactly 2 child wrappers, but got " + Arrays.toString(childWrappers));
				}
				IntegerField sizeField = parentSettings != null ? parentSettings.sizeField() : DEFAULT_SIZE_FIELD;
				return new MapFieldWrapper(parentField, sizeField, childWrappers[0], childWrappers[1]);
			} else {
				if (childWrappers.length != 1) {
					throw new UnexpectedBitserException("Expected exactly 1 child wrapper, but got " + Arrays.toString(childWrappers));
				}
				IntegerField sizeField = parentSettings != null ? parentSettings.sizeField() : DEFAULT_SIZE_FIELD;
				return new BitCollectionFieldWrapper(parentField, sizeField, childWrappers[0]);
			}
		}

		if (field.type == SimpleLazyBits.class || field.type == ReferenceLazyBits.class) {
			if (createSimpleWrapper(field, true) != null || field.annotations.has(ReferenceFieldTarget.class)) {
				throw new InvalidBitFieldException(
						"SimpleLazyBits and ReferenceLazyBits fields shouldn't have any bitser annotations " +
								"except @BitField, but found " + field
				);
			}

			if (genericType instanceof ParameterizedType) {
				Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
				if (typeArguments.length != 1) {
					throw new UnexpectedBitserException("Expected exactly 1 type argument for " + field);
				}
				if (typeArguments[0] instanceof Class<?> valueClass) {
					if (!valueClass.isAnnotationPresent(BitStruct.class)) {
						throw new InvalidBitFieldException("Generic type of " + field + " must be a BitStruct");
					}
					if (field.type == ReferenceLazyBits.class) {
						var info = field.annotations.get(LazyReferences.class);
						if (info == null) {
							throw new InvalidBitFieldException(field + " must be annotated with @LazyReferences");
						}
						return new ReferenceLazyFieldWrapper(field, valueClass, info.labels());
					} else return new SimpleLazyFieldWrapper(field, valueClass);
				} else {
					throw new InvalidBitFieldException(
							"Unexpected generic type " + genericType + " for " + field + ": type must be a BitStruct"
					);
				}
			} else {
				throw new UnexpectedBitserException(
						"Unexpected generic type " + genericType + " for " + field + ": must be a BitStruct"
				);
			}
		}

		return createSimpleWrapper(field, expectNothing);
	}

	private static Type[] getActualTypeArguments(VirtualField field, Type genericType, int expectedAmount) {
		if (genericType instanceof ParameterizedType parGenericType) {

			Type[] actualTypeArguments = parGenericType.getActualTypeArguments();
			if (actualTypeArguments.length != expectedAmount) {
				throw new InvalidBitFieldException(
						"Unexpected number of type arguments " + actualTypeArguments.length + " for field " + field
				);
			}
			return actualTypeArguments;
		} else if (genericType instanceof GenericArrayType genericArrayType) {
			if (expectedAmount != 1) {
				throw new InvalidBitFieldException("Expected " + expectedAmount + " type parameters on field " + field);
			}
			return new Type[] { genericArrayType.getGenericComponentType() };
		} else throw new InvalidBitFieldException("Unexpected generic type " + genericType + " for " + field);
	}

	private static BitFieldWrapper createSimpleWrapper(VirtualField field, boolean expectNothing) {
		if (field.referenceTargetLabel != null && field.type.isPrimitive()) {
			throw new InvalidBitFieldException("Reference target " + field + " is primitive, which is forbidden");
		}

		if (field.annotations.has(StableReferenceFieldId.class)) {
			if (field.type != UUID.class) throw new InvalidBitFieldException(
					"Only UUID fields can have @StableReferenceFieldId"
			);
			if (field.optional) throw new InvalidBitFieldException("@StableReferenceFieldId's can't be optional");
		}

		ReferenceField referenceField = field.annotations.get(ReferenceField.class);
		if (referenceField != null) {
			if (field.annotations.has(ReferenceFieldTarget.class)) throw new InvalidBitFieldException(
					field + " is both a reference field and a reference target, which is forbidden"
			);
			return new ReferenceFieldWrapper(field, referenceField.label(), referenceField.stable());
		}

		List<BitFieldWrapper> result = new ArrayList<>(1);

		IntegerField intField = field.annotations.get(IntegerField.class);
		if (intField != null) result.add(new IntegerFieldWrapper(field, intField));

		FloatField floatField = field.annotations.get(FloatField.class);
		if (floatField != null) result.add(new FloatFieldWrapper(field, floatField));

		StringField stringField = field.annotations.get(StringField.class);
		if (stringField != null) result.add(new StringFieldWrapper(field, stringField));

		ClassField classField = field.annotations.get(ClassField.class);
		if (classField != null || field.type.getAnnotation(BitStruct.class) != null) {
			result.add(new StructFieldWrapper(field, classField));
		}

		EnumField enumField = field.annotations.get(EnumField.class);
		if (enumField != null) {
			result.add(new EnumFieldWrapper(field, enumField.mode()));
		}
		BitEnum bitEnum = field.type.getAnnotation(BitEnum.class);
		if (enumField == null && bitEnum != null) result.add(new EnumFieldWrapper(field, bitEnum.mode()));

		if (expectNothing && result.isEmpty()) return null;

		if (field.type == UUID.class) result.add(new UUIDFieldWrapper(field));
		if (field.annotations.has(LazyReferences.class)) {
			throw new InvalidBitFieldException("The @LazyReferences annotation is only " +
					"allowed on ReferenceLazyBits, but found " + field);
		}

		if (field.type == boolean.class || field.type == Boolean.class) result.add(new BooleanFieldWrapper(field));
		if (field.type == String.class && stringField == null) result.add(new StringFieldWrapper(field, null));

		if (result.isEmpty()) throw new InvalidBitFieldException("Missing annotations for " + field);
		if (result.size() > 1) throw new UnexpectedBitserException("Too many annotations on " + field);
		return result.get(0);
	}
}
