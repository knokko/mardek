package com.github.knokko.bitser.test.util;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.SimpleLazyBits;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.distributions.FloatDistributionTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLazyFloatDistributionTracker {

	@BitStruct(backwardCompatible = false)
	private static class InnerStruct {

		@FloatField
		@SuppressWarnings("unused")
		private float[] temperatures;
	}

	@BitStruct(backwardCompatible = false)
	private static class OuterStruct {

		final SimpleLazyBits<InnerStruct> lazy = new SimpleLazyBits<>(new InnerStruct());
	}

	@Test
	public void test() {
		OuterStruct outer = new OuterStruct();
		outer.lazy.get().temperatures = new float[] { 3f, 3f };

		Bitser bitser = new Bitser();
		FloatDistributionTracker tracker1 = new FloatDistributionTracker();
		OuterStruct copied = bitser.stupidDeepCopy(outer, tracker1);

		String field = tracker1.getSortedFields().get(0);
		assertContains(field, "temperatures");
		List<FloatDistributionTracker.Entry> entries = tracker1.optimize(
				field, 1, new double[] { 0.1 }
		);
		assertEquals(3.0, entries.get(0).commonValues[0], 0.001);

		FloatDistributionTracker tracker2 = new FloatDistributionTracker();
		bitser.toBytes(copied, Bitser.FORBID_LAZY_SAVING, tracker2);
		entries = tracker2.optimize(field, 1, new double[] { 0.1 });
		assertEquals(3.0, entries.get(0).commonValues[0], 0.001);
	}
}
