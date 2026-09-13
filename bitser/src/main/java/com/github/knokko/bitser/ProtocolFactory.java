package com.github.knokko.bitser;

import com.github.knokko.bitser.connection.BitStructProtocol;

class ProtocolFactory {

	private static final Class<?>[] STRUCT_FIELD_CLASSES = {
			StructFieldWrapper.class,
			SimpleLazyFieldWrapper.class,
			ReferenceFieldWrapper.class
	};

	static BitStructProtocol createProtocol(Bitser bitser, BitStructWrapper<?> bitStruct) {
		var protocol = new BitStructProtocol();

		for (var bitClass : bitStruct.classHierarchy) {
			for (var field : bitClass.fields) {
				if (field.readsMethodResult()) continue;

				BitStructProtocol.FieldType fieldType = BitStructProtocol.FieldType.SIMPLE;
				for (var structFieldClass : STRUCT_FIELD_CLASSES) {
					if (field.bitField().getClass() == structFieldClass) {
						fieldType = BitStructProtocol.FieldType.STRUCT;
						break;
					}
				}

				protocol.addField(
						field.classField(), fieldType,
						(output, value) -> field.bitField().writeFlat(bitser, output, value),
						input -> field.bitField().readFlat(bitser, input)
				);
			}
		}

		protocol.finishRegistration();
		return protocol;
	}
}
