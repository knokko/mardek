package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;

public interface BitServerStructOperation<T> {

	void readRequestData(BitInputStream fromClient);

	void execute(BitServerStructController<T> controller, BitServerContext context);
}
