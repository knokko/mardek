package com.github.knokko.bitser.test;

import com.github.knokko.bitser.BitPostInit;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.SimpleLazyBits;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.options.WithParameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class TestDeepCopy {

	@BitStruct(backwardCompatible = false)
	private static class WithClass {

		@ReferenceFieldTarget(label = "IDs")
		final ElementStruct target = new ElementStruct(134);
	}

	@BitStruct(backwardCompatible = false)
	private static class References {

		@ReferenceFieldTarget(label = "IDs")
		final ElementStruct myTarget = new ElementStruct(123);

		@NestedFieldSetting(path = "c", optional = true)
		@ReferenceField(stable = false, label = "IDs")
		ElementStruct[] references;

		@ReferenceField(stable = false, label = "IDs")
		ElementStruct reference;

		@BitField(optional = true)
		@ReferenceField(stable = false, label = "IDs")
		ElementStruct nullReference;
	}

	@BitStruct(backwardCompatible = false)
	private static class ElementStruct {

		@IntegerField(expectUniform = false)
		int test;

		@SuppressWarnings("unused")
		ElementStruct() {}

		ElementStruct(int test) {
			this.test = test;
		}

		@Override
		public int hashCode() {
			return test;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof ElementStruct && this.test == ((ElementStruct) other).test;
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class Collections {

		@BitField
		@NestedFieldSetting(path = "c", optional = true)
		ElementStruct[] array;

		@BitField
		final ArrayList<ElementStruct> list = new ArrayList<>();

		@BitField
		final HashMap<ElementStruct, ElementStruct> map = new HashMap<>();
	}

	@BitStruct(backwardCompatible = false)
	private static class NeedsPostInit implements BitPostInit {

		int sum;

		@BitField(id = 4)
		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		int base() {
			return 4;
		}

		@Override
		public void postInit(Context context) {
			this.sum = (Integer) context.values.get(NeedsPostInit.class)[4] +
					(Integer) context.withParameters.get("right");
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class RootClass {

		@BitField
		final References references = new References();

		@BitField
		final Collections collections = new Collections();

		@NestedFieldSetting(path = "", writeAsBytes = true)
		int[] simpleInts;

		@BitField
		NeedsPostInit postInit = new NeedsPostInit();

		@BitField
		SimpleLazyBits<ElementStruct> lazy = new SimpleLazyBits<>(new ElementStruct(4));
	}

	@Test
	public void testDeepCopy() {
		Bitser bitser = new Bitser();

		WithClass withObject = new WithClass();

		RootClass original = new RootClass();
		original.simpleInts = new int[] { 3, -5, 9 };
		original.collections.array = new ElementStruct[] { new ElementStruct(4), null };
		original.collections.list.add(new ElementStruct(7));
		original.collections.map.put(new ElementStruct(-4), new ElementStruct(4));
		original.references.reference = withObject.target;
		original.references.references = new ElementStruct[] { original.references.myTarget, withObject.target, null };
		original.lazy = new SimpleLazyBits<>(new ElementStruct(987));

		RootClass copied = bitser.deepCopy(original, new WithParameter("right", 7));
		assertArrayEquals(new int[] { 3, -5, 9 }, copied.simpleInts);
		assertNotSame(original.simpleInts, copied.simpleInts);

		assertArrayEquals(new ElementStruct[] { new ElementStruct(4), null }, copied.collections.array);
		assertNotSame(original.collections.array, copied.collections.array);
		assertEquals(1, copied.collections.list.size());
		assertEquals(new ElementStruct(7), copied.collections.list.get(0));
		assertNotSame(original.collections.list.get(0), copied.collections.list.get(0));
		assertEquals(new ElementStruct(4), copied.collections.map.get(new ElementStruct(-4)));
		assertNotSame(original.collections.map.keySet().iterator().next(), copied.collections.map.keySet().iterator().next());

		assertSame(withObject.target, copied.references.reference);
		assertEquals(3, copied.references.references.length);
		assertSame(copied.references.myTarget, copied.references.references[0]);
		assertNotSame(original.references.myTarget, copied.references.myTarget);
		assertSame(withObject.target, copied.references.references[1]);
		assertNull(copied.references.references[2]);
		assertNull(copied.references.nullReference);

		assertEquals(11, copied.postInit.sum);
		assertNotSame(original.postInit, copied.postInit);

		assertEquals(new ElementStruct(987), copied.lazy.get());
		assertNotSame(original.lazy.get(), copied.lazy.get());
	}
}
