package com.github.knokko.bitser.distributions;

import com.github.knokko.bitser.FloatBitser;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.min;

/**
 * A {@link DistributionTracker} for {@link FloatField}s. You can use it like this: {@code
 * var distribution = new FloatDistributionTracker();
 * bitser.boBytes(someStruct, distribution);
 * distribution.optimize(new PrintWriter(System.out), 5, 2, 5, new double[] { 0.01, 0.1 });
 * }
 */
public class FloatDistributionTracker extends DistributionTracker<Double> {

	/**
	 * An entry of the list returned by {@link #optimize(String, int, double[])}. Each entry predicts how many bits
	 * would have been needed to serialize all the values, if a specific {@link FloatField} configuration were used.
	 */
	public static class Entry {

		/**
		 * The digit size of {@link FloatField#expectedIntegerMultiple()}
		 */
		public int digitSize;

		/**
		 * The {@link FloatField#expectMultipleOf()}
		 */
		public double expectMultipleOf;

		/**
		 * The {@link FloatField#commonValues()}
		 */
		public double[] commonValues;

		/**
		 * The number of bits that would be needed to serialize all the values of the corresponding floating-point
		 * field, if this {@link FloatField} configuration would be used.
		 */
		public int spentBits;

		@Override
		public String toString() {
			return "(spentBits=" + spentBits + ", digitSize=" + (digitSize == -1 ? "uniform" : digitSize) +
					", expectMultipleOf=" + expectMultipleOf + ", commonValues=" + Arrays.toString(commonValues) + ")";
		}
	}

	@Override
	public List<String> getSortedFields() {
		List<String> fields = new ArrayList<>(mapping.keySet());
		fields.sort((a, b) -> -Long.compare(countBits(a), countBits(b)));
		return fields;
	}

	private int countBits(String field) {
		Field<Double> fieldData = mapping.get(field);
		return countBits(fieldData, (FloatField.Properties) fieldData.properties);
	}

	private int countBits(Field<Double> fieldData, FloatField.Properties properties) {
		boolean doublePrecision = fieldData.type == double.class || fieldData.type == Double.class;
		BitCountStream bitCounter = new BitCountStream();
		fieldData.countMap.forEach((value, occurrences) -> {
			for (int counter = 0; counter < occurrences; counter++) {
				try {
					FloatBitser.encodeFloat(value, doublePrecision, properties, bitCounter);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		return bitCounter.getCounter();
	}

	/**
	 * Prints recommendations to optimize the top-{@code maxFields} {@link FloatField}s
	 * @param writer The writer to which the recommendations are printed, e.g. {@code new PrintWriter(System.out)}
	 * @param maxFields The maximum number of fields for which recommendations will be printed.
	 * @param maxCommonValues The maximum length of {@link FloatField#commonValues()} that will be attempted
	 * @param maxSuggestions The maximum number of suggestions that will be printed per field. Only the best
	 *                       {@code maxSuggestions} suggestions will be printed.
	 * @param expectMultipleOfCandidates The candidate elements for {@link FloatField#expectMultipleOf()} that will be
	 *                                   attempted. Typical options are e.g. {@code [0.001, 0.01, 0.1, 1.0]}, but the
	 *                                   best value depends on the distribution. You can try to find it with trial &
	 *                                   error.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public void optimize(
			PrintWriter writer, int maxFields, int maxCommonValues,
			int maxSuggestions, double[] expectMultipleOfCandidates
	) {
		int remainingFields = maxFields;
		for (String field : getSortedFields()) {
			if (remainingFields <= 0) break;

			writer.println("Field " + field + ": (x " + count(field) + ")");
			Field<Double> fieldData = mapping.get(field);
			FloatField.Properties properties = (FloatField.Properties) fieldData.properties;

			List<Entry> entries = optimize(field, maxCommonValues, expectMultipleOfCandidates);
			if (entries.get(0).spentBits >= countBits(fieldData, properties)) {
				writer.println("  already optimal");
				writer.println();
				continue;
			} writer.println("  suboptimal: currently uses " + countBits(field) + " bits");

			writer.println("  used interval is [" + fieldData.countMap.keySet().stream().min(Double::compare).get() + ", " +
					fieldData.countMap.keySet().stream().max(Double::compare).get() + "]");

			int remainingSuggestions = maxSuggestions;
			for (Entry entry : entries) {
				if (remainingSuggestions-- <= 0) break;
				writer.println("  " + entry);
			}
			writer.println();
			remainingFields -= 1;
		}
		writer.flush();
	}

	/**
	 * Computes recommendations to optimize {@code field}
	 * @param field The field/description to be optimized, should be an element of {@link #getSortedFields()}
	 * @param maxCommonValues The maximum length of {@link FloatField#commonValues()} that will be attempted
	 * @param expectMultipleOfCandidates The candidate elements for {@link FloatField#expectMultipleOf()} that will be
	 *                                   attempted. Typical options are e.g. {@code [0.001, 0.01, 0.1, 1.0]}, but the
	 *                                   best value depends on the distribution. You can try to find it with trial &
	 *                                   error.
	 */
	public List<Entry> optimize(String field, int maxCommonValues, double[] expectMultipleOfCandidates) {
		List<Entry> entries = new ArrayList<>((1 + maxCommonValues) * 7);

		Field<Double> fieldData = mapping.get(field);
		FloatField.Properties originalProperties = (FloatField.Properties) fieldData.properties;

		List<Double> mostCommonValues = new ArrayList<>(fieldData.countMap.keySet());
		mostCommonValues.sort((a, b) -> -Integer.compare(fieldData.countMap.get(a), fieldData.countMap.get(b)));

		for (double expectMultipleOf : expectMultipleOfCandidates) {
			for (int numCommonValues = 0; numCommonValues <= maxCommonValues; numCommonValues++) {
				double[] commonValues = new double[min(numCommonValues, mostCommonValues.size())];
				for (int index = 0; index < commonValues.length; index++) {
					commonValues[index] = mostCommonValues.get(index);
				}

				for (int digitSize = 0; digitSize <= 7; digitSize++) {
					if (digitSize == 1) continue;

					FloatField.Properties properties = new FloatField.Properties(
							expectMultipleOf, originalProperties.errorTolerance,
							new IntegerField.Properties(
									originalProperties.expectedIntegerMultiple.minValue,
									originalProperties.expectedIntegerMultiple.maxValue,
									false, digitSize, new long[0]
							),
							commonValues
					);
					Entry entry = new Entry();
					entry.digitSize = digitSize;
					entry.expectMultipleOf = expectMultipleOf;
					entry.commonValues = commonValues;
					entry.spentBits = countBits(fieldData, properties);
					entries.add(entry);
				}

				FloatField.Properties properties = new FloatField.Properties(
						expectMultipleOf, originalProperties.errorTolerance,
						new IntegerField.Properties(
								originalProperties.expectedIntegerMultiple.minValue,
								originalProperties.expectedIntegerMultiple.maxValue,
								true, 0, new long[0]
						),
						commonValues
				);
				Entry entry = new Entry();
				entry.digitSize = -1;
				entry.expectMultipleOf = expectMultipleOf;
				entry.commonValues = commonValues;
				entry.spentBits = countBits(fieldData, properties);
				entries.add(entry);
			}
		}

		entries.sort(Comparator.comparingInt(a -> a.spentBits));
		return entries;
	}
}
