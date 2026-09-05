package com.github.knokko.bitser.legacy;

/**
 * Instances of this class are created during the backward-compatible deserialization of
 * {@link com.github.knokko.bitser.field.ReferenceField}s. This class is usually not interesting for users, but it may
 * appear in the legacy values of a {@link com.github.knokko.bitser.BitPostInit.Context}. This class is very similar to
 * {@link WithReference}, but the difference is that this class is used for 'internal'
 * references, whereas WithReference is used for references to the <i>with</i> objects.
 *
 * @param reference The legacy reference, which is usually a {@link LegacyStructInstance}.
 */
public record LegacyReference(Object reference) {

	@Override
	public String toString() {
		return "reference to " + reference;
	}
}
