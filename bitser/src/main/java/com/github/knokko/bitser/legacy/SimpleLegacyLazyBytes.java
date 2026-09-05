package com.github.knokko.bitser.legacy;

import com.github.knokko.bitser.BitPostInit;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.SimpleLazyBits;

/**
 * Instances of this class are created during the backward-compatible deserialization of a
 * {@link SimpleLazyBits} field. This class is usually not interesting for users, but it may
 * appear in the legacy values of a {@link BitPostInit.Context}.
 *
 * @param bytes The bytes containing the data of the wrapped struct that was serialized.
 *              You should be able to deserialize it using {@link Bitser#fromBytes}.
 */
public record SimpleLegacyLazyBytes(byte[] bytes) { }
