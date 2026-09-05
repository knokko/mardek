package com.github.knokko.bitser.legacy;

import com.github.knokko.bitser.BitPostInit;
import com.github.knokko.bitser.field.ReferenceField;

/**
 * Instances of this class are created during the backward-compatible deserialization of
 * {@link ReferenceField}s. This class is usually not interesting for users, but it may
 * appear in the legacy values of a {@link BitPostInit.Context}. This class is very similar to
 * {@link LegacyReference}, but the difference is that LegacyReference is used for
 * 'internal' references, whereas this class is used for references to the <i>with</i> objects.
 *
 * @param reference The <i>with</i> object to which this reference points
 */
public record WithReference(Object reference) {

	@Override
	public String toString() {
		return "with reference to " + reference;
	}
}
