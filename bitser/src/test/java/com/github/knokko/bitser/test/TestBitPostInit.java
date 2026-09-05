package com.github.knokko.bitser.test;

import com.github.knokko.bitser.BitPostInit;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.legacy.LegacyArrayValue;
import com.github.knokko.bitser.legacy.LegacyIntValue;
import com.github.knokko.bitser.legacy.LegacyStringValue;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.options.WithParameter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestBitPostInit {

	@BitStruct(backwardCompatible = true)
	private static class DerivedSum implements BitPostInit {

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		final int a;

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		final int b;

		int c;

		DerivedSum(int a, int b) {
			this.a = a;
			this.b = b;
			this.c = a + b;
		}

		@SuppressWarnings("unused")
		DerivedSum() {
			this(0, 0);
		}

		@Override
		public void postInit(Context context) {
			this.c = this.a + this.b;
		}
	}

	@Test
	public void testDerivedSum() {
		Bitser bitser = new Bitser();
		DerivedSum original = new DerivedSum(3, 4);
		assertEquals(3, original.a);
		assertEquals(7, original.c);

		DerivedSum loaded = bitser.stupidDeepCopy(original);
		assertEquals(3, loaded.a);
		assertEquals(4, loaded.b);
		assertEquals(7, loaded.c);
	}

	@BitStruct(backwardCompatible = true)
	private static class LegacyConversionBefore {

		@SuppressWarnings("unused")
		@BitField(id = 4)
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
		final int percentage = 75;

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final String hello = "world";
	}

	@BitStruct(backwardCompatible = true)
	private static class BeforeParent extends LegacyConversionBefore {

		@SuppressWarnings("unused")
		@BitField(id = 4)
		final String owner = "me";
	}

	@BitStruct(backwardCompatible = true)
	private static class LegacyConversionAfter implements BitPostInit {

		@BitField(id = 0)
		String hello;

		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.01)
		double fraction;

		@Override
		public void postInit(Context context) {
			assertEquals("world", hello);
			long percentage = ((LegacyIntValue) context.legacyValues.get(LegacyConversionAfter.class)[4]).value();
			this.fraction = percentage * 0.01;
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class AfterParent extends LegacyConversionAfter {

		@BitField(id = 1)
		String[] owners;

		@Override
		public void postInit(Context context) {
			super.postInit(context);
			this.owners = new String[] {((LegacyStringValue) context.legacyValues.get(AfterParent.class)[4]).value()};
		}
	}

	@Test
	public void testLegacyConversion() {
		Bitser bitser = new Bitser();
		assertEquals(0.75, bitser.fromBytes(
				LegacyConversionAfter.class,
				bitser.toBytes(new LegacyConversionBefore(), Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE).fraction
		);
	}

	@Test
	public void testLegacyConversionWithInheritance() {
		Bitser bitser = new Bitser();
		AfterParent parent = bitser.fromBytes(
				AfterParent.class, bitser.toBytes(new BeforeParent(), Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertArrayEquals(new String[] { "me" }, parent.owners);
		assertEquals(0.75, parent.fraction);
	}

	@BitStruct(backwardCompatible = true)
	private static class DefaultValue implements BitPostInit {

		@BitField(id = 0)
		String value = "";

		@Override
		public void postInit(Context context) {
			if (value.isEmpty()) value = (String) context.withParameters.get("default-value");
		}
	}

	@Test
	public void testWithParameters() {
		Bitser bitser = new Bitser();
		DefaultValue customValue = new DefaultValue();
		customValue.value = "hello";

		DefaultValue defaultValue = bitser.stupidDeepCopy(
				new DefaultValue(), new WithParameter("default-value", "it worked"), new WithParameter("unused", 1234)
		);
		customValue = bitser.stupidDeepCopy(customValue);
		assertEquals("it worked", defaultValue.value);
		assertEquals("hello", customValue.value);

		defaultValue = bitser.fromBytes(DefaultValue.class, bitser.toBytes(
				new DefaultValue(), Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE, new WithParameter("default-value", "also backward-compatible"));
		assertEquals("also backward-compatible", defaultValue.value);
	}

	@Test
	public void testDuplicateDeserializeWithParameter() {
		String errorMessage = assertThrows(
				IllegalArgumentException.class,
				() -> new Bitser().fromBytes(
					AfterParent.class, new byte[0], new WithParameter("ok", 1), new WithParameter("ok", 2))
		).getMessage();
		assertContains(errorMessage, "Duplicate with parameter");
		assertContains(errorMessage, "ok");
	}

	@Test
	public void testDuplicateSerializeWithParameter() {
		String errorMessage = assertThrows(
				IllegalArgumentException.class,
				() -> new Bitser().toBytes(
					new AfterParent(), new WithParameter("ok", 1), new WithParameter("ok", 2))
		).getMessage();
		assertContains(errorMessage, "Duplicate with parameter");
		assertContains(errorMessage, "ok");
	}

	@BitStruct(backwardCompatible = true)
	private static class SaveClassName implements BitPostInit {

		private Class<?> wrapped;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int x;

		@SuppressWarnings("unused")
		@BitField(id = 2)
		private String className() {
			return wrapped.getName();
		}

		@Override
		public void postInit(Context context) {
			assertEquals(1234, x);
			try {
				this.wrapped = Class.forName((String) context.values.get(SaveClassName.class)[2]);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	public void testReadFunctionValues() {
		SaveClassName original = new SaveClassName();
		original.wrapped = String.class;
		original.x = 1234;

		SaveClassName loaded = new Bitser().stupidDeepCopy(original);
		assertSame(String.class, loaded.wrapped);
		assertEquals(1234, loaded.x);
	}

	@BitStruct(backwardCompatible = true)
	private static class MultipleClassNames implements BitPostInit {

		private final List<Class<?>> classes = new ArrayList<>();

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		int x;

		@SuppressWarnings("unused")
		@BitField(id = 3)
		private ArrayList<String> classNames() {
			ArrayList<String> names = new ArrayList<>();
			for (Class<?> wrapped : classes) names.add(wrapped.getName());
			return names;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void postInit(Context context) {
			List<String> names;
			assertEquals(-12, x);
			if (context.legacyValues == null) {
				names = (List<String>) context.values.get(MultipleClassNames.class)[3];
			} else {
				Object[] legacyValues = context.legacyValues.get(MultipleClassNames.class);
				Object[] currentValues = context.values.get(MultipleClassNames.class);
				assertEquals(-12L, ((LegacyIntValue) context.legacyValues.get(MultipleClassNames.class)[1]).value());
				if (legacyValues[2] instanceof LegacyStringValue) {
					names = new ArrayList<>(1);
					names.add(((LegacyStringValue) legacyValues[2]).value());
					assertArrayEquals(new Object[] {null, -12, null}, currentValues);
				} else {
					LegacyArrayValue legacyNames = (LegacyArrayValue) legacyValues[3];
					Object[] rawNames = (Object[]) legacyNames.array;
					names = new ArrayList<>(rawNames.length);
					for (Object rawName : rawNames) names.add(((LegacyStringValue) rawName).value());

					List<String> expectedListAtIndex2 = new ArrayList<>(2);
					expectedListAtIndex2.add("java.io.File");
					expectedListAtIndex2.add("java.nio.file.Path");
					assertArrayEquals(new Object[] { null, -12, null, expectedListAtIndex2}, currentValues);
				}
			}

			for (String name : names) {
				try {
					classes.add(Class.forName(name));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	public void testSingleToMultipleClassNames() {
		Bitser bitser = new Bitser();

		SaveClassName original = new SaveClassName();
		original.wrapped = IOException.class;
		original.x = -12;

		MultipleClassNames multiple = bitser.fromBytes(
				MultipleClassNames.class,
				bitser.toBytes(original, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(-12, multiple.x);
		assertEquals(1, multiple.classes.size());
		assertSame(IOException.class, multiple.classes.get(0));
	}

	@Test
	public void testMultipleClasses() {
		Bitser bitser = new Bitser();
		MultipleClassNames original = new MultipleClassNames();
		original.x = -12;
		original.classes.add(Integer.class);
		original.classes.add(Class.class);

		MultipleClassNames loaded = bitser.stupidDeepCopy(original);
		assertEquals(-12, loaded.x);
		assertEquals(original.classes, loaded.classes);
	}

	@Test
	public void testMultipleClassesBackwardCompatible() {
		Bitser bitser = new Bitser();
		MultipleClassNames original = new MultipleClassNames();
		original.x = -12;
		original.classes.add(File.class);
		original.classes.add(Path.class);

		MultipleClassNames loaded = bitser.fromBytes(
				MultipleClassNames.class, bitser.toBytes(original, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(-12, loaded.x);
		assertEquals(original.classes, loaded.classes);
	}

	@BitStruct(backwardCompatible = true)
	private static class ClassSet implements BitPostInit {

		private final Set<Class<?>> classes = new HashSet<>();

		@SuppressWarnings("unused")
		@BitField(id = 3)
		private HashSet<String> classNames() {
			HashSet<String> names = new HashSet<>();
			for (Class<?> wrapped : classes) names.add(wrapped.getName());
			return names;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void postInit(BitPostInit.Context context) {
			Object[] legacyValues = context.legacyValues.get(ClassSet.class);
			Object[] currentValues = context.values.get(ClassSet.class);

			Set<String> names = (Set<String>) currentValues[3];
			assertArrayEquals(new Object[] { null, null, null, names }, currentValues);

			LegacyArrayValue legacyNamesInstance = (LegacyArrayValue) legacyValues[3];
			assertArrayEquals(new Object[] { null, new LegacyIntValue(0), null, legacyNamesInstance }, legacyValues);
			for (String name : names) {
				try {
					classes.add(Class.forName(name));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	public void testAutomaticConversion() {
		Bitser bitser = new Bitser();
		MultipleClassNames original = new MultipleClassNames();
		original.classes.add(File.class);
		original.classes.add(Path.class);

		ClassSet loaded = bitser.fromBytes(
				ClassSet.class,
				bitser.toBytes(original, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(2, loaded.classes.size());
		assertTrue(loaded.classes.contains(File.class));
		assertTrue(loaded.classes.contains(Path.class));
	}

	@BitStruct(backwardCompatible = true)
	private static class UsesFunctionContext implements BitPostInit {

		int c;

		@SuppressWarnings("unused")
		@BitField(id = 2)
		DerivedSum create(FunctionContext context) {
			return new DerivedSum(5, (int) context.withParameters.get("b"));
		}

		@Override
		public void postInit(Context context) {
			this.c = ((DerivedSum) context.values.get(UsesFunctionContext.class)[2]).c;
		}
	}

	@Test
	public void testFunctionContext() {
		Bitser bitser = new Bitser();
		UsesFunctionContext loaded = bitser.fromBytes(
				UsesFunctionContext.class,
				bitser.toBytes(new UsesFunctionContext(), new WithParameter("b", 7))
		);
		assertEquals(12, loaded.c);
	}

	@Test
	public void testBackwardCompatibleFunctionContext() {
		Bitser bitser = new Bitser();
		UsesFunctionContext loaded = bitser.fromBytes(
				UsesFunctionContext.class,
				bitser.toBytes(
						new UsesFunctionContext(), Bitser.BACKWARD_COMPATIBLE, new WithParameter("b", 7)
				),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(12, loaded.c);
	}

	@BitStruct(backwardCompatible = true)
	private static class WithoutPostInit {}

	@BitStruct(backwardCompatible = true)
	private static class WithPostInit implements BitPostInit {

		boolean calledPostInit = false;

		@Override
		public void postInit(Context context) {
			assertTrue(context.backwardCompatible);
			this.calledPostInit = true;
		}
	}

	@Test
	public void testLegacyClassDoesNotHavePostInit() {
		Bitser bitser = new Bitser();
		assertTrue(bitser.fromBytes(
				WithPostInit.class,
				bitser.toBytes(new WithoutPostInit(), Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		).calledPostInit);
	}

	@BitStruct(backwardCompatible = true)
	private static class OuterWithoutPostInit {}

	@BitStruct(backwardCompatible = true)
	private static class OuterWithPostInit {

		@BitField(id = 0)
		final WithPostInit inner = new WithPostInit();
	}

	@Test
	public void testPostInitWithoutLegacyClass() {
		Bitser bitser = new Bitser();
		assertTrue(bitser.fromBytes(
				OuterWithPostInit.class,
				bitser.toBytes(new OuterWithoutPostInit(), Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		).inner.calledPostInit);
	}

	@BitStruct(backwardCompatible = false)
	private static class InnerStruct implements BitPostInit {

		@IntegerField(expectUniform = false)
		int value;

		@SuppressWarnings("unused")
		InnerStruct() {
			this.value = -5;
		}

		InnerStruct(int value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			return value;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof InnerStruct && this.value == ((InnerStruct) other).value;
		}

		@Override
		public String toString() {
			return "Inner(" + value + ")";
		}

		@Override
		public void postInit(Context context) {
			if (value == 70) value += 1;
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class OuterMapStruct implements BitPostInit {

		@BitField
		final HashMap<InnerStruct, InnerStruct> map = new HashMap<>();

		@Override
		public void postInit(Context context) {
			InnerStruct valueOf5 = map.get(new InnerStruct(5));
			assertEquals(10, valueOf5.value);
			valueOf5.value += 1;

			assertEquals(30, map.remove(new InnerStruct(25)).value);
			assertEquals(20, Objects.requireNonNull(
					map.put(new InnerStruct(15), new InnerStruct(16))
			).value);
			map.put(new InnerStruct(55), new InnerStruct(55));
			map.keySet().forEach(key -> {
				if (key.value == 60) {
					assertEquals(71, map.get(key).value);
					key.value += 1;
				}
			});
		}
	}

	@Test
	public void testMapModificationDuringPostInit() {
		OuterMapStruct original = new OuterMapStruct();
		original.map.put(new InnerStruct(5), new InnerStruct(10));
		original.map.put(new InnerStruct(15), new InnerStruct(20));
		original.map.put(new InnerStruct(25), new InnerStruct(30));
		original.map.put(new InnerStruct(60), new InnerStruct(70));

		OuterMapStruct copied = new Bitser().stupidDeepCopy(original);
		assertEquals(4, copied.map.size());
		assertEquals(new InnerStruct(11), copied.map.get(new InnerStruct(5)));
		assertEquals(new InnerStruct(16), copied.map.get(new InnerStruct(15)));
		assertEquals(new InnerStruct(55), copied.map.get(new InnerStruct(55)));
		assertEquals(new InnerStruct(71), copied.map.get(new InnerStruct(61)));
	}

	@BitStruct(backwardCompatible = false)
	private static class OuterSetStruct implements BitPostInit {

		@BitField
		final HashSet<InnerStruct> set = new HashSet<>();

		@Override
		public void postInit(Context context) {
			assertEquals(4, set.size());
			assertTrue(set.remove(new InnerStruct(25)));
			assertTrue(set.contains(new InnerStruct(20)));
			set.add(new InnerStruct(33));

			for (var element : set) {
				if (element.value == 10) element.value += 1;
			}
		}
	}

	@Test
	public void testSetModificationDuringPostInit() {
		OuterSetStruct original = new OuterSetStruct();
		original.set.add(new InnerStruct(10));
		original.set.add(new InnerStruct(20));
		original.set.add(new InnerStruct(25));
		original.set.add(new InnerStruct(70));

		OuterSetStruct copied = new Bitser().stupidDeepCopy(original);
		assertEquals(4, copied.set.size());
		assertTrue(copied.set.contains(new InnerStruct(11)));
		assertTrue(copied.set.contains(new InnerStruct(20)));
		assertTrue(copied.set.contains(new InnerStruct(33)));
		assertTrue(copied.set.contains(new InnerStruct(71)));
	}
}
