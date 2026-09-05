package com.github.knokko.bitser;

import java.lang.reflect.Field;

record PopulateFieldJob(
		Object[] functionValues, int functionID,
		Object structObject, Field classField,
		RecursionNode node
) {
	void populate() throws Exception {
		classField.set(structObject, functionValues[functionID]);
	}
}
