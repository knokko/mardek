package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;

public interface BitServerStructListOperation<T> {

	void readRequestData(BitInputStream fromClient);

	void execute(BitServerStructListController<T> controller, BitServerContext context);
}
