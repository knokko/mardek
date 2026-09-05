package com.github.knokko.bitser.legacy;

/**
 * Instances of this class are created during the backward-compatible deserialization of map fields. This class is
 * usually not interesting for users, but it may appear in the legacy values of a
 * {@link com.github.knokko.bitser.BitPostInit.Context}.
 *
 * @param keys All the keys that were in the map, not in any particular order.
 * @param values All the values that were in the map. This array must have the same length as {@code keys}, and each
 *               element in {@code keys} was mapped to the element in {@code values} with the same index
 *               (e.g. {@code keys[0]} was mapped to {@code values[0]}).
 */
public record LegacyMapValue(Object[] keys, Object[] values) {}
