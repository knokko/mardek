package com.github.knokko.bitser.connection;

import java.util.List;

public class BitFlatStructListConnectionView<T> extends BitConnectionView {

	public final BitFlatStructView elementsView;

	public BitFlatStructListConnectionView(BitFlatStructView elementsView) {
		this.elementsView = elementsView;
	}

	@Override
	public BitServerController initializeController(BitServerContext context, Object targetObject) {
		//noinspection unchecked
		return new BitServerStructListController<T>(context, (List<T>) targetObject, elementsView.protocol);
	}
}
