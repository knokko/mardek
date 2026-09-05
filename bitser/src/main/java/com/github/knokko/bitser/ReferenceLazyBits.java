package com.github.knokko.bitser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *     A more powerful and complicated version of {@link SimpleLazyBits}. Unlike {@code SimpleLazyBits}, this class
 *     allows the lazy/wrapped object to have references to the 'outer' objects, as well as the 'with' objects.
 *     You need the {@link com.github.knokko.bitser.field.LazyReferences} annotation to specify which outer reference
 *     labels are allowed.
 * </p>
 *
 * <p>
 *     The drawback of this class, compared to {@link SimpleLazyBits}, is that all fields of this type must be annotated
 *     with {@link com.github.knokko.bitser.field.LazyReferences}, and that it may have a larger memory usage or
 *     performance impact.
 * </p>
 *
 * <h3>Backward compatibility</h3>
* <p>
 *     If a field of the original serialized struct is a {@code ReferenceLazyBits}, the corresponding field of the new
 *     deserialized struct must either be a {@code ReferenceLazyBits}, a {@link SimpleLazyBits}, or a simple non-lazy
 *     struct. (But, if the field of the deserialized struct is a {@link SimpleLazyBits}, the 'outer' references
 *     need to be removed before the deserialized instance can be saved again.)
 * </p>
 * <p>
 *     Basically, it is allowed to turn any lazy struct into an eager struct (or another lazy struct), and vice versa.
 *     This should be convenient for profiling.
 * </p>
 * @param <T> The type of the values to be wrapped. The class must be annotated with {@link BitStruct}.
 */
public final class ReferenceLazyBits<T> {

	Class<?> valueClass;
	private Bitser bitser;
	private boolean backwardCompatible;
	LazyReferenceTargets[] potentialTargets;

	T value;
	byte[] bytes;

	String[] labels;

	/**
	 * Creates a new {@link ReferenceLazyBits} that wraps {@code value}.
	 * You can access it later by calling {@link #get()}.
	 */
	public ReferenceLazyBits(T value) {
		this.value = Objects.requireNonNull(value);
	}

	ReferenceLazyBits(Bitser bitser, boolean backwardCompatible, Class<? extends T> valueClass) {
		this.bitser = Objects.requireNonNull(bitser);
		this.backwardCompatible = backwardCompatible;
		this.valueClass = valueClass;
	}

	/**
	 * Gets the wrapped value that was supplied to the constructor. Calling this method is usually very cheap, except
	 * when it is called for the first time on a deserialized instance of this class.
	 */
	public T get() {
		if (value == null) {
			List<Object> optionList = new ArrayList<>(2);
			optionList.add(potentialTargets);
			if (backwardCompatible) optionList.add(Bitser.BACKWARD_COMPATIBLE);
			//noinspection unchecked
			value = (T) bitser.fromBytes(valueClass, bytes, optionList.toArray());
		}
		return value;
	}
}
