package com.github.knokko.bitser.test.io;

import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.options.AnalyzePerformance;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnalysisBitInputStream {

	@Test
	public void tryOnBenchmarkClass() {
		Bitser bitser = new Bitser();

		SerializeBenchmark.RootStruct root = new SerializeBenchmark.RootStruct();
		for (int counter = 0; counter < 2500; counter++) {
			root.largeMap.put("test" + counter, "test" + counter + "test");
		}

		Random rng = new Random(12345);

		root.bigBoys = new SerializeBenchmark.BigStruct[5000];
		for (int index = 0; index < root.bigBoys.length; index++) {
			root.bigBoys[index] = new SerializeBenchmark.BigStruct();
			if (rng.nextInt(10) == 0) root.bigBoys[index].friend = root.bigBoys[rng.nextInt(index + 1)];
			root.bigBoys[index].children.add(new SerializeBenchmark.SmallStruct(
					rng.nextInt(100), -rng.nextInt(200), rng.nextInt(),
					rng.nextFloat(), 0.5f * rng.nextInt(100)
			));
		}

		assertTrue(bitser.deepEquals(root, bitser.stupidDeepCopy(root)));

		var analysis = new AnalyzePerformance();
		assertTrue(bitser.deepEquals(root, bitser.stupidDeepCopy(root, analysis)));

		assertEquals(1, analysis.rootNodes.size());
		var rootNode = analysis.rootNodes.get(0);
		assertEquals(1_649_596, rootNode.spentBits());
		assertEquals(2, rootNode.children().size());
		assertEquals("RootStruct", rootNode.context().generateTrace(null));
		assertEquals(1_237_332, rootNode.children().get(0).spentBits());
		assertEquals("RootStruct -> bigBoys", rootNode.children().get(0).context().generateTrace(null));
		assertEquals(412_264, rootNode.children().get(1).spentBits());
		assertEquals("RootStruct -> largeMap", rootNode.children().get(1).context().generateTrace(null));

		assertEquals(6, analysis.stages.size());
		assertEquals(1_587_388, analysis.stages.get(0).spentBits());
		assertEquals(0, analysis.stages.get(1).spentBits());
		assertEquals(0, analysis.stages.get(2).spentBits());
		assertEquals(62_208, analysis.stages.get(3).spentBits());
		assertEquals(0, analysis.stages.get(4).spentBits());
		assertEquals(0, analysis.stages.get(5).spentBits());

		analysis = new AnalyzePerformance();
		assertTrue(bitser.deepEquals(root, bitser.stupidDeepCopy(root, analysis, Bitser.BACKWARD_COMPATIBLE)));

		assertEquals(2, analysis.rootNodes.size());
		rootNode = analysis.rootNodes.get(0);
		assertEquals(1_649_596, rootNode.spentBits());
		assertEquals(2, rootNode.children().size());
		assertEquals("RootStruct", rootNode.context().generateTrace(null));
		assertEquals(1_237_332, rootNode.children().get(0).spentBits());
		assertEquals("RootStruct -> bigBoys", rootNode.children().get(0).context().generateTrace(null));
		assertEquals(412_264, rootNode.children().get(1).spentBits());
		assertEquals("RootStruct -> largeMap", rootNode.children().get(1).context().generateTrace(null));

		var backNode = analysis.rootNodes.get(1);
		assertEquals(424, backNode.spentBits());
		assertEquals(2, backNode.children().size());
		assertEquals("LegacyClasses", backNode.context().generateTrace(null));
		assertEquals(388, backNode.children().get(0).spentBits());
		assertEquals("LegacyClasses -> classes", backNode.children().get(0).context().generateTrace(null));
		assertEquals(36, backNode.children().get(1).spentBits());
		assertEquals("LegacyClasses -> structs", backNode.children().get(1).context().generateTrace(null));

		assertEquals(17, analysis.stages.size());
		assertEquals(406, analysis.stages.get(1).spentBits());
		assertEquals(18, analysis.stages.get(4).spentBits());
		assertEquals(1_587_388, analysis.stages.get(8).spentBits());
		assertEquals(62_208, analysis.stages.get(11).spentBits());
	}
}
