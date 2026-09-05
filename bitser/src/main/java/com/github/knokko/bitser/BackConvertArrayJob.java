package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyBooleanValue;
import com.github.knokko.bitser.legacy.LegacyFloatValue;
import com.github.knokko.bitser.legacy.LegacyIntValue;

import java.lang.reflect.Array;

record BackConvertArrayJob(Object legacyArray, Object modernArray, BitFieldWrapper modernWrapper, RecursionNode node) {

	void convert(BackDeserializer deserializer) {
		int length = Array.getLength(legacyArray);
		for (int index = 0; index < length; index++) {

			Object legacyElement = Array.get(legacyArray, index);
			if (legacyElement instanceof Boolean) legacyElement = LegacyBooleanValue.get((Boolean) legacyElement);
			if (legacyElement instanceof Character) legacyElement = new LegacyIntValue((Character) legacyElement);
			if (legacyElement instanceof Float) legacyElement = new LegacyFloatValue((Float) legacyElement);
			if (legacyElement instanceof Double) legacyElement = new LegacyFloatValue((Double) legacyElement);
			if (legacyElement instanceof Number)
				legacyElement = new LegacyIntValue(((Number) legacyElement).longValue());

			if (legacyElement == null) {
				if (modernWrapper.field.optional) continue;
				throw new LegacyBitserException(
						"An element of " + modernWrapper.field + " is null, which is no longer allowed"
				);
			}

			Object modernElement = modernWrapper.convert(deserializer, legacyElement, node, "elements");
			Array.set(modernArray, index, modernElement);
			if (modernWrapper.field.referenceTargetLabel != null) {
				deserializer.references.registerModern(legacyElement, modernElement);
			}
		}
	}
}
