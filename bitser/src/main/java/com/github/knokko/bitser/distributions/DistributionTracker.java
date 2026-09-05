package com.github.knokko.bitser.distributions;

import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.IntegerField;

import java.io.PrintWriter;
import java.util.*;

/**
 * Distribution trackers can be used in the <i>withAndOptions</i> to {@link Bitser#serialize}.
 * When present, bitser will track how many times each value is saved. Afterward, this information can be used to
 * compute the most optimal way to encode their fields (e.g. choose the best {@link IntegerField#digitSize()} or
 * {@link IntegerField#commonValues()}). Currently, there are only two implementations of this class:
 * <ul>
 *     <li>{@link IntegerDistributionTracker} to optimize {@link IntegerField}s</li>
 *     <li>{@link FloatDistributionTracker} to optimize {@link com.github.knokko.bitser.field.FloatField}s</li>
 * </ul>
 * @param <T> The value type whose distribution is being tracked
 */
public abstract class DistributionTracker<T> {

	/**
	 * Maps the name/description of each tracked field to the information about that field. The exact format of the keys
	 * is not specified, but you can safely use any element of {@link #getSortedFields()}.
	 */
	public final Map<String, Field<T>> mapping = new HashMap<>();

	// This constructor is package-private to prevent users from creating unknown subclasses, which bitser wouldn't
	// support anyway.
	DistributionTracker() {}

	/**
	 * Registers a value for the given description/key
	 * @param description The description/identifier of the field that has {@code value}
	 * @param value The value of the field/element
	 * @param properties The properties of the field: should e.g. be {@link IntegerField.Properties} for
	 *                   {@link IntegerDistributionTracker}
	 */
	public void insert(String description, T value, Object properties) {
		mapping.computeIfAbsent(description, key -> new Field<>(value.getClass(), properties)).insert(value);
	}

	/**
	 * Counts the number of times the given field was saved. For instance, when 100 instances of some class
	 * {@code InnerStruct} are serialized, each of its fields will be counted 100 times. The fields that occur most
	 * often are typically good candidates to optimize.
	 */
	public long count(String field) {
		return mapping.get(field).count();
	}

	/**
	 * Creates a list containing the description of all serialized fields, sorted by their number of occurrences, in
	 * descending order. So, the most-frequently-serialized field can be found at {@code getSortedFields().get(0)}
	 */
	public List<String> getSortedFields() {
		List<String> fields = new ArrayList<>(mapping.keySet());
		fields.sort((a, b) -> -Long.compare(count(a), count(b)));
		return fields;
	}

	/**
	 * Prints how many times each field occurs. The most-frequently-serialized fields are printed first. Note that you
	 * can use {@code printFieldOccurrences(new PrintWriter(System.out))} to print to stdout.
	 */
	public void printFieldOccurrences(PrintWriter writer) {
		for (String field : getSortedFields()) {
			writer.println(count(field) + " x " + field);
		}
		writer.flush();
	}

	protected List<T> getSortedValues(Map<T, Integer> countMap) {
		List<T> values = new ArrayList<>(countMap.keySet());
		values.sort(Comparator.comparingInt(countMap::get).reversed());
		return values;
	}

	/**
	 * Print the most common {@code maxValuesPerField} values of the {@code maxFields} most-frequently-serialized
	 * fields.
	 * @param writer The fields and values will be printed to {@code writer}. Note that you can use
	 *               {@code new PrintWriter(System.out)} to print to stdout.
	 * @param maxFields The maximum number of fields whose most-frequently-occurring values will be printed
	 * @param maxValuesPerField The maximum number of most-frequently-occurring values that will be printed per field
	 */
	public void printFieldValueOccurrences(PrintWriter writer, int maxFields, int maxValuesPerField) {
		int remainingFields = maxFields;
		for (String field : getSortedFields()) {
			if (remainingFields-- <= 0) break;
			Field<T> fieldData = mapping.get(field);
			writer.println(count(field) + " x " + field + ":");

			int remainingValues = maxValuesPerField;
			for (T value : getSortedValues(fieldData.countMap)) {
				if (remainingValues-- <= 0) break;
				writer.println("  " + fieldData.countMap.get(value) + " x " + value);
			}
		}
		writer.flush();
	}

	/**
	 * This class stores the information and values for one field.
	 * @param <T> The type of value stored in the field, e.g. {@link Integer} for {@link IntegerDistributionTracker}
	 */
	public static class Field<T> {

		/**
		 * The type of the field, which may or may not be primitive.
		 */
		public final Class<?> type;

		/**
		 * The field properties, which should e.g. be {@link IntegerField.Properties} for integer fields
		 */
		public final Object properties;

		/**
		 * Maps each value to its number of occurrences. Note that this map will only contain the values that occurred
		 * at least once.
		 */
		public final Map<T, Integer> countMap = new HashMap<>();

		/**
		 * Creates a new Field, without any values yet. Use {@link #insert(Object)} to register value occurrences.
		 */
		public Field(Class<?> type, Object properties) {
			this.type = type;
			this.properties = properties;
		}

		/**
		 * Increments the counter for {@code value} in the {@link #countMap}
		 */
		public void insert(T value) {
			countMap.put(value, countMap.getOrDefault(value, 0) + 1);
		}

		/**
		 * Computes the total number of occurrences of this field.
		 */
		public long count() {
			return countMap.values().stream().mapToLong(x -> x).sum();
		}
	}
}
