package com.github.knokko.bitser;

import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;

import java.io.Serializable;

@BitStruct(backwardCompatible = false)
class LegacyField implements Serializable {

	@IntegerField(expectUniform = false, minValue = 0)
	final int id;

	@ClassField(root = BitFieldWrapper.class)
	final BitFieldWrapper bitField;

	@BitField
	final boolean readsMethodResult;

	LegacyField(int id, BitFieldWrapper bitField, boolean readsMethodResult) {
		this.id = id;
		this.bitField = bitField;
		this.readsMethodResult = readsMethodResult;
	}

	@SuppressWarnings("unused")
	private LegacyField() {
		this(0, null, false);
	}
}
