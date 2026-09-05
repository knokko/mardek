package com.github.knokko.bitser.test.wrapper;

//import com.github.knokko.bitser.BitEnum;
//import com.github.knokko.bitser.BitStruct;
//import com.github.knokko.bitser.connection.BitStructConnection;
//import com.github.knokko.bitser.field.BitField;
//import com.github.knokko.bitser.field.FloatField;
//import com.github.knokko.bitser.field.IntegerField;
//import com.github.knokko.bitser.io.BitInputStream;
//import com.github.knokko.bitser.io.BitOutputStream;
//import com.github.knokko.bitser.Bitser;
//import org.junit.jupiter.api.Test;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//import java.util.function.Consumer;
//
//import static org.junit.jupiter.api.Assertions.*;

public class TestBitStructConnection {

//	@BitStruct(backwardCompatible = false)
//	private static class Primitives {
//
//		@BitField
//		boolean b1;
//
//		@BitField
//		boolean b2;
//
//		@IntegerField(expectUniform = false)
//		int i;
//
//		@IntegerField(expectUniform = false)
//		int j;
//
//		@FloatField
//		float f;
//
//		@FloatField
//		double d;
//	}
//
//	static class ChangeTracker implements Consumer<BitStructConnection.ChangeListener> {
//
//		private byte[] bytes;
//		int expectedNumberOfChanges;
//
//		ChangeTracker(int expectedNumberOfChanges) {
//			this.expectedNumberOfChanges = expectedNumberOfChanges;
//		}
//
//		@Override
//		public void accept(BitStructConnection.ChangeListener changeListener) {
//			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
//			BitOutputStream bitOutput = new BitOutputStream(byteOutput);
//			try {
//				assertEquals(expectedNumberOfChanges, changeListener.report(bitOutput));
//				bitOutput.finish();
//			} catch (IOException e) {
//				throw new Error(e);
//			}
//			this.bytes = byteOutput.toByteArray();
//		}
//
//		public void applyChanges(BitStructConnection<?> receiver) throws IOException {
//			receiver.handleChanges(new BitInputStream(new ByteArrayInputStream(bytes)));
//		}
//	}
//
//	static class CombinedChangeTracker implements Consumer<BitStructConnection.ChangeListener> {
//
//		private final List<byte[]> packets = new ArrayList<>();
//
//		@Override
//		public void accept(BitStructConnection.ChangeListener changeListener) {
//			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
//			BitOutputStream bitOutput = new BitOutputStream(byteOutput);
//			try {
//				changeListener.report(bitOutput);
//				bitOutput.finish();
//			} catch (IOException e) {
//				throw new Error(e);
//			}
//			packets.add(byteOutput.toByteArray());
//		}
//
//		public void applyChanges(BitStructConnection<?>... receivers) throws IOException {
//			for (byte[] packet : packets) {
//				for (BitStructConnection<?> receiver : receivers) {
//					receiver.handleChanges(new BitInputStream(new ByteArrayInputStream(packet)));
//				}
//			}
//			packets.clear();
//		}
//	}
//
//	@Test
//	public void testFindPrimitiveChanges() throws IOException {
//		Bitser bitser = new Bitser();
//		ChangeTracker tracker = new ChangeTracker(3);
//
//		Primitives original = new Primitives();
//		original.j = 12;
//		original.f = 1f;
//
//		BitStructConnection<Primitives> connection1 = bitser.createStructConnection(bitser.shallowCopy(original), tracker);
//		BitStructConnection<Primitives> connection2 = bitser.createStructConnection(bitser.shallowCopy(original), null);
//
//		connection1.state.b1 = true;
//		connection1.state.i = 1234;
//		connection1.state.j = 12; // Note that it was already 12
//		connection1.state.f = 3f;
//
//		connection1.checkForChanges();
//		connection2.state.d = 60f;
//		tracker.applyChanges(connection2);
//
//		assertTrue(connection2.state.b1);
//		assertFalse(connection2.state.b2);
//		assertEquals(1234, connection2.state.i);
//		assertEquals(12, connection2.state.j);
//		assertEquals(3f, connection2.state.f);
//		assertEquals(60f, connection2.state.d);
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class PrimitiveWrappers {
//
//		@BitField
//		Boolean b1;
//
//		@BitField(optional = true)
//		Boolean b2;
//
//		@IntegerField(expectUniform = false)
//		Integer i;
//
//		@BitField(optional = true)
//		@IntegerField(expectUniform = false)
//		Integer j;
//
//		@FloatField
//		Float f;
//
//		@BitField(optional = true)
//		@FloatField
//		Double d;
//	}
//
//	@Test
//	public void testFindPrimitiveWrapperChanges() throws IOException {
//		Bitser bitser = new Bitser();
//		ChangeTracker tracker = new ChangeTracker(0);
//
//		PrimitiveWrappers original = new PrimitiveWrappers();
//		original.b1 = true;
//		original.i = 1234;
//		original.f = -1234.5f;
//
//		BitStructConnection<PrimitiveWrappers> connection1 = bitser.createStructConnection(bitser.shallowCopy(original), tracker);
//		BitStructConnection<PrimitiveWrappers> connection2 = bitser.createStructConnection(bitser.shallowCopy(original), null);
//
//		connection1.state.b1 = true;
//		connection1.state.i = 1234;
//		assertNotSame(original.i, connection1.state.i);
//		connection1.state.f = -1234.5f;
//		assertNotSame(original.f, connection1.state.f);
//
//		connection1.checkForChanges();
//
//		connection1.state.b2 = false;
//		connection1.state.i = 1235;
//		connection1.state.d = 1234.5;
//		tracker.expectedNumberOfChanges = 3;
//		connection1.checkForChanges();
//
//		tracker.applyChanges(connection2);
//		assertTrue(connection2.state.b1);
//		assertFalse(connection2.state.b2);
//		assertEquals(1235, connection2.state.i);
//		assertNull(connection2.state.j);
//		assertEquals(-1234.5f, connection2.state.f);
//		assertEquals(1234.5, connection2.state.d);
//	}
//
//	@BitEnum(mode = BitEnum.Mode.Ordinal)
//	private enum ExampleEnum {
//		A,
//		B,
//		C
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class NoNesting {
//
//		@BitField(optional = true)
//		String s1;
//
//		@BitField
//		String s2;
//
//		@BitField(optional = true)
//		UUID id1;
//
//		@BitField
//		UUID id2;
//
//		@BitField(optional = true)
//		ExampleEnum example1;
//
//		@BitField
//		ExampleEnum example2;
//	}
//
//	@Test
//	public void testFindSimpleNonNestedChanges() throws IOException {
//		Bitser bitser = new Bitser();
//		ChangeTracker tracker = new ChangeTracker(5);
//		NoNesting original = new NoNesting();
//		original.s1 = "hello";
//		original.s2 = "hi";
//		original.id1 = new UUID(1, 2);
//		original.id2 = new UUID(3, 4);
//		original.example1 = ExampleEnum.A;
//		original.example2 = ExampleEnum.C;
//
//		BitStructConnection<NoNesting> connection1 = bitser.createStructConnection(bitser.shallowCopy(original), tracker);
//		BitStructConnection<NoNesting> connection2 = bitser.createStructConnection(bitser.shallowCopy(original), null);
//		connection1.state.s1 = null;
//		connection1.state.s2 = "world";
//		connection1.state.id1 = null;
//		connection1.state.id2 = original.id2;
//		connection1.state.example1 = null;
//		connection1.state.example2 = ExampleEnum.B;
//		connection1.checkForChanges();
//
//		tracker.applyChanges(connection2);
//
//		assertNull(connection2.state.s1);
//		assertEquals("world", connection2.state.s2);
//		assertNull(connection2.state.id1);
//		assertEquals(connection1.state.id2, connection2.state.id2);
//		assertNull(connection2.state.example1);
//		assertEquals(ExampleEnum.B, connection2.state.example2);
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class Nested3 {
//
//		@IntegerField(expectUniform = false)
//		int x;
//
//		@IntegerField(expectUniform = false)
//		int y;
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class Nested2 {
//
//		@IntegerField(expectUniform = false)
//		int a;
//
//		@BitField
//		Nested3 deeper = new Nested3();
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class Nested1 {
//
//		@BitField
//		Nested2 deeper = new Nested2();
//
//		@FloatField
//		float f;
//	}
//
//	@Test
//	public void testNestedStructs() throws IOException {
//		ChangeTracker tracker = new ChangeTracker(1);
//		Bitser bitser = new Bitser();
//
//		Nested1 state2 = new Nested1();
//		state2.f = 123f;
//		state2.deeper.deeper.x = 999;
//
//		BitStructConnection<Nested1> connection1 = bitser.createStructConnection(new Nested1(), tracker);
//		BitStructConnection<Nested1> connection2 = bitser.createStructConnection(state2, null);
//
//		connection1.state.deeper.a = 43;
//		connection1.state.deeper.deeper.y = 45;
//		BitStructConnection<Nested2> nested1 = connection1.getChildStruct("deeper");
//		nested1.checkForChanges();
//		assertSame(nested1, connection1.getChildStruct("deeper"));
//
//		tracker.applyChanges(connection2);
//		assertEquals(43, state2.deeper.a);
//		nested1.getChildStruct("deeper").checkForChanges();
//
//		tracker.applyChanges(connection2);
//		assertEquals(999, state2.deeper.deeper.x);
//		assertEquals(45, state2.deeper.deeper.y);
//
//		assertSame(nested1, connection1.getChildStruct("deeper"));
//	}
//
//	@Test
//	public void testNestedStructReassignments() throws IOException {
//		ChangeTracker tracker = new ChangeTracker(1);
//		Bitser bitser = new Bitser();
//
//		BitStructConnection<Nested1> connection1 = bitser.createStructConnection(new Nested1(), tracker);
//		BitStructConnection<Nested1> connection2 = bitser.createStructConnection(new Nested1(), tracker);
//
//		connection1.state.deeper.a = 43;
//		BitStructConnection<Nested2> oldNested1 = connection1.getChildStruct("deeper");
//		oldNested1.checkForChanges();
//		tracker.applyChanges(connection2);
//
//		assertEquals(43, connection2.state.deeper.a);
//		BitStructConnection<Nested2> oldNested2 = connection2.getChildStruct("deeper");
//		assertEquals(43, oldNested2.state.a);
//
//		// Replace child struct...
//		connection2.state.deeper = new Nested2();
//		connection2.checkForChanges();
//		tracker.applyChanges(connection1);
//		connection2.state.deeper.a = 100;
//		BitStructConnection<Nested2> newNested2 = connection2.getChildStruct("deeper");
//		newNested2.checkForChanges();
//
//		tracker.applyChanges(connection1);
//
//		BitStructConnection<Nested2> newNested1 = connection1.getChildStruct("deeper");
//		assertSame(connection1.state.deeper, newNested1.state);
//		assertEquals(100, newNested1.state.a);
//
//		// Changing oldNested1 should not have any effect
//		oldNested1.state.a = 200;
//		oldNested1.checkForChanges();
//		tracker.applyChanges(connection2);
//		assertEquals(100, connection2.state.deeper.a);
//
//		// Changing newNested1 should also update nested2
//		newNested1.state.a = 300;
//		newNested1.checkForChanges();
//		tracker.applyChanges(connection2);
//		assertEquals(300, connection2.state.deeper.a);
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class OptionalRoot {
//
//		@BitField(optional = true)
//		Nested2 nested;
//
//		@BitField(optional = true)
//		String b;
//	}
//
//	@Test
//	public void testHandleOptionalNestedStruct() throws IOException {
//		ChangeTracker tracker = new ChangeTracker(1);
//		Bitser bitser = new Bitser();
//
//		BitStructConnection<OptionalRoot> connection1 = bitser.createStructConnection(new OptionalRoot(), tracker);
//		BitStructConnection<OptionalRoot> connection2 = bitser.createStructConnection(new OptionalRoot(), tracker);
//
//		connection1.state.nested = new Nested2();
//		connection1.state.nested.a = 321;
//		connection1.checkForChanges();
//		BitStructConnection<Nested2> nested1 = connection1.getChildStruct("nested");
//		tracker.applyChanges(connection2);
//
//		assertEquals(321, connection2.state.nested.a);
//		connection2.state.nested = null;
//		connection2.checkForChanges();
//		tracker.applyChanges(connection1);
//		assertNull(connection1.state.nested);
//
//		nested1.state.a += 10;
//		nested1.checkForChanges();
//		assertNull(connection1.state.nested);
//		tracker.applyChanges(connection2);
//		assertNull(connection2.state.nested);
//	}
//
//	@Test
//	public void testHandleOptionalNestedHazard() throws IOException {
//		Bitser bitser = new Bitser();
//		CombinedChangeTracker tracker1 = new CombinedChangeTracker();
//		ChangeTracker tracker2 = new ChangeTracker(1);
//
//		BitStructConnection<OptionalRoot> connection1 = bitser.createStructConnection(new OptionalRoot(), tracker1);
//		BitStructConnection<OptionalRoot> connection2 = bitser.createStructConnection(new OptionalRoot(), tracker2);
//
//		connection1.state.nested = new Nested2();
//		connection1.state.nested.a = 321;
//		connection1.checkForChanges();
//		tracker1.applyChanges(connection2, connection1);
//		BitStructConnection<Nested2> nested1 = connection1.getChildStruct("nested");
//
//		assertEquals(321, connection2.state.nested.a);
//		connection2.state.nested = null;
//		connection2.checkForChanges();
//
//		nested1.state.a = 5;
//		nested1.checkForChanges();
//
//		tracker2.applyChanges(connection1);
//		tracker2.applyChanges(connection2);
//		tracker1.applyChanges(connection1, connection2);
//
//		assertNull(connection1.state.nested);
//		assertNull(connection2.state.nested);
//	}
}
