package com.github.knokko.bitser;

record BackReadParameters(
		BackDeserializer deserializer,
		BitFieldWrapper modernField,
		RecursionNode parentNode,
		String fieldName
) {}
