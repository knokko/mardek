package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import org.junit.jupiter.api.Test;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestInvalidFunctions {

	@BitStruct(backwardCompatible = false)
	private static class WithNegativeID {

		@BitField
		@IntegerField(expectUniform = false)
		int test() {
			return 1234;
		}
	}

	@Test
	public void testMethodWithNegativeID() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new WithNegativeID())
		).getMessage();
		assertContains(errorMessage, "method IDs must be non-negative");
		assertContains(errorMessage, "TestInvalidFunctions$WithNegativeID.test()");
	}

	@BitStruct(backwardCompatible = false)
	private static class WithDuplicateID {

		@BitField(id = 5)
		@IntegerField(expectUniform = false)
		int test1() {
			return 1;
		}

		@BitField(id = 5)
		@IntegerField(expectUniform = false)
		int test2() {
			return 2;
		}
	}

	@Test
	public void testTwoMethodsWithTheSameID() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new WithDuplicateID())
		).getMessage();
		assertContains(errorMessage, "multiple @BitField methods with id 5");
		assertContains(errorMessage, "TestInvalidFunctions$WithDuplicateID");
	}

	@BitStruct(backwardCompatible = false)
	private static class ReadsMissingMethod {

		@BitField(id = 2, readsMethodResult = true)
		int readsMethod;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int test1() {
			return 1;
		}

		@BitField(id = 3)
		@IntegerField(expectUniform = false)
		int test3() {
			return 3;
		}
	}

	@Test
	public void testFieldThatReadsFromMissingMethod() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new ReadsMissingMethod())
		).getMessage();
		assertContains(errorMessage, "needs a method annotated with @BitField(id = 2)");
		assertContains(errorMessage, "TestInvalidFunctions$ReadsMissingMethod.readsMethod");
	}

	@BitStruct(backwardCompatible = false)
	private static class ReadsSimpleMethod {

		@BitField(id = 2, readsMethodResult = true)
		int readsMethod;

		@BitField(id = 2)
		@IntegerField(expectUniform = false)
		int test1() {
			return 2;
		}
	}

	@Test
	public void testReadsSimpleMethod() {
		assertEquals(2, new Bitser().stupidDeepCopy(new ReadsSimpleMethod()).readsMethod);
	}

	@BitStruct(backwardCompatible = false)
	private static class MethodReadsOtherMethod {

		@BitField(id = 1, readsMethodResult = true)
		String readsOtherMethod() {
			return null;
		}
	}

	@Test
	public void testMethodThatReadsFromOtherMethod() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new MethodReadsOtherMethod())
		).getMessage();
		assertContains(errorMessage, "BitField methods cannot have readsMethodResult");
		assertContains(errorMessage, "TestInvalidFunctions$MethodReadsOtherMethod.readsOtherMethod");
	}

	@BitStruct(backwardCompatible = false)
	private static class ReadsMethodWithNegativeID {

		@BitField(readsMethodResult = true)
		String readNegative;
	}

	@Test
	public void testFieldWithNegativeIdReadsMethod() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new ReadsMethodWithNegativeID())
		).getMessage();
		assertContains(errorMessage, "BitField's with readsMethodResult = true must have a non-negative ID");
		assertContains(errorMessage, "TestInvalidFunctions$ReadsMethodWithNegativeID.readNegative");
	}

	@BitStruct(backwardCompatible = false)
	private static class DifferentTypes {

		@BitField(id = 0, readsMethodResult = true)
		@IntegerField(expectUniform = false)
		int fieldX;

		@BitField(id = 0)
		@IntegerField(expectUniform = true)
		long methodY() {
			return 5;
		}
	}

	@Test
	public void testDifferentTypes() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new DifferentTypes())
		).getMessage();
		assertContains(errorMessage, "does not match the return type");
		assertContains(errorMessage, "TestInvalidFunctions$DifferentTypes");
		assertContains(errorMessage, "fieldX");
		assertContains(errorMessage, "methodY");
	}

	@BitStruct(backwardCompatible = true)
	private static class FieldMethodConflict {

		@BitField(id = 1)
		String fieldX;

		@BitField(id = 1)
		String methodY() {
			return "abc";
		}
	}

	@Test
	public void testConflictBetweenFieldIdAndMethodId() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new FieldMethodConflict())
		).getMessage();
		assertContains(errorMessage, "id conflict between");
		assertContains(errorMessage, "TestInvalidFunctions$FieldMethodConflict");
		assertContains(errorMessage, "fieldX");
		assertContains(errorMessage, "methodY");
	}
}
