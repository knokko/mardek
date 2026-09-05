package com.github.knokko.bitser.legacy;

import java.util.UUID;

/**
 * Instances of this class are created during the backward-compatible deserialization of
 * {@link com.github.knokko.bitser.BitStruct}s. This class is usually not interesting for users, but it may
 * appear in the legacy values of a {@link com.github.knokko.bitser.BitPostInit.Context}.
 */
public class LegacyStructInstance {

	/**
	 * When the original field was annotated with {@link com.github.knokko.bitser.field.ClassField}, this is the index
	 * such that {@code root.BITSER_HIERARCHY[allowedClassIndex] == originalValue.class}. This field is usually not
	 * interesting for users.
	 */
	public final int allowedClassIndex;

	/**
	 * This array has an element for each class in the class/inheritance hierarchy of the deserialized bit struct.
	 * Each element contains the deserialized values of the {@code @BitField}s of that class.
	 */
	public final LegacyClassValues[] hierarchy;

	/**
	 * If the original class had a field annotated with {@link com.github.knokko.bitser.field.StableReferenceFieldId},
	 * this will be its stable ID. Otherwise, it will be {@code null}.
	 */
	public UUID stableID;

	/**
	 * This field should only be used by bitser internally.
	 */
	public Object modernObject;

	/**
	 * This constructor should only be used by bitser internally.
	 */
	public LegacyStructInstance(int allowedClassIndex, LegacyClassValues[] hierarchy) {
		this.allowedClassIndex = allowedClassIndex;
		this.hierarchy = hierarchy;
	}

	@Override
	public String toString() {
		return "LegacyStructInstance";
	}
}
