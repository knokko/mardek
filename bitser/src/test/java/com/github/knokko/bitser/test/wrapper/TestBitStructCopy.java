package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestBitStructCopy {

	@BitStruct(backwardCompatible = false)
	private static class ChildStruct {

		@IntegerField(expectUniform = false)
		final int x;

		@SuppressWarnings("unused")
		ChildStruct() {
			this(0);
		}

		ChildStruct(int x) {
			this.x = x;
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class Root {

		@BitField
		boolean b;

		@BitField
		ChildStruct c = new ChildStruct(15);

		@FloatField
		double d = 1.25;

		@IntegerField(expectUniform = false)
		int i = 12;

		@IntegerField(expectUniform = false)
		long[] l = { 1 };

		@ReferenceFieldTarget(label = "children")
		ChildStruct target = new ChildStruct(20);

		@ReferenceField(stable = false, label = "children")
		ChildStruct reference = target;
	}

	@Test
	public void testShallowCopyAndDeepCopy() {
		Root root = new Root();
		root.b = true;
		root.c = new ChildStruct(16);
		root.d = -5.5;
		root.i = 321;
		root.l = new long[] { 12, -34 };
		root.target = new ChildStruct(-123);
		root.reference = root.target;

		Root copied = new Bitser().shallowCopy(root);
		assertNotSame(root, copied);
		assertTrue(copied.b);
		assertEquals(16, copied.c.x);
		assertSame(root.c, copied.c);
		assertEquals(-5.5, copied.d);
		assertEquals(321, copied.i);
		assertSame(root.l, copied.l);
		assertSame(root.target, copied.target);
		assertSame(copied.reference, copied.target);

		Root deepCopy = new Bitser().stupidDeepCopy(root);
		assertNotSame(root, deepCopy);
		assertNotSame(root.c, deepCopy.c);
		assertTrue(deepCopy.b);
		assertNotSame(root.l, deepCopy.l);
		assertArrayEquals(root.l, deepCopy.l);
		assertNotSame(root.target, deepCopy.target);
		assertSame(deepCopy.reference, deepCopy.target);
	}
}
