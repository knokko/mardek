package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     You can use this annotation to mark a field as a <i>reference</i> to the field of another struct. This annotation
 *     is needed if the identity of fields needs to be preserved.
 * </p>
 *
 * <p>
 *     By default, all fields of a {@link com.github.knokko.bitser.BitStruct} are serialized whenever the struct is
 *     serialized. This is usually desirable, but not always, as demonstrated by the following example. Consider an
 *     example graph with a {@code Node} class:
 * </p>
 * <ul>
 *     <li>Node {@code root} has children {@code a} and {@code b}.</li>
 *     <li>Node {@code a} has children {@code c} and {@code d}.</li>
 *     <li>Node {@code b} has children {@code d} and {@code e}.</li>
 *     <li>Nodes {@code b}, {@code c}, and {@code e} are leaf nodes.</li>
 * </ul>
 *
 * <p>
 *     Most importantly, node {@code d} is a child node of both {@code a} and {@code b}, so
 *     {@code a.children[1] == b.children[0]} yields {@code true}. However, if {@code root} would be serialized to
 *     bytes, and a new {@code root2} would be deserialized from these bytes, it would go wrong: the expression
 *     {@code a2.children[1] == b2.children[0]} would yield {@code false}. The node {@code d} was serialized and
 *     deserialized twice.
 * </p>
 *
 * <p>
 *     Things would get even worse if the {@code Node} class would also have a {@code parent} field: the serialization
 *     would never finish because it encounters an endless cycle. It would eventually run out of memory or disk space.
 * </p>
 *
 * <p>
 *     Both problems mentioned above can be solved using reference fields. To do so, the {@code Graph} class should
 *     have 2 fields:
 * </p>
 * <ul>
 *     <li>{@code @ReferenceFieldTarget(label = "nodes") ArrayList<Node> allNodes;}</li>
 *     <li>{@code Node root;}</li>
 * </ul>
 * and the {@code Node} class should have the following 2 fields:
 * <ul>
 *     <li>{@code @ReferenceField(stable = false, label = "nodes") Node parent;}</li>
 *     <li>{@code @ReferenceField(stable = false, label = "nodes") Node[] children;}</li>
 * </ul>
 * <p>
 *     The {@code Graph.allNodes} fields <b>owns</b> all the nodes: it should contain every node in the graph exactly
 *     once. Bitser will serialize all of them. All the {@code parent}s and {@code children} are <i>references</i> to
 *     the nodes in {@code allNodes}. When the {@code parent}s and {@code children} fields are serialized, bitser will
 *     only save the <i>index</i> into {@code Graph.allNodes}. These indices will be used during deserialization to
 *     populate all the {@code parent}s and {@code children}.
 * </p>
 *
 * <p>
 *     This will preserve reference quality: if {@code root.children[0].children[1] == root.children[1].children[0]},
 *     then this will also hold for the deserialized {@code root2}:
 *     {@code root2.children[0].children[1] == root2.children[1].children[0]},
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     When a field of the original serialized struct is a reference field, the corresponding field of the new
 *     deserialized struct must also be a reference field. Their settings for {@link #stable()} and {@link #label()}
 *     do <b>not</b> need to be equal.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceField {

	/**
	 * <p>
	 *     Whether the reference is <b>stable</b>.
	 * </p>
	 *
	 * <p>
	 *     In 'normal' cases, using {@code stable = false} is the best option, because it is easier and takes the
	 *     least amount of bits.
	 * </p>
	 *
	 * <p>
	 *     However, unstable references can cause problems when their <i>target</i> is inside a <i>with object</i>: the
	 *     {@code withAndOptions} of {@link com.github.knokko.bitser.Bitser#serialize}. Unstable references can break
	 *     when the <i>with object</i> is mutated between {@code bitser.serialize} and {@code bitser.deserialize}. In
	 *     particular, when reference targets with the same {@link #label()} are added, removed, or swapped. In such
	 *     cases, the references need to be stable.
	 * </p>
	 *
	 * <p>
	 *     When a reference is <b>stable</b>, its <i>target</i> must be a {@code BitStruct}, and exactly 1 of its fields
	 *     must be annotated with {@link StableReferenceFieldId}. When bitser serializes a stable reference, it will
	 *     serialize the stable ID of the reference target. Such a reference will not break, unless the reference
	 *     target is missing during deserialization, or if its stable ID is changed.
	 * </p>
	 */
	boolean stable();

	/**
	 * The label of the reference field, which is needed to match references with their targets. Reference fields can
	 * only point to reference targets with the same label. (See {@link ReferenceFieldTarget#label()}.)
	 */
	String label();
}
