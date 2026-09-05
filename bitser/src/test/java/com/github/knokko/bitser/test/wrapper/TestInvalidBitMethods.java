package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.exceptions.RecursionException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestInvalidBitMethods {

	@BitStruct(backwardCompatible = false)
	private static class StaticBitMethod {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		private static String test() {
			return "hello";
		}
	}

	@Test
	public void testForbidStaticBitMethods() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser().toBytes(new StaticBitMethod())
		).getMessage();
		assertContains(errorMessage, "must not be static");
		assertContains(errorMessage, "StaticBitMethod.test");
	}

	@BitStruct(backwardCompatible = true)
	private static class MultipleParameters {

		@SuppressWarnings("unused")
		@BitField(id = 1)
		private String test(int x, int y) {
			return x + "," + y;
		}
	}

	@Test
	public void testForbidMultipleParameters() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser().toBytes(new MultipleParameters())
		).getMessage();
		assertContains(errorMessage, "at most 1 parameter");
		assertContains(errorMessage, "MultipleParameters.test");
	}

	@BitStruct(backwardCompatible = false)
	private static class WrongParameter {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		private String wrong(String hello) {
			return hello;
		}
	}

	@Test
	public void testForbidWrongParameterType() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser().toBytes(new WrongParameter())
		).getMessage();
		assertContains(errorMessage, "parameter type must be FunctionContext");
		assertContains(errorMessage, "WrongParameter.wrong");
	}

	@BitStruct(backwardCompatible = true)
	private static class WithoutID {

		@SuppressWarnings("unused")
		@BitField
		private String missing() {
			return "missing";
		}
	}

	@Test
	public void testIDsAreRequired() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser().toBytes(new WithoutID())
		).getMessage();
		assertContains(errorMessage, "method IDs must be non-negative");
		assertContains(errorMessage, "WithoutID.missing");
	}

	@BitStruct(backwardCompatible = true)
	private static class DuplicateID {

		@SuppressWarnings("unused")
		@BitField(id = 1)
		private String hello() {
			return "hello";
		}

		@SuppressWarnings("unused")
		@BitField(id = 1)
		private String world() {
			return "world";
		}
	}

	@Test
	public void testForbidDuplicateIDs() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser().toBytes(new DuplicateID())
		).getMessage();
		assertContains(errorMessage, "multiple @BitField methods with id 1");
		assertContains(errorMessage, "DuplicateID");
	}

	@BitStruct(backwardCompatible = false)
	private static class ThrowsError {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		private String throwsError() {
			throw new Error("nothing personal");
		}
	}

	@Test
	public void testPropagateErrors() {
		RecursionException exception = assertThrows(
				RecursionException.class, () -> new Bitser().toBytes(new ThrowsError())
		);
		assertEquals("nothing personal", exception.getCause().getMessage());
		assertContains(exception.debugInfoStack, "-> throwsError");
	}

	@BitStruct(backwardCompatible = false)
	private static class ThrowsUncheckedException {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		private String throwsError() {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testPropagateUncheckedExceptions() {
		RecursionException exception = assertThrows(
				RecursionException.class,
				() -> new Bitser().toBytes(new ThrowsUncheckedException())
		);
		assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
		assertContains(exception.debugInfoStack, "-> throwsError");
	}

	@BitStruct(backwardCompatible = true)
	private static class ThrowsCheckedException {

		@SuppressWarnings("unused")
		@BitField(id = 5)
		private UUID throwsIO(FunctionContext context) throws Exception {
			throw new Exception("nope: " + context);
		}
	}

	@Test
	public void testPropagateCheckedExceptions() {
		RecursionException exception = assertThrows(
				RecursionException.class,
				() -> new Bitser().toBytes(new ThrowsCheckedException())
		);
		Exception cause = (Exception) exception.getCause();
		assertContains(cause.getMessage(), "nope: ");
	}
}
