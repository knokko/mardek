package com.github.knokko.bitser.exceptions;

/**
 * <p>
 *     During e.g. (de)serialization, bitser will 'emulate' recursion (using some kind of stack) to avoid potential
 *     {@link StackOverflowError}s when deep nested structs are (de)serialized. The drawback of faking recursion is that
 *     the stacktraces of exceptions thrown during fake recursion are not very enlightening.
 * </p>
 *
 * <p>
 *     To work around this problem, bitser will maintain a simplified stacktrace during fake recursions. When an
 *     exception is thrown, bitser will wrap it into a {@code RecursionException}, and put the simplified stacktrace
 *     in {@link #debugInfoStack}
 * </p>
 */
public class RecursionException extends BitserException {

	/**
	 * The simplified 'fake' stacktrace that bitser maintained during fake recursion.
	 */
	public final String debugInfoStack;

	/**
	 * See {@link RecursionException}
	 * @param debugInfoStack The simplified 'fake' stacktrace that bitser maintained during fake recursion.
	 * @param cause The exception that was thrown during the 'fake' recursion
	 */
	public RecursionException(String debugInfoStack, Throwable cause) {
		super("Error during fake recursion: \"" + cause.getMessage() + "\", debug stack: " + debugInfoStack, cause);
		this.debugInfoStack = debugInfoStack;
	}
}
