package com.github.knokko.bitser.options;

import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.io.*;

import java.io.PrintWriter;

/**
 * When an instance of this class is used in the {@code withAndOptions} of
 * {@link Bitser#toBytes} or {@link Bitser#fromBytes}, bitser will use a
 * {@link DebugBitOutputStream} or {@link DebugBitInputStream} instead of the regular {@link BitOutputStream} or
 * {@link BitInputStream} for (de)serialization, which can be convenient for debugging.
 *
 * @param writer See {@link DebugBitOutputStream#DebugBitOutputStream}
 * @param aggressiveFlush See {@link DebugBitOutputStream#DebugBitOutputStream}
 */
public record DebugBits(PrintWriter writer, boolean aggressiveFlush) {

	/**
	 * An instance of {@link DebugBits} that writes its debug info to {@link System#out}, and with
	 * {@code aggressiveFlush} enabled.
	 */
	public static final DebugBits HARD = new DebugBits(new PrintWriter(System.out), true);
}
