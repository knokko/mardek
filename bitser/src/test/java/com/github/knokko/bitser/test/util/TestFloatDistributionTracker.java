package com.github.knokko.bitser.test.util;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.distributions.FloatDistributionTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFloatDistributionTracker {

	@BitStruct(backwardCompatible = false)
	private static class ExampleStruct {

		@FloatField
		float[] numbers;

		@FloatField
		double lonely;
	}

	@Test
	public void testLotsOfFivesAndHalves() {
		Bitser bitser = new Bitser();
		ExampleStruct example = new ExampleStruct();
		example.numbers = new float[] { 3.1f, 5.5f, 5.5f, 5.5f, 1.2f, 2.8f, 3.1f, 2.7f, 5.5f };
		example.lonely = -12.5;

		FloatDistributionTracker distribution = new FloatDistributionTracker();
		bitser.toBytes(example, distribution);

		List<String> sortedFields = distribution.getSortedFields();
		assertEquals(2, sortedFields.size());
		assertContains(sortedFields.get(0), "ExampleStruct.numbers");
		assertContains(sortedFields.get(1), "ExampleStruct.lonely");

		List<FloatDistributionTracker.Entry> recommendations = distribution.optimize(
				sortedFields.get(0), 1, new double[] { 0.01, 0.1 }
		);

		assertEquals(64, recommendations.get(0).spentBits);
		assertEquals(3, recommendations.get(0).digitSize);
		assertArrayEquals(new double[] { 5.5 }, recommendations.get(0).commonValues);
		assertEquals(0.1, recommendations.get(0).expectMultipleOf);
		assertEquals(2, recommendations.get(1).digitSize);
	}
}
