package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TestLinkedLists {

	@BitStruct(backwardCompatible = true)
	private static class BitLinkedList {

		private static BitLinkedList generateRandom() {
			Random rng = new Random();
			BitLinkedList node = null;

			for (int counter = 0; counter < 10_000; counter++) {
				BitLinkedList next = new BitLinkedList();
				next.value = "R" + rng.nextInt();
				next.next = node;
				node = next;
			}

			return node;
		}

		@BitField(id = 0)
		String value = "";

		@BitField(id = 1, optional = true)
		BitLinkedList next;
	}

	@Test
	public void testShort() {
		Bitser bitser = new Bitser();

		BitLinkedList root = new BitLinkedList();
		root.value = "hello";
		root.next = new BitLinkedList();
		root.next.value = "world";

		BitLinkedList recovered = bitser.stupidDeepCopy(root);
		assertEquals("hello", recovered.value);
		assertEquals("world", recovered.next.value);
		assertEquals(bitser.hashCode(root), bitser.hashCode(recovered));
		assertNull(recovered.next.next);
	}

	@Test
	public void testShortBackwardCompatible() {
		Bitser bitser = new Bitser();

		BitLinkedList root = new BitLinkedList();
		root.value = "hello";
		root.next = new BitLinkedList();
		root.next.value = "world";

		BitLinkedList recovered = bitser.stupidDeepCopy(root, Bitser.BACKWARD_COMPATIBLE);
		assertEquals("hello", recovered.value);
		assertEquals("world", recovered.next.value);
		assertNull(recovered.next.next);
	}

	@Test
	public void testLong() {
		Bitser bitser = new Bitser();
		BitLinkedList list = BitLinkedList.generateRandom();

		BitLinkedList recovered = bitser.stupidDeepCopy(list);
		int totalLength = 0;

		BitLinkedList node = recovered;
		while (node != null) {
			totalLength += node.value.length();
			node = node.next;
		}

		assertTrue(totalLength > 20_000);
		assertEquals(bitser.hashCode(list), bitser.hashCode(recovered));
	}

	@Test
    public void testLongBackwardCompatible() {
		Bitser bitser = new Bitser();
		BitLinkedList list = BitLinkedList.generateRandom();

		BitLinkedList recovered = bitser.stupidDeepCopy(list, Bitser.BACKWARD_COMPATIBLE);
		int totalLength = 0;

		BitLinkedList node = recovered;
		while (node != null) {
			totalLength += node.value.length();
			node = node.next;
		}
		assertTrue(totalLength > 20_000);
		assertEquals(bitser.hashCode(list), bitser.hashCode(recovered));
	}
}
