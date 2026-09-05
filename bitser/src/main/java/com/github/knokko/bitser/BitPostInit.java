package com.github.knokko.bitser;

import java.util.Map;
import java.util.Objects;

/**
 * When {@link BitStruct}s implement this interface, bitser will invoke their {@link #postInit} method after each
 * instance is deserialized. (So it will be invoked exactly once per instance.) This can be used to add custom
 * deserialization logic to the struct, or simply to run some code on start-up.
 */
public interface BitPostInit {

	/**
	 * This method will be called after each field has been deserialized.
	 *
	 * @param context The context contains all kinds of information that might be useful.
	 */
	void postInit(Context context);

	/**
	 * The context parameter type of {@link #postInit}. This is a group of (read-only) fields that the implementing
	 * class may or may not use.
	 */
	class Context {

		/**
		 * The bitser instance that is used to deserialize this {@link BitStruct}. You can use this to e.g.
		 * (de)serialize other objects, or use some utility methods like {@link Bitser#hashCode(Object)}.
		 */
		public final Bitser bitser;

		/**
		 * Whether this struct is being deserialized in a backward-compatible manner, in other words: whether the
		 * {@link Bitser#BACKWARD_COMPATIBLE} option was passed to the {@link Bitser#deserialize} that is deserializing
		 * this struct.
		 */
		public final boolean backwardCompatible;

		/**
		 * <p>
		 *     This map contains an entry for the {@link BitStruct} itself, as well as an entry for each of its
		 *     superclasses. Each value is an {@code Object[]} that stores the values of all bit fields and the results
		 *     of all bit functions.
		 * </p>
		 *
		 * <p>
		 *     The value of the field annotated with {@code @BitField(id = x)} of class {@code ExampleClass} can be
		 *     accessed using {@code values.get(ExampleClass.class)[x]}.
		 * </p>
		 *
		 * <p>
		 *     If {@link #postInit} is invoked during backward-compatible deserialization, most of the values in this
		 *     map are already converted from 'legacy values' (e.g.
		 *     {@link com.github.knokko.bitser.legacy.LegacyStringValue}) to their 'modern counterparts'
		 *     (e.g. {@link String}).
		 * </p>
		 *
		 * <h3>Nullability</h3>
		 * <p>
		 *     This field will be {@code null} if and only if {@link #backwardCompatible} is {@code true} and this
		 *     struct (instance) did <i>not</i> yet exist when it was serialized, for instance when:
		 * </p>
		 *
		 * <ol>
		 *     <li>Some bit struct {@code OuterStruct} is saved to disk</li>
		 *     <li>You create a new class {@code InnerStruct} that implements {@link BitPostInit}</li>
		 *     <li>You add a new field of type {@code InnerStruct} to {@code OuterStruct}</li>
		 *     <li>You deserialize an {@code OuterStruct} from the file saved in step 1</li>
		 *     <li>
		 *         The {@code functionValues} of the {@code Context} passed to {@code InnerStruct.postInit}
		 *         will be {@code null}, since that {@code InnerStruct} was never saved.
		 *     </li>
		 * </ol>
		 *
		 * <p>
		 *     Note that in this example, steps 1 and 2 could be swapped: it only matters that step 3 comes after
		 *     step 1, and that step 4 comes after step 3.
		 * </p>
		 */
		public final Map<Class<?>, Object[]> values;

		/**
		 * <p>
		 *     This map contains an entry for the {@link BitStruct} itself, as well as an entry for each of its
		 *     superclasses. Each value is an {@code Object[]} that stores all the 'legacy' field values (e.g.
		 *     the values of {@code @IntegerField}s will be {@code LegacyIntValue}s).
		 * </p>
		 *
		 * <p>
		 *     The legacy value of the field annotated with {@code @BitField(id = x)} of class {@code ExampleClass}
		 *     can be accessed using {@code functionValues.get(ExampleClass.class)[x]}. However, if {@code ExampleClass}
		 *     also has a method annotated with {@code BitField(id = x)}, the method result will be stored instead.
		 * </p>
		 *
		 * <h3>Nullability</h3>
		 * <p>
		 *     This field will be {@code null} if and only if {@link #backwardCompatible} is {@code false}, or if
		 *     {@link #values} is null.
		 * </p>
		 */
		public final Map<Class<?>, Object[]> legacyValues;

		/**
		 * This map contains the {@link com.github.knokko.bitser.options.WithParameter}s that were passed to
		 * {@link Bitser#deserialize}.
		 */
		public final Map<String, Object> withParameters;

		/**
		 * This constructor should only be used by bitser internally.
		 */
		public Context(
				Bitser bitser,
				boolean backwardCompatible,
				Map<Class<?>, Object[]> values,
				Map<Class<?>, Object[]> legacyValues,
				Map<String, Object> withParameters
		) {
			this.bitser = Objects.requireNonNull(bitser);
			this.backwardCompatible = backwardCompatible;
			this.values = values;
			this.legacyValues = legacyValues;
			this.withParameters = Objects.requireNonNull(withParameters);
		}
	}
}
