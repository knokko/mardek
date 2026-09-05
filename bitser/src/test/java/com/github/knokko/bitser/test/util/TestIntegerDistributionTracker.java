package com.github.knokko.bitser.test.util;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.distributions.IntegerDistributionTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestIntegerDistributionTracker {

	@BitStruct(backwardCompatible = false)
	private static class ExampleStruct {

		@IntegerField(expectUniform = false)
		int[] numbers;

		@IntegerField(expectUniform = false)
		long lonely;
	}

	@Test
	public void testLotsOfFives() {
		Bitser bitser = new Bitser();
		ExampleStruct example = new ExampleStruct();
		example.numbers = new int[] { 3, 5, 5, 5, 1, 2, 3, 2, 5 };
		example.lonely = -12;

		IntegerDistributionTracker distribution = new IntegerDistributionTracker();
		bitser.toBytes(example, distribution);

		List<String> fields = distribution.getSortedFields();
		assertEquals(4, fields.size());
		assertContains(fields.get(0), "ExampleStruct.numbers");

		List<IntegerDistributionTracker.Entry> recommendations = distribution.optimize(fields.get(0), 1);
		assertEquals(38, recommendations.get(0).spentBits);
		assertEquals(2, recommendations.get(0).digitSize);
		assertArrayEquals(new long[] { 5 }, recommendations.get(0).commonValues);
		assertEquals(0, recommendations.get(1).digitSize);
	}
}
