package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@BitStruct(backwardCompatible = false)
public class TestUUIDField {

	@BitField(optional = true)
	private UUID optional;

	@BitField
	private UUID required;

	@Test
	public void testNullAndZero() {
		this.required = new UUID(0, 0);
		TestUUIDField loaded = new Bitser().stupidDeepCopy(this);
		assertNull(loaded.optional);
		assertEquals(new UUID(0, 0), loaded.required);
	}

	@Test
	public void testGeneral() {
		this.optional = UUID.randomUUID();
		this.required = new UUID(12, 345);
		TestUUIDField loaded = new Bitser().stupidDeepCopy(this);
		assertEquals(this.optional, loaded.optional);
		assertEquals(new UUID(12, 345), loaded.required);
	}
}
