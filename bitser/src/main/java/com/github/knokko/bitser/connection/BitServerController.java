package com.github.knokko.bitser.connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;

public abstract class BitServerController {

	final BitServerContext context;

	BitServerController(BitServerContext context) {
		this.context = context;
	}

	abstract Object getTargetObject();

	public abstract void addClient(
			OutputStream toClient, InputStream fromClient, Runnable closeStream,
			int viewIndex
	);

	abstract boolean addViewIfNotPresent(BitConnectionView view);

	abstract void getChildViews(Deque<BitServerControllerMapping.ViewPair> destination);
}
