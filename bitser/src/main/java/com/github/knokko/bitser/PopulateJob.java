package com.github.knokko.bitser;

abstract class PopulateJob {

	final RecursionNode node;

	PopulateJob(RecursionNode node) {
		this.node = node;
	}

	abstract void populate();
}
