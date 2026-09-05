package com.github.knokko.bitser.field;

import java.lang.annotation.*;

/**
 * <p>
 *     This annotation can be used on arrays, collections, or maps. It is needed to get more fine-grained control over
 *     the serialization of such collections.
 * </p>
 *
 * <p>
 *     By default, when you annotate e.g. a field of type <b>int[]</b> with an {@code @IntegerField(...)}, the
 *     {@code @IntegerField} is applied to the <i>elements</i> of the array, and not to the array itself.
 *     This is desirable most of the time, but there are rare cases where you might want to annotate the array itself
 *     (e.g. to make it a reference (target)).
 * </p>
 *
 * <p>
 *     Furthermore, this annotation is needed for annotating the keys or values of maps. Map fields don't allow
 *     'loose' annotations since it's unclear whether the annotation should apply to the keys, or to the values.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     If the field of the original serialized struct is a collection or array, the corresponding field of the new
 *     deserialized struct must also be a collection or array. Furthermore, the elements of the old collection/array
 *     must be compatible with the elements of the new collection/array. Some examples:
 * </p>
 * <ul>
 *     <li>Converting {@code ArrayList<String>} to {@code LinkedList<String>} or {@code String[]} is allowed.</li>
 *     <li>Converting {@code int[]} to {@code ArrayList<Long>} is allowed.</li>
 *     <li>Converting {@code int[]} to {@code String[]} is <b>not</b> allowed.</li>
 *     <li>Converting {@code int[][]} to {@code ArrayList<LinkedList<Integer>>} is allowed.</li>
 *     <li>
 *         Converting {@code long[]} to {@code byte[]} is allowed if and only if all the <b>long</b>s are at
 *         least {@code Byte.MIN_VALUE} and at most {@code Byte.MAX_VALUE}.
 *     </li>
 * </ul>
 * <p>
 *     If the field of the original serialized struct is a map, the corresponding field of the new deserialized struct
 *     must also be a map. Furthermore, the keys of the old map must be compatible with the keys of the new map, and
 *     the values of the old map must be compatible with the values of the new map. Some examples:
 * </p>
 * <ul>
 *     <li>Converting {@code HashMap<UUID, Long>} to {@code TreeMap<UUID, Long>} is allowed.</li>
 *     <li>Converting {@code HashMap<UUID, Long>} to {@code HashMap<Long, UUID>} is <b>not</b> allowed.</li>
 *     <li>Converting {@code HashMap<Long, Long>} to {@code ArrayList<Long>} is <b>not</b> allowed.</li>
 *     <li>Converting {@code HashMap<Integer, byte[]>} to {@code TreeMap<Integer, ArrayList<Byte>>} is allowed.</li>
 *     <li>
 *         Converting {@code HashMap<Integer, byte[]>} to {@code TreeMap<Integer, ArrayList<byte[]>>}
 *         is <b>not</b>allowed.
 *     </li>
 * </ul>
 */
@Repeatable(NestedFieldSettings.class)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface NestedFieldSetting {

    /**
     * This path specifies to which target this annotation should apply. The basic rules are:
     * <ul>
     *     <li>The empty string targets the collection itself.</li>
     *     <li>The string "c" targets the elements of the collection or array.</li>
     *     <li>The string "k" targets the keys of the map.</li>
     *     <li>The string "v" targets the values of the map.</li>
     * </ul>
     * For nested arrays, collections, and maps, you can 'recursively' define the path. For instance:
     * <ul>
     *     <li>
     *         The string "ck" targets the keys of the elements. You can use this on e.g. a field of type
     *         {@code ArrayList<HashMap<String, Integer>>} to target the string keys.
     *     </li>
     *     <li>
     *         The string "vc" targets the elements of the values. You can use this on e.g. a field of type
     *         {@code HashMap<String, long[]>} to target the elements of the <b>long[]</b>s.
     *     </li>
     *     <li>
     *         The string "vcvk" targets the keys of the values of the elements of the values. You can use this on e.g.
     *         a field of type {@code TreeMap<Integer, LinkedList<ConcurrentHashMap<UUID, HashMap<Byte, Short>>>>} to
     *         targets the {@code Short} values.
     *     </li>
     * </ul>
     * Using these examples, you should be able to target anything in a nested map/collection/array, but I would still
     * not recommend creating such 'monster' collections for code quality reasons.
     */
    String path();

    /**
     * <p>
     *     You can use the field name to fetch the annotations from a <b>static final</b> field named {@code fieldName},
     *     and apply them to the target specified by {@link #path()}. The type and value of this static field are
     *     irrelevant, so you can just make it a static <b>boolean</b> that is always <b>false</b>.
     * </p>
     * <p>
     *     To give an example, the following code can be used to tell bitser that the keys of a map are uniformly
     *     distributed between 0 and 100:
     * </p>
     * <ul>
     *     <li>{@code
     *         @IntegerField(expectUniform=true, minValue = 0, maxValue = 0)
     *         @SuppressWarnings("unused")
     *         private static final boolean KEY_PROPERTIES = false; }
     *     </li>
     *     <li>
     *         {@code @NestedFieldSetting(path = "k", fieldName = "KEY_PROPERTIES") HashMap<Integer, Integer> example;}
     *     </li>
     * </ul>
     */
    String fieldName() default "";

    /**
     * <p>
     *     Whether the target specified by {@link #path()} is optional.
     * </p>
     *
     * <p>
     *     For instance, you can use this setting to determine whether the {@code ArrayList} or the {@code String}s in
     *     an {@code ArrayList<String>} are optional:
     * </p>
     * <ul>
     *     <li>{@code @NestedFieldSetting(path = "", optional = true)} makes the {@code ArrayList} optional.</li>
     *     <li>{@code @NestedFieldSetting(path = "c", optional = true)} makes the {@code String}s optional.</li>
     * </ul>
     *
     * <p>
     *     Everything is required by default, but you can make them optional using this setting.
     * </p>
     */
    boolean optional() default false;

    /**
     * <p>
     *     Whether the target collection specified by {@link #path()} should be saved as a <b>byte[]</b>.
     * </p>
     * <p>
     *     This is a potential optimization that can be used to speed up the serialization of large primitive arrays,
     *     at the cost of losing options to reduce its size.
     * </p>
     * When {@code writeAsBytes} is true:
     * <ul>
     *     <li>
     *         The {@link #path()} must point to a primitive array (e.g. <b>byte[]</b> or <b>int[]</b>):
     *         <ul>
     *             <li>The path should be "" when the field is of type <b>byte[]</b>.</li>
     *             <li>The path should be "c" when the field is of type {@code ArrayList<byte[]>}.</li>
     *         </ul>
     *     </li>
     *     <li>
     *         The contents will be converted to a <b>byte[]</b> (if needed) before being written, which can improve the
     *         encoding speed considerably (between 10 and 100 times on my machine). An <b>int[]</b> would be converted
     *         to a <b>byte[]</b> of 4 times its length. A <b>long[]</b> would be converted to a <b>byte[]</b> of 8
     *         times its length, etc...
     *     </li>
     *     <li>
     *         The <b>byte[]</b> will be written using {@link java.io.OutputStream#write(byte[])}
     *     </li>
     * </ul>
     */
    boolean writeAsBytes() default false;

    /**
     * <p>
     *     This setting tells bitser how the size of the target collection specified by {@link #path()} should be
     *     serialized.
     * </p>
     *
     * <p>
     *     For instance, if the size of an array is never larger than 10, you can annotate it with
     *     {@code @NestedFieldSetting(path = "", sizeField = @IntegerField(expectUniform=true, maxValue=10))}, which
     *     would cause bitser to always use 4 bits to store the size of the array. However, if the size of the array
     *     does turn out to be larger than 10, an {@link com.github.knokko.bitser.exceptions.InvalidBitValueException}
     *     will be thrown.
     * </p>
     */
    IntegerField sizeField() default @IntegerField(expectUniform = false, minValue = 0, maxValue = Integer.MAX_VALUE);
}
