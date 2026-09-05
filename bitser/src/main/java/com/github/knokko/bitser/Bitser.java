package com.github.knokko.bitser;

import com.github.knokko.bitser.io.*;
import com.github.knokko.bitser.exceptions.*;
import com.github.knokko.bitser.options.AnalyzePerformance;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.options.DebugBits;
import com.github.knokko.bitser.options.WithParameter;
import com.github.knokko.bitser.distributions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * <p>
 *     This class is the core of the bitser library. Instances of this class can be used to (de)serialize objects.
 *     Furthermore, this class contains some utility methods like {@link #deepCopy}.
 * </p>
 *
 * <h3>Instances</h3>
 * <p>
 *     Each instance of this class contains its own 'reflection cache' for each class that it needed to (de)serialize.
 *     If you create a new Bitser instance every time you (de)serialize an object, each Bitser instance would build
 *     its own reflection cache every time, which potentially wastes time.
 * </p>
 *
 * <p>
 *     You can avoid this double work by reusing Bitser instances. This is especially useful if the same classes are
 *     (de)serialized each time. Since this class is completely thread-safe, you could reuse the same Bitser instance
 *     everywhere, and even make it a singleton (by using the same
 *     {@code public static final Bitser BITSER = new Bitser();} everywhere). Using it like singleton might not be
 *     such a bad idea, since this class doesn't really carry any <i>state</i> (aside from its reflection cache).
 * </p>
 */
public class Bitser {

	/**
	 * When this option is passed to {@link #serialize} or {@link #deserialize}, the (de)serialization will be
	 * backward-compatible, which makes it possible to serialize an object to a file, modify its class (within
	 * reason), and deserialize it. The drawback is that backward-compatible (de)serialization requires more bits, and
	 * is slower.
	 */
	public static final Object BACKWARD_COMPATIBLE = new Object();

	/**
	 * When this option is passed to {@link #serialize}, any encountered {@link SimpleLazyBits} are guaranteed to be
	 * serialized to bytes, even if the bytes from the previous (de)serialization could be used instead. This option
	 * is potentially useful in combination with e.g. {@link IntegerDistributionTracker} to make sure that the lazy
	 * structs are not skipped.
	 */
	public static final Object FORBID_LAZY_SAVING = new Object();

	final BitserCache cache;

	/**
	 * Constructs a new {@link Bitser} instance.
	 */
	public Bitser() {
		this.cache = new BitserCache();
	}

	private void rethrowRecursorException(RecursionException failure) throws IOException {
		Throwable cause = failure.getCause();
		if (cause instanceof InvalidBitValueException) {
			throw new InvalidBitValueException(
					"Invalid value at " + failure.debugInfoStack + ": " + cause.getMessage()
			);
		}
		if (cause instanceof InvalidBitFieldException) {
			throw new InvalidBitFieldException(
					"Invalid field at " + failure.debugInfoStack + ": " + cause.getMessage()
			);
		}
		if (cause instanceof LegacyBitserException) {
			throw new LegacyBitserException(
					"Legacy problem at " + failure.debugInfoStack + ": " + cause.getMessage()
			);
		}
		if (cause instanceof ReferenceBitserException) {
			throw new ReferenceBitserException(
					"Reference problem at " + failure.debugInfoStack + ": " + cause.getMessage()
			);
		}
		if (cause instanceof IOException) {
			throw new IOException(
					"IO exception at " + failure.debugInfoStack + ": " + cause.getMessage(), cause
			);
		}
	}

	/**
	 * Serializes {@code object}, and writes its bits to {@code output}.
	 * @param object The object to be serialized. Its class must be annotated with {@link BitStruct}.
	 * @param output The {@link BitOutputStream} to which the bits of {@code object} should be written
	 * @param withAndOptions In this varargs parameter, you can put additional options, or {@link BitStruct}s. Some
	 *                       options are {@link #BACKWARD_COMPATIBLE} and {@link #FORBID_LAZY_SAVING}. Furthermore,
	 *                       you can add {@link BitStruct}s. These additional structs will <b>not</b> be serialized,
	 *                       but their {@link com.github.knokko.bitser.field.ReferenceFieldTarget} fields can be used
	 *                       by the reference fields of {@code object}.
	 * @throws IOException If {@code output} throws an IOException
	 * @throws BitserException If {@code object} or any of its children are invalid {@link BitStruct}s, or if any of
	 *                       their fields have an invalid value.
	 * @throws IllegalArgumentException If {@code withAndOptions} are invalid
	 */
	public void serialize(
			Object object, BitOutputStream output, Object... withAndOptions
	) throws IOException, BitserException, IllegalArgumentException {
		try {
			rawSerialize(object, output, withAndOptions);
		} catch (RecursionException failure) {
			rethrowRecursorException(failure);
			throw failure;
		}
	}

	private void rawSerialize(Object object, BitOutputStream output, Object... withAndOptions) {
		boolean backwardCompatible = false;
		boolean forbidLazySaving = false;
		IntegerDistributionTracker integerDistribution = null;
		FloatDistributionTracker floatDistribution = null;
		Map<String, Object> withParameters = new HashMap<>();

		for (Object withObject : withAndOptions) {
			if (withObject == BACKWARD_COMPATIBLE) backwardCompatible = true;
			if (withObject == FORBID_LAZY_SAVING) forbidLazySaving = true;
			if (withObject instanceof IntegerDistributionTracker) {
				integerDistribution = (IntegerDistributionTracker) withObject;
			}
			if (withObject instanceof FloatDistributionTracker) {
				floatDistribution = (FloatDistributionTracker) withObject;
			}
			if (withObject instanceof WithParameter parameter) {
				if (withParameters.containsKey(parameter.key())) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key());
				}
				withParameters.put(parameter.key(), parameter.value());
			}
		}

		if (backwardCompatible) {
			LegacyClasses legacy = new LegacyClasses();
			new UsedStructCollector(cache, legacy, object.getClass()).collect();

			try {
				output.setMarker("Prepare legacy classes");
				serialize(legacy, output, new WithParameter("legacy-classes", legacy));
				output.setMarker("Prepare main content");
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		}

		ArrayList<Object> withObjects = new ArrayList<>();
		ArrayList<LazyReferenceTargets> lazyTargets = new ArrayList<>();
		for (Object maybeWithObject : withAndOptions) {
			if (maybeWithObject == BACKWARD_COMPATIBLE || maybeWithObject == FORBID_LAZY_SAVING) continue;
			if (maybeWithObject instanceof WithParameter || maybeWithObject instanceof DistributionTracker) continue;
			if (maybeWithObject instanceof DebugBits || maybeWithObject instanceof AnalyzePerformance) continue;
			if (maybeWithObject instanceof LazyReferenceTargets[] targetsArray) {
				Collections.addAll(lazyTargets, targetsArray);
				continue;
			}
			withObjects.add(maybeWithObject);
		}

		Serializer serializer = new Serializer(
				this, withAndOptions, withParameters, output, backwardCompatible, object,
				forbidLazySaving, integerDistribution, floatDistribution
		);
		serializer.references.setLazyTargets(lazyTargets);
		serializer.references.setWithObjects(withObjects);
		serializer.run();
	}

	/**
	 * Serializes {@code object} to a <b>byte[]</b>. This is sometimes more convenient than {@link #serialize}.
	 * @param object The object to be serialized. Its class must be annotated with {@link BitStruct}.
	 * @param withAndOptions See {@link #serialize}
	 * @throws BitserException If {@code object} or any of its children are invalid {@link BitStruct}s, or if any of
	 *                       their fields have an invalid value.
	 * @throws IllegalArgumentException If {@code withAndOptions} are invalid
	 */
	public byte[] toBytes(Object object, Object... withAndOptions) {
		DebugBits debug = null;
		for (Object option : withAndOptions) {
			if (option instanceof DebugBits) debug = (DebugBits) option;
			if (option instanceof AnalyzePerformance) {
				System.out.println("Ignoring AnalyzePerformance during toBytes(). It can only be used in fromBytes().");
			}
		}

		try {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			BitOutputStream bitOutput = debug != null ?
					new DebugBitOutputStream(byteOutput, debug.writer(), debug.aggressiveFlush()) :
					new BitOutputStream(byteOutput);
			serialize(object, bitOutput, withAndOptions);
			bitOutput.finish();
			return byteOutput.toByteArray();
		} catch (IOException shouldNotHappen) {
			throw new UnexpectedBitserException("ByteArrayOutputStream threw an IOException?");
		}
	}

	/**
	 * Deserializes an instance of {@code objectClass} that was previously serialized using {@link #serialize}.
	 * @param input The {@link BitInputStream} from which the bits should be read.
	 * @param withAndOptions See {@link #serialize}. Requirements:
	 *                       <ul>
	 *                           <li>
	 *                               All {@link BitStruct}s passed to {@link #serialize} must also be passed to this
	 *                               method, <i>in the same order</i>.
	 *                           </li>
	 *                           <li>
	 *                               It must not contain any {@link BitStruct}s that were not passed to
	 *                               {@link #serialize}.
	 *                           </li>
	 *                           <li>
	 *                               The {@link #BACKWARD_COMPATIBLE} option must be included if and only if it was
	 *                               passed to {@link #serialize}.
	 *                           </li>
	 *                       </ul>
	 * @throws IOException If {@code input} throws an IOException
	 * @throws BitserException If the data from {@code input} cannot be used to deserialize an instance of
	 *                       {@code objectClass} (using the given {@code withAndOptions}). This could happen when you
	 *                       try to deserialize an instance of the wrong class, or are using data that wasn't
	 *                       generated by {@link #serialize}.
	 * @throws IllegalArgumentException If {@code withAndOptions} are invalid
	 */
	public <T> T deserialize(
			Class<T> objectClass, BitInputStream input, Object... withAndOptions
	) throws IOException, BitserException, IllegalArgumentException {
		try {
			return rawDeserialize(objectClass, input, withAndOptions);
		} catch (RecursionException failure) {
			rethrowRecursorException(failure);
			throw failure;
		}
	}

	private <T> T rawDeserialize(
			Class<T> objectClass, BitInputStream input, Object... withAndOptions
	) throws IOException {
		CollectionSizeLimit sizeLimit = null;
		boolean backwardCompatible = false;
		Map<String, Object> withParameters = new HashMap<>();
		ArrayList<Object> withObjects = new ArrayList<>();
		var lazyTargets = new ArrayList<LazyReferenceTargets>();

		for (Object maybeWithObject : withAndOptions) {
			if (maybeWithObject == BACKWARD_COMPATIBLE) {
				backwardCompatible = true;
				continue;
			}
			if (maybeWithObject instanceof CollectionSizeLimit) {
				if (sizeLimit != null) throw new IllegalArgumentException("Encountered multiple size limits");
				sizeLimit = (CollectionSizeLimit) maybeWithObject;
				continue;
			}
			if (maybeWithObject instanceof WithParameter parameter) {
				if (withParameters.containsKey(parameter.key())) {
					throw new IllegalArgumentException("Duplicate with parameter " + parameter.key());
				}
				withParameters.put(parameter.key(), parameter.value());
				continue;
			}
			if (maybeWithObject == null || maybeWithObject instanceof DebugBits ||
					maybeWithObject instanceof AnalyzePerformance
			) {
				continue;
			}
			if (maybeWithObject instanceof LazyReferenceTargets[] targetsArray) {
				Collections.addAll(lazyTargets, targetsArray);
				continue;
			}
			if (maybeWithObject == FORBID_LAZY_SAVING) {
				System.out.println("Ignoring FORBID_LAZY_SAVING option in Bitser.deserialize");
				continue;
			}
			if (maybeWithObject instanceof DistributionTracker) {
				System.out.println("Ignoring DistributionTracker option in Bitser.deserialize");
				continue;
			}

			withObjects.add(maybeWithObject);
		}

		LegacyClasses legacy = null;
		if (backwardCompatible) {
			input.setMarker("Prepare legacy classes");
			legacy = deserialize(LegacyClasses.class, input, sizeLimit);
			input.setMarker("Prepare main content");
		}

		BitStructWrapper<T> wrapper = cache.getWrapper(objectClass);

		if (legacy != null) {
			BackDeserializer deserializer = new BackDeserializer(
					this, input, legacy, sizeLimit, withParameters, wrapper
			);
			deserializer.references.setLazyTargets(lazyTargets);
			deserializer.references.setWithObjects(withObjects);
			deserializer.run();
			//noinspection unchecked
			return (T) deserializer.rootStruct;
		} else {
			Deserializer deserializer = new Deserializer(this, input, sizeLimit, withParameters, wrapper);
			deserializer.references.setLazyTargets(lazyTargets);
			deserializer.references.setWithObjects(withObjects);
			deserializer.run();
			//noinspection unchecked
			return (T) deserializer.rootStruct;
		}
	}

	/**
	 * Deserializes an instance of {@code objectClass} that was previously serialized using {@link #serialize}. This
	 * method is basically the same as {@link #deserialize}, but reads from a <b>byte[]</b> instead of a
	 * {@link BitInputStream}.
	 * @param bytes The bytes from which the object should be serialized. This byte array should be generated by
	 *              {@link #toBytes}   .
	 * @param withAndOptions See {@link #deserialize}.
	 * @throws BitserException If the data from {@code bytes} cannot be used to deserialize an instance of
	 *                       {@code objectClass} (using the given {@code withAndOptions}). This could happen when you
	 *                       try to deserialize an instance of the wrong class, or are using data that wasn't
	 *                       generated by {@link #serialize} or {@link #toBytes}..
	 * @throws IllegalArgumentException If {@code withAndOptions} are invalid
	 */
	public <T> T fromBytes(
			Class<T> objectClass, byte[] bytes, Object... withAndOptions
	) throws BitserException, IllegalArgumentException {
		DebugBits debug = null;
		AnalyzePerformance analyze = null;
		for (Object option : withAndOptions) {
			if (option instanceof DebugBits debugOption) debug = debugOption;
			if (option instanceof AnalyzePerformance analyzeOption) analyze = analyzeOption;
		}
		if (debug != null && analyze != null) {
			throw new IllegalArgumentException("DebugBits and AnalyzePerformance are mutually exclusive");
		}
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
			BitInputStream bitInput;
			if (debug != null) bitInput = new DebugBitInputStream(byteInput, debug.writer(), debug.aggressiveFlush());
			else if (analyze != null) bitInput = new AnalysisBitInputStream(byteInput, analyze);
			else bitInput = new BitInputStream(byteInput);
			T result = deserialize(objectClass, bitInput, withAndOptions);
			bitInput.close();
			return result;
		} catch (IOException shouldNotHappen) {
			throw new IllegalArgumentException("bytes is too short or invalid/corrupted", shouldNotHappen);
		}
	}

	/**
	 * Creates a <i>shallow</i> copy of {@code object}. This object will have the same class as {@code object}, but it
	 * will have a different identity. Its <i>fields</i> will have the <i>same</i> identity. Note that this method only
	 * copies the fields that are annotated with a bitser annotation.
	 * @param object The object to be copied, whose class must be annotated with {@link BitStruct}
	 * @return A shallow copy of {@code object}
	 * @param <T> The type/class of {@code object}
	 */
	public <T> T shallowCopy(T object) {
		//noinspection unchecked
		return (T) cache.getWrapper(object.getClass()).shallowCopy(object);
	}

	/**
	 * Creates a <i>deep</i> (nested/recursive) copy of {@code object}, by serializing it, and immediately deserializing
	 * it. This is a quite inefficient method of copying, but quite convenient for the unit tests of bitser.
	 * {@link #deepCopy} is usually a better fit for library users.
	 * @param object The object to be copied, whose class must be annoated with {@link BitStruct}
	 * @param with The {@code withAndOptions} that should be passed to {@link #serialize} and {@link #deserialize}
	 * @return A deep copy of {@code object}
	 * @param <T> The type/class of {@code object}
	 * @throws BitserException If {@link #serialize} or {@link #deserialize} throws a {@link BitserException}
	 * @throws IllegalArgumentException If {@link #serialize} or {@link #deserialize} throws an
	 * {@link IllegalArgumentException}
	 */
	public <T> T stupidDeepCopy(
			T object, Object... with
	) throws BitserException, IllegalArgumentException {
		//noinspection unchecked
		return (T) fromBytes(object.getClass(), toBytes(object, with), with);
	}

	/**
	 * <p>
	 *     Determines whether the bit structs {@code a} and {@code b} are deeply (recursively) equal to each other.
	 * </p>
	 *
	 * <p>
	 *     This comparison will only compare the fields that are annotated with {@code @BitField} (or a similar
	 *     annotation). All other fields are ignored. Furthermore, all methods are ignored, even those that are
	 *     annotated with {@code @BitField}.
	 * </p>
	 *
	 * <h3>Reference equality</h3>
	 * <p>
	 *     References in {@code a} that point to a target in {@code a} are considered equal to the corresponding
	 *     references in {@code b} when those references point to the same corresponding target in {@code b}.
	 *     References that point to objects <b>outside</b> of {@code a} and {@code b} are considered equal when they
	 *     point to exactly the same external object.
	 * </p>
	 *
	 * <h3>Warning about <b>HashSet</b>s and <b>HashMap</b>s</h3>
	 * <p>
	 *     If {@code a}, {@code b}, or any of their descendants contains hash-based collections like {@link HashSet}
	 *     or {@link HashMap}, you need to make sure that their elements or keys implement {@link Object#hashCode()}
	 *     and {@link Object#equals(Object)}. Without them, this method may return {@code false}, even when {@code a}
	 *     is equal to {@code b}.
	 * </p>
	 */
	public boolean deepEquals(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		BitStructWrapper<?> wrapperA = cache.getWrapper(a.getClass());
		BitStructWrapper<?> wrapperB = cache.getWrapper(b.getClass());
		if (wrapperA != wrapperB) return false;

		var comparator = new DeepComparator(a, b, wrapperA, this);
		return comparator.equals();
	}

	/**
	 * Computes a hash code of {@code value}, based on its fields that are annotated with a bitser annotation. This
	 * is going to be less efficient than a handwritten hash code implementation, but it can save you some typing.
	 *
	 * <h3>Warning about <b>HashSet</b>s and <b>HashMap</b>s</h3>
	 * <p>
	 *     If {@code value} or any of its descendants contains hash-based collections like {@link HashSet}
	 *     or {@link HashMap}, you need to make sure that their elements or keys implement {@link Object#hashCode()}
	 *     and {@link Object#equals(Object)}. Without them, this method may return different hash codes for equivalent
	 *     values, when the iteration order of their maps or sets is different.
	 * </p>
	 * @param value The value whose hash code should be computed
	 * @return A hash code for {@code value}
	 */
	public int hashCode(Object value) {
		if (value == null) return 0;
		var computer = new HashComputer(value, this);
		computer.feedHash();
		byte[] byteHash = computer.digest.digest();
		return ByteBuffer.wrap(byteHash).getInt(0);
	}

	/**
	 * Creates a <i>deep</i> (nested/recursive) copy of {@code original} and its children.
	 * <ul>
	 *     <li>The {@code original} must be a {@link BitStruct}</li>
	 *     <li>Only the fields annotated with {@code @BitField} will be copied.</li>
	 *     <li>
	 *         References to targets <i>within</i> {@code original} will be set to the corresponding copied target.
	 *     </li>
	 *     <li>
	 *         References to targets <i>outside</i> {@code original} (e.g. 'with' objects) will <b>not</b> be copied:
	 *         the copied reference will have the same identity as the original reference.
	 *     </li>
	 * </ul>
	 *
	 * <h3>Warning about <b>HashSet</b>s and <b>HashMap</b>s</h3>
	 * <p>
	 *     If {@code original} or any of its descendants contains hash-based collections like {@link HashSet}
	 *     or {@link HashMap}, you may want to make sure that their elements or keys implement {@link Object#hashCode()}
	 *     and {@link Object#equals(Object)}. Without them, there is no guarantee that the iteration order of the copy
	 *     is the same as the iteration order of the original. (The iteration order would be determined by the identity
	 *     hash codes, which are not really deterministic.) This would also cause their results of e.g. {@link #toBytes}
	 *     to change, since this depends on the iteration order of the collections.
	 * </p>
	 * @param original The struct to be copied
	 * @param withParameters The with parameters that should be passed to {@link BitPostInit.Context} and
	 *                       {@link com.github.knokko.bitser.field.FunctionContext}. These parameters are completely
	 *                       optional. If no (child) struct implements {@link BitPostInit}, nor has any methods
	 *                       annotated with {@code @BitField}, the {@code withParameters} are useless/ignored.
	 * @return A deep copy of {@code original}
	 * @param <T> The type of {@code original}
	 */
	public <T> T deepCopy(T original, WithParameter...withParameters) {
		Map<String, Object> withMapping = new HashMap<>();
		for (var parameter : withParameters) {
			withMapping.put(parameter.key(), parameter.value());
		}

		DeepCopyMachine machine = new DeepCopyMachine(original, this, withMapping);
		machine.run();
		//noinspection unchecked
		return (T) machine.destinationRoot;
	}

	/**
	 * <p>
	 *     Collects all instances of the keys of {@code destination} that occur inside {@code rootStruct} or any of its
	 *     children.
	 * </p>
	 * <p>
	 *     For instance, to find all instances of {@code ExampleClass} or {@code ExampleInterface}, you should create
	 *     a Map that maps {@code ExampleClass} to an empty collection, and maps {@code ExampleInterface} to
	 *     <i>another</i> empty collection. Bitser will add any instances of {@code ExampleClass} and
	 *     {@code ExampleInterface} to their respective collections, before this method returns.
	 * </p>
	 *
	 * <h3>Recommendations</h3>
	 * <p>
	 *     The concrete types of these collections are not very important. {@code ArrayList} is the most standard
	 *     choice, but you could also use e.g. {@code HashSet} to automatically eliminate duplicates.
	 * </p>
	 * <p>
	 *     It is recommended to put <i>empty</i> collections, but this is not required. If the collections are
	 *     non-empty, bitser will simply <i>add</i> the instances that it found to the non-empty collection.
	 * </p>
	 * <p>
	 *     If you put the same collection instance at two different classes/keys, that collection will contain the
	 *     instances of both classes after this method returns.
	 * </p>
	 *
	 * <h3>References</h3>
	 * <p>
	 *     This method will <b>skip</b> all reference fields that it encounters, which avoids duplicates and potentially
	 *     endless loops. Instead, the encountered references will be put into {@code referencesDestination}. The
	 *     children of the references will <b>not</b> be scanned though.
	 * </p>
	 *
	 * <h3>Reference targets</h3>
	 * <p>
	 *     The reference <b>targets</b> are treated just like any other field, so they can still be
	 *     collected. So, if there are 5 references to the same target, the target will be searched (and possibly
	 *     collected) exactly once.
	 * </p>
	 * @param rootStruct The struct object in which bitser should search for instances of the desired classes
	 * @param destination The map that should contain a key for each class of interest
	 * @param referencesDestination Similar to {@code destination}. The encountered references will be put in this map.
	 * @param withParameters The with parameters that should be passed to
	 *                       {@link com.github.knokko.bitser.field.FunctionContext}. These parameters are completely
	 *                       optional, and only useful if there are any methods annotated with {@code @BitField} and
	 *                       a parameter of type {@code FunctionContext}.
	 */
	public void collectInstances(
			Object rootStruct,
			Map<Class<?>, Collection<Object>> destination,
			Map<Class<?>, Collection<Object>> referencesDestination,
			WithParameter...withParameters
	) {
		Map<String, Object> withMapping = new HashMap<>();
		for (var parameter : withParameters) {
			withMapping.put(parameter.key(), parameter.value());
		}

		for (var destinationClass : destination.keySet()) {
			if (destinationClass.isPrimitive()) {
				throw new IllegalArgumentException("Don't use primitive classes: use their boxed/wrapper type instead");
			}
		}

		for (var destinationClass : referencesDestination.keySet()) {
			if (destinationClass.isPrimitive()) {
				throw new IllegalArgumentException("Don't use primitive classes: use their boxed/wrapper type instead");
			}
		}

		new InstanceCollector(rootStruct, destination, referencesDestination, this, withMapping).run();
	}

	static List<Object> getOptionsWithoutWithObjects(Object[] withAndOptions) {
		var optionList = new ArrayList<>();
		for (var candidateOption : withAndOptions) {
			if (candidateOption == null) continue;
			if (candidateOption.getClass().isAnnotationPresent(BitStruct.class)) continue;
			if (candidateOption.getClass() == LazyReferenceTargets[].class) continue;
			optionList.add(candidateOption);
		}
		return optionList;
	}
}
