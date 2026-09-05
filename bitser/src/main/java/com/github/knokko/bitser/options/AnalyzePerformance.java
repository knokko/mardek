package com.github.knokko.bitser.options;

import com.github.knokko.bitser.io.AnalysisBitInputStream;
import com.github.knokko.bitser.Bitser;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     This option can be used in {@link Bitser#fromBytes} (and {@link Bitser#stupidDeepCopy}).
 *     When this option is used, bitser will track how many bits are used for each class and field,
 *     as well as how long it took to deserialize each class and field.
 * </p>
 *
 * <h3>Reading the results</h3>
 * <p>
 *     After deserialization is finished, bitser will store the results in {@link #rootNodes}.
 *     The easiest way to read these results, is by putting a debugger breakpoint right after {@code fromBytes},
 *     and inspecting the {@code rootNodes}. Alternatively, you can iterate over the root nodes programmatically.
 * </p>
 */
public class AnalyzePerformance {

	/**
	 * When bitser finishes the deserialization, the analysis results will be put in this list. You do <b>not</b> need
	 * to modify this list yourself.
	 */
	public final List<AnalysisBitInputStream.Node> rootNodes = new ArrayList<>();

	/**
	 * When bitser finishes the deserialization, the spent time and bits of each deserialization stage will be put in
	 * this list. You do <b>not</b> need to modify this list yourself.
	 */
	public final List<AnalysisBitInputStream.Stage> stages = new ArrayList<>();
}
