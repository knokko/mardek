package com.github.knokko.bitser.test.docs;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestReadmeExample {

	@BitStruct(backwardCompatible = false)
	private static class Monster1 {

		@IntegerField(expectUniform = false)
		int maxHealth;

		@IntegerField(expectUniform = false)
		int maxMana;

		@BitField
		boolean ignoresFireDamage;

		@IntegerField(expectUniform = false)
		int strength;

		@IntegerField(expectUniform = false)
		int agility;

		@IntegerField(expectUniform = false)
		int hitChance;
	}

	@BitStruct(backwardCompatible = false)
	private static class Monster2 {

		@IntegerField(expectUniform = false, minValue = 1)
		int maxHealth;

		@IntegerField(expectUniform = false, minValue = 1)
		int maxMana;

		@BitField
		boolean ignoresFireDamage;

		@IntegerField(expectUniform = false, minValue = 0)
		int strength;

		@IntegerField(expectUniform = false, minValue = 0)
		int agility;

		@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
		int hitChance;
	}

	@Test
	public void testExample1() throws IOException {
		var monster = new Monster1();
		monster.maxHealth = 100;
		monster.maxMana = 40;
		monster.ignoresFireDamage = false;
		monster.strength = 15;
		monster.agility = 5;
		monster.hitChance = 90;

		var counter = new BitCountStream();
		new Bitser().serialize(monster, counter);
		assertEquals(57, counter.getCounter());

	}

	@Test
	public void testExample2() throws IOException {
		var monster = new Monster2();
		monster.maxHealth = 100;
		monster.maxMana = 40;
		monster.ignoresFireDamage = false;
		monster.strength = 15;
		monster.agility = 5;
		monster.hitChance = 90;

		var counter = new BitCountStream();
		new Bitser().serialize(monster, counter);
		assertEquals(46, counter.getCounter());
	}
}
