package com.github.knokko.bitser.test.wrapper;

//import com.github.knokko.bitser.BitStruct;
//import com.github.knokko.bitser.connection.BitListConnection;
//import com.github.knokko.bitser.connection.BitStructConnection;
//import com.github.knokko.bitser.field.BitField;
//import com.github.knokko.bitser.field.FloatField;
//import com.github.knokko.bitser.field.IntegerField;
//import com.github.knokko.bitser.field.NestedFieldSetting;
//import com.github.knokko.bitser.Bitser;
//import org.junit.jupiter.api.Test;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.LinkedList;
//
//import static org.junit.jupiter.api.Assertions.*;

public class TestBitListConnection {

//	@BitStruct(backwardCompatible = false)
//	private static class Root {
//
//		@SuppressWarnings("unused")
//		@BitField
//		String hello = "world";
//
//		@IntegerField(expectUniform = false)
//		@NestedFieldSetting(path = "c", optional = true)
//		ArrayList<Integer> integers = new ArrayList<>();
//
//		@BitField
//		ArrayList<String> strings = new ArrayList<>();
//	}
//
//	@Test
//	public void testIntegerList() throws IOException {
//		Bitser bitser = new Bitser();
//		TestBitStructConnection.ChangeTracker tracker = new TestBitStructConnection.ChangeTracker(1);
//
//		Root original = new Root();
//		original.integers.add(3);
//		original.integers.add(null);
//		original.strings.add("ok");
//
//		BitStructConnection<Root> root1 = bitser.createStructConnection(bitser.deepCopy(original), tracker);
//		BitStructConnection<Root> root2 = bitser.createStructConnection(bitser.deepCopy(original), tracker);
//
//		BitListConnection<Integer> integers1 = root1.getChildList("integers");
//		integers1.addDelayed(5);
//		tracker.applyChanges(root2);
//
//		assertEquals(3, root2.state.integers.size());
//		assertEquals(3, root2.state.integers.get(0));
//		assertNull(root2.state.integers.get(1));
//		assertEquals(5, root2.state.integers.get(2));
//
//		BitListConnection<Integer> integers2 = root2.getChildList("integers");
//		tracker.applyChanges(root1);
//		integers2.addDelayed(12);
//		tracker.applyChanges(root1);
//
//		assertEquals(4, root1.state.integers.size());
//		assertEquals(5, root1.state.integers.get(2));
//		assertEquals(12, root1.state.integers.get(3));
//	}
//
//	@Test
//	public void testStringList() throws IOException {
//		Bitser bitser = new Bitser();
//		TestBitStructConnection.ChangeTracker tracker = new TestBitStructConnection.ChangeTracker(1);
//
//		BitStructConnection<Root> root1 = bitser.createStructConnection(new Root(), tracker);
//		BitStructConnection<Root> root2 = bitser.createStructConnection(new Root(), tracker);
//
//		BitListConnection<String> strings1 = root1.getChildList("strings");
//		BitListConnection<String> strings2 = root2.getChildList("strings");
//
//		strings1.addDelayed("hello");
//		strings2.checkForChanges();
//		tracker.applyChanges(root1);
//		strings1.checkForChanges();
//		tracker.applyChanges(root2);
//
//		strings2.addDelayed(0, "hi");
//		tracker.applyChanges(root1);
//		tracker.applyChanges(root2);
//
//		strings1.replaceDelayed(1, "world");
//		tracker.applyChanges(root1);
//		tracker.applyChanges(root2);
//
//		strings2.removeDelayed(0);
//		tracker.applyChanges(root1);
//		tracker.applyChanges(root2);
//
//		assertEquals(1, strings1.list.size());
//		assertSame(strings1.list, root1.state.strings);
//		assertEquals("world", strings1.list.get(0));
//		assertEquals(strings1.list, strings2.list);
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class Mini {
//		@IntegerField(expectUniform = false)
//		int x;
//
//		Mini(int x) {
//			this.x = x;
//		}
//
//		@SuppressWarnings("unused")
//		Mini() {
//			this(0);
//		}
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class StructList {
//
//		@SuppressWarnings("unused")
//		@BitField
//		LinkedList<Mini> list = new LinkedList<>();
//	}
//
//	@Test
//	public void testStructList() throws IOException {
//		Bitser bitser = new Bitser();
//		TestBitStructConnection.ChangeTracker tracker = new TestBitStructConnection.ChangeTracker(1);
//
//		BitStructConnection<StructList> connection1 = bitser.createStructConnection(new StructList(), tracker);
//		BitStructConnection<StructList> connection2 = bitser.createStructConnection(new StructList(), tracker);
//
//		BitListConnection<Mini> list1 = connection1.getChildList("list");
//		BitListConnection<Mini> list2 = connection2.getChildList("list");
//
//		list1.addDelayed(new Mini(20));
//		tracker.applyChanges(connection1);
//		tracker.applyChanges(connection2);
//
//		BitStructConnection<Mini> mini2 = list2.getChildStruct(0);
//		assertEquals(20, mini2.state.x);
//		mini2.state.x += 5;
//		mini2.checkForChanges();
//		tracker.applyChanges(connection1);
//		tracker.applyChanges(connection2);
//
//		BitStructConnection<Mini> mini1 = list1.getChildStruct(0);
//		assertEquals(25, mini1.state.x);
//		mini1.state.x = 5;
//		mini1.checkForChanges();
//		tracker.applyChanges(connection2);
//		assertEquals(5, mini2.state.x);
//	}
//
//	@Test
//	public void testEditDeletedStruct() throws IOException {
//		Bitser bitser = new Bitser();
//		TestBitStructConnection.ChangeTracker tracker1 = new TestBitStructConnection.ChangeTracker(1);
//		TestBitStructConnection.CombinedChangeTracker tracker2 = new TestBitStructConnection.CombinedChangeTracker();
//
//		BitStructConnection<StructList> connection1 = bitser.createStructConnection(new StructList(), tracker1);
//		BitStructConnection<StructList> connection2 = bitser.createStructConnection(new StructList(), tracker2);
//
//		BitListConnection<Mini> list1 = connection1.getChildList("list");
//		BitListConnection<Mini> list2 = connection2.getChildList("list");
//
//		list1.addDelayed(new Mini(20));
//		tracker1.applyChanges(connection1);
//		tracker1.applyChanges(connection2);
//		list1.addDelayed(new Mini(50));
//		tracker1.applyChanges(connection1);
//		tracker1.applyChanges(connection2);
//
//		list1.removeDelayed(1);
//
//		BitStructConnection<Mini> mini21 = list2.getChildStruct(1);
//		assertEquals(50, mini21.state.x);
//		mini21.state.x = 100;
//		mini21.checkForChanges();
//
//		BitStructConnection<Mini> mini20 = list2.getChildStruct(0);
//		assertEquals(20, mini20.state.x);
//		mini20.state.x += 5;
//		mini20.checkForChanges();
//
//		tracker1.applyChanges(connection1);
//		tracker1.applyChanges(connection2);
//		tracker2.applyChanges(connection1, connection2);
//		assertEquals(1, list1.list.size());
//		assertEquals(1, list2.list.size());
//		assertEquals(25, list1.<Mini>getChildStruct(0).state.x);
//		assertEquals(25, mini20.state.x);
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class NestedList {
//
//		@SuppressWarnings("unused")
//		@FloatField
//		ArrayList<LinkedList<Double>> outerList = new ArrayList<>();
//	}
//
//	@Test
//	public void testNestedList() throws IOException {
//		Bitser bitser = new Bitser();
//		TestBitStructConnection.ChangeTracker tracker = new TestBitStructConnection.ChangeTracker(1);
//
//		BitStructConnection<NestedList> connection1 = bitser.createStructConnection(new NestedList(), tracker);
//		BitStructConnection<NestedList> connection2 = bitser.createStructConnection(new NestedList(), tracker);
//
//		BitListConnection<LinkedList<Double>> outer1 = connection1.getChildList("outerList");
//		BitListConnection<LinkedList<Double>> outer2 = connection2.getChildList("outerList");
//
//		LinkedList<Double> toAdd = new LinkedList<>();
//		toAdd.add(12.5);
//
//		outer1.addDelayed(toAdd);
//		tracker.applyChanges(connection1);
//		tracker.applyChanges(connection2);
//
//		BitListConnection<Double> inner2 = outer2.getChildList(0);
//		inner2.addDelayed(125.0);
//		tracker.applyChanges(connection1);
//		tracker.applyChanges(connection2);
//		inner2.addDelayed(-10.5);
//		tracker.applyChanges(connection1);
//		tracker.applyChanges(connection2);
//
//		BitListConnection<Double> inner1 = outer1.getChildList(0);
//		inner1.addDelayed(50.0);
//		tracker.applyChanges(connection2);
//		assertEquals(4, inner2.list.size());
//		assertEquals(12.5, inner2.list.get(0));
//		assertEquals(125.0, inner2.list.get(1));
//		assertEquals(-10.5, inner2.list.get(2));
//		assertEquals(50.0, inner2.list.get(3));
//	}
}
