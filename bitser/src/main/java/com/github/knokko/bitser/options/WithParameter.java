package com.github.knokko.bitser.options;

import com.github.knokko.bitser.BitPostInit;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.FunctionContext;

/**
 * With parameters can be used in the {@code withAndOptions} of {@link Bitser#serialize} and {@link Bitser#deserialize}.
 * These values will be propagated to {@link BitPostInit.Context#withParameters} and
 * {@link FunctionContext#withParameters}. This allows classes that implement {@link BitPostInit} to alter their
 * behavior, depending on which {@code WithParameter}s were passed to bitser.
 *
 * @param key The key/name of the parameter
 * @param value The value of the parameter
 */
public record WithParameter(String key, Object value) {}
