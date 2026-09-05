package com.github.knokko.bitser.test.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BackwardCompatibilityRegressionTest {

	@BitStruct(backwardCompatible = true)
	private static class ContentRoot {

		@BitField(id = 0)
		private final ItemStuff itemStuff;

		@BitField(id = 1)
		private final BattleStuff battleStuff;

		ContentRoot(ItemStuff itemStuff, BattleStuff battleStuff) {
			this.itemStuff = itemStuff;
			this.battleStuff = battleStuff;
		}

		@SuppressWarnings("unused")
		ContentRoot() {
			this(null, null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class ItemStuff {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "plot items")
		final ArrayList<PlotItem> plotItems = new ArrayList<>();

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "dream stones")
		final ArrayList<DreamStone> dreamStones = new ArrayList<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class BattleStuff {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "monsters")
		final ArrayList<Monster> monsters = new ArrayList<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class PlotItem {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

		@BitField(id = 1)
		final String name;

		PlotItem(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		PlotItem() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class DreamStone {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		final int index;

		DreamStone(int index) {
			this.index = index;
		}

		@SuppressWarnings("unused")
		DreamStone() {
			this(-1);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class PotentialPlotItem {

		@BitField(id = 0)
		@ReferenceField(stable = false, label = "plot items")
		final PlotItem item;

		@BitField(id = 1)
		@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
		final int chance;

		PotentialPlotItem(PlotItem item, int chance) {
			this.item = item;
			this.chance = chance;
		}

		@SuppressWarnings("unused")
		PotentialPlotItem() {
			this(null, -1);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class Monster {

		@BitField(id = 0)
		final ArrayList<PotentialPlotItem> plotLoot = new ArrayList<>();

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "dream stones")
		final ArrayList<DreamStone> dreamLoot = new ArrayList<>();

		@SuppressWarnings("unused")
		@BitField(id = 2)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testMardekRegression1() {
		ItemStuff itemStuff = new ItemStuff();
		for (int counter = 0; counter < 300; counter++) {
			itemStuff.plotItems.add(new PlotItem("Item " + counter));
		}
		for (int counter = 0; counter < 25; counter++) {
			itemStuff.dreamStones.add(new DreamStone(counter));
		}

		BattleStuff battleStuff = new BattleStuff();
		Monster monster = new Monster();
		monster.plotLoot.add(new PotentialPlotItem(itemStuff.plotItems.get(71), 71));
		monster.dreamLoot.add(itemStuff.dreamStones.get(5));

		battleStuff.monsters.add(new Monster());
		battleStuff.monsters.add(monster);
		ContentRoot root = new ContentRoot(itemStuff, battleStuff);

		ContentRoot copied = new Bitser().stupidDeepCopy(root, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(300, copied.itemStuff.plotItems.size());
		assertEquals(25, copied.itemStuff.dreamStones.size());
		assertEquals(2, copied.battleStuff.monsters.size());

		monster = copied.battleStuff.monsters.get(1);
		assertEquals(1, monster.plotLoot.size());
		assertEquals(1, monster.dreamLoot.size());
		assertSame(copied.itemStuff.plotItems.get(71), monster.plotLoot.get(0).item);
		assertSame(copied.itemStuff.dreamStones.get(5), monster.dreamLoot.get(0));
	}

	@BitStruct(backwardCompatible = true)
	private static class StateRoot {

		@BitField(id = 0, optional = true)
		Monster monster;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int kills;
	}

	@Test
	public void testMardekRegression2() {
		ItemStuff itemStuff = new ItemStuff();
		BattleStuff battleStuff = new BattleStuff();

		PlotItem plotItem = new PlotItem("Cool");
		itemStuff.plotItems.add(plotItem);

		Monster monster = new Monster();
		monster.plotLoot.add(new PotentialPlotItem(plotItem, 5));
		battleStuff.monsters.add(monster);

		ContentRoot content = new ContentRoot(itemStuff, battleStuff);
		StateRoot state = new StateRoot();
		state.kills = 1234;

		StateRoot copy = new Bitser().stupidDeepCopy(state, content, Bitser.BACKWARD_COMPATIBLE);
		assertNull(copy.monster);
		assertEquals(1234, copy.kills);
	}

	@BitStruct(backwardCompatible = true)
	static class OptionalClass {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "combatants")
		final String state = "state";

		@BitField(id = 1)
		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "combatants")
		final String target = state;
	}

	@BitStruct(backwardCompatible = true)
	static class TestClass {

		@SuppressWarnings("unused")
		@BitField(id = 0, optional = true)
		private OptionalClass state;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int test;
	}

	@Test
	public void testMardekRegression3() {
		Bitser bitser = new Bitser();
		TestClass instance = new TestClass();
		instance.test = 1234;
		TestClass copy = bitser.stupidDeepCopy(instance, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(1234, copy.test);
	}
}
