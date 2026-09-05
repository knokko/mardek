package com.github.knokko.bitser.distributions;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static java.lang.Math.min;

/**
 * A {@link DistributionTracker} for {@link IntegerField}s. You can use it like this: {@code
 * var distribution = new IntegerDistributionTracker();
 * bitser.boBytes(someStruct, distribution);
 * distribution.optimize(new PrintWriter(System.out), 5, 2, 5);
 * }
 */
public class IntegerDistributionTracker extends DistributionTracker<Long> {

	/**
	 * An entry of the list returned by {@link #optimize(String, int)}. Each entry predicts how many bits
	 * would have been needed to serialize all the values, if a specific {@link IntegerField} configuration were used.
	 */
	public static class Entry {

		/**
		 * The {@link IntegerField#digitSize()} that was used
		 */
		public int digitSize;

		/**
		 * The {@link IntegerField#commonValues()} that were used
		 */
		public long[] commonValues;

		/**
		 * The number of bits that would be needed to serialize all the values of the corresponding integer
		 * field, if this {@link IntegerField} configuration would be used.
		 */
		public int spentBits;

		@Override
		public String toString() {
			return "(spentBits=" + spentBits + ", digitSize=" + (digitSize == -1 ? "uniform" : digitSize) +
					", commonValues=" + Arrays.toString(commonValues) + ")";
		}
	}

	@Override
	public List<String> getSortedFields() {
		List<String> fields = new ArrayList<>(mapping.keySet());
		fields.sort((a, b) -> -Long.compare(countBits(a), countBits(b)));
		return fields;
	}

	private int countBits(String field) {
		Field<Long> fieldData = mapping.get(field);
		return countBits(fieldData, (IntegerField.Properties) fieldData.properties);
	}

	/**
	 * Prints recommendations to optimize the top-{@code maxFields} {@link IntegerField}s
	 * @param writer The writer to which the recommendations are printed, e.g. {@code new PrintWriter(System.out)}
	 * @param maxFields The maximum number of fields for which recommendations will be printed.
	 * @param maxCommonValues The maximum length of {@link IntegerField#commonValues()} that will be attempted
	 * @param maxSuggestions The maximum number of suggestions that will be printed per field. Only the best
	 *                       {@code maxSuggestions} suggestions will be printed.
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public void optimize(PrintWriter writer, int maxFields, int maxCommonValues, int maxSuggestions) {
		int remainingFields = maxFields;
		for (String field : getSortedFields()) {
			if (remainingFields <= 0) break;

			writer.println("Field " + field + ": (x " + count(field) + ")");
			Field<Long> fieldData = mapping.get(field);
			IntegerField.Properties properties = (IntegerField.Properties) fieldData.properties;

			List<Entry> entries = optimize(field, maxCommonValues);
			if (entries.get(0).spentBits >= countBits(fieldData, properties)) {
				writer.println("  already optimal");
				writer.println();
				continue;
			} writer.println("  suboptimal: currently uses " + countBits(field) + " bits");

			writer.println("  allowed interval is [" + properties.minValue + ", " + properties.maxValue + "]");
			writer.println("  used interval is [" + fieldData.countMap.keySet().stream().min(Long::compare).get() + ", " +
					fieldData.countMap.keySet().stream().max(Long::compare).get() + "]");

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
	 * @param maxCommonValues The maximum length of {@link IntegerField#commonValues()} that will be attempted
	 */
	public List<Entry> optimize(String field, int maxCommonValues) {
		List<Entry> entries = new ArrayList<>((1 + maxCommonValues) * 7);

		Field<Long> fieldData = mapping.get(field);
		IntegerField.Properties originalProperties = (IntegerField.Properties) fieldData.properties;

		List<Long> mostCommonValues = new ArrayList<>(fieldData.countMap.keySet());
		mostCommonValues.sort((a, b) -> -Integer.compare(fieldData.countMap.get(a), fieldData.countMap.get(b)));

		for (int numCommonValues = 0; numCommonValues <= maxCommonValues; numCommonValues++) {
			long[] commonValues = new long[min(numCommonValues, mostCommonValues.size())];
			for (int index = 0; index < commonValues.length; index++) {
				commonValues[index] = mostCommonValues.get(index);
			}

			for (int digitSize = 0; digitSize <= 7; digitSize++) {
				if (digitSize == 1) continue;

				IntegerField.Properties properties = new IntegerField.Properties(
						originalProperties.minValue, originalProperties.maxValue,
						false, digitSize, commonValues
				);
				Entry entry = new Entry();
				entry.digitSize = digitSize;
				entry.commonValues = commonValues;
				entry.spentBits = countBits(fieldData, properties);
				entries.add(entry);
			}

			IntegerField.Properties properties = new IntegerField.Properties(
					originalProperties.minValue, originalProperties.maxValue,
					true, 0, commonValues
			);
			Entry entry = new Entry();
			entry.digitSize = -1;
			entry.commonValues = commonValues;
			entry.spentBits = countBits(fieldData, properties);
			entries.add(entry);
		}

		entries.sort(Comparator.comparingInt(a -> a.spentBits));
		return entries;
	}

	private int countBits(Field<Long> fieldData, IntegerField.Properties properties) {
		BitCountStream bitCounter = new BitCountStream();
		fieldData.countMap.forEach((value, occurrences) -> {
			for (int counter = 0; counter < occurrences; counter++) {
				try {
					IntegerBitser.encodeInteger(value, properties, bitCounter);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		return bitCounter.getCounter();
	}
}
