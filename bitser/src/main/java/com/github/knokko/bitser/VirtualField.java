package com.github.knokko.bitser;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.ReferenceFieldTarget;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;

@BitStruct(backwardCompatible = false)
class VirtualField {

	private final String source;
	final Class<?> type;

	@BitField
	final boolean optional;

	@BitField(optional = true)
	final String referenceTargetLabel;

	final AnnotationHolder annotations;
	final Function<Object, Object> getValue;
	final BiConsumer<Object, Object> setValue;

	VirtualField(
			String source, Class<?> type, boolean optional, AnnotationHolder annotations,
			Function<Object, Object> getValue, BiConsumer<Object, Object> setValue
	) {
		this.source = source;
		this.type = type;
		this.optional = optional;
		ReferenceFieldTarget referenceFieldTarget = annotations.get(ReferenceFieldTarget.class);
		this.referenceTargetLabel = referenceFieldTarget != null ? referenceFieldTarget.label() : null;
		this.annotations = annotations;
		this.getValue = getValue;
		this.setValue = setValue;
	}

	VirtualField() {
		this.source = null;
		this.type = null;
		this.optional = false;
		this.referenceTargetLabel = null;
		this.annotations = null;
		this.getValue = null;
		this.setValue = null;
	}

	@Override
	public String toString() {
		return source;
	}

	interface AnnotationHolder {

		<T extends Annotation> T get(Class<T> annotation);

		<T extends Annotation> T[] getMultiple(Class<T> annotation);

		default <T extends Annotation> boolean has(Class<T> annotation) {
			return get(annotation) != null;
		}
	}

	static class NoAnnotations implements AnnotationHolder {
		@Override
		public <T extends Annotation> T get(Class<T> annotation) {
			return null;
		}

		@Override
		public <T extends Annotation> T[] getMultiple(Class<T> annotation) {
			return null;
		}
	}

	static class FieldAnnotations implements AnnotationHolder {

		private final Field field;

		FieldAnnotations(Field field) {
			this.field = field;
		}

		@Override
		public <T extends Annotation> T get(Class<T> annotation) {
			return field.getAnnotation(annotation);
		}

		@Override
		public <T extends Annotation> T[] getMultiple(Class<T> annotation) {
			return field.getAnnotationsByType(annotation);
		}
	}

	static class MethodAnnotations implements AnnotationHolder {

		private final Method method;

		MethodAnnotations(Method method) {
			this.method = method;
		}

		@Override
		public <T extends Annotation> T get(Class<T> annotation) {
			return method.getAnnotation(annotation);
		}

		@Override
		public <T extends Annotation> T[] getMultiple(Class<T> annotation) {
			return method.getAnnotationsByType(annotation);
		}
	}
}
