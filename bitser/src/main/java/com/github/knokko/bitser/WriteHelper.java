package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;

class WriteHelper {

	static boolean writeOptional(
			BitOutputStream output, Object value, boolean optional, String errorMessage
	) throws IOException, InvalidBitValueException {
		if (optional) {
			output.prepareProperty("optional");
			output.write(value != null);
			output.finishProperty();
			return value == null;
		} else if (value == null) throw new InvalidBitValueException(errorMessage);
		return false;
	}
}
