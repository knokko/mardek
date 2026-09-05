package com.github.knokko.bitser.field;

import com.github.knokko.bitser.Bitser;

import java.util.Map;
import java.util.Objects;

/**
 * When a <i>method</i> is annotated with {@link BitField}, it must either:
 * <ul>
 *     <li>Not have any parameters, or</li>
 *     <li>have exactly 1 parameter of type {@code FunctionContext}.</li>
 * </ul>
 * This class contains potentially useful information that such methods may need to serialize their value.
 */
public class FunctionContext {

	/**
	 * The bitser instance that is used to serialize the object containing this method
	 */
	public final Bitser bitser;

	/**
	 * Whether the {@link Bitser#BACKWARD_COMPATIBLE} is being used to save the object containing this method
	 */
	public final boolean backwardCompatible;

	/**
	 * The {@link com.github.knokko.bitser.options.WithParameter}s that were passed to the options of
	 * {@link Bitser#serialize}
	 */
	public final Map<String, Object> withParameters;

	/**
	 * Constructs a new {@link FunctionContext}. This should normally only be used by bitser itself.
	 */
	public FunctionContext(Bitser bitser, boolean backwardCompatible, Map<String, Object> withParameters) {
		this.bitser = Objects.requireNonNull(bitser);
		this.backwardCompatible = backwardCompatible;
		this.withParameters = withParameters;
	}
}
