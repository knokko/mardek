package com.github.knokko.bitser;

import com.github.knokko.bitser.io.BitInputStream;

import java.io.IOException;

class ReadHelper {

	static boolean readOptional(BitInputStream input, boolean optional) throws IOException {
		if (!optional) return false;
		input.prepareProperty("optional");
		boolean nonNull = input.read();
		input.finishProperty();
		return !nonNull;
	}
}
