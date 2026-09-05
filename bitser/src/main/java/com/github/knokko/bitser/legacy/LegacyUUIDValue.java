package com.github.knokko.bitser.legacy;

import java.util.UUID;

/**
 * Instances of this class are returned during the backward-compatible deserialization of UUID fields. This class
 * is usually not interesting for users, but it may appear in the legacy values of a
 * {@link com.github.knokko.bitser.BitPostInit.Context}.
 *
 * @param value The wrapped UUID value
 */
public record LegacyUUIDValue(UUID value) {

	@Override
	public String toString() {
		return "id " + value;
	}
}
