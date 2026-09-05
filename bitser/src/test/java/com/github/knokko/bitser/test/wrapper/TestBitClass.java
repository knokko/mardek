package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TestBitClass {

	private static abstract class Entity {

		@SuppressWarnings("unused")
		private static final Class<?>[] BITSER_HIERARCHY = {
				Boat.class, Player.class, Zombie.class, PigZombie.class, Creeper.class
		};
	}

	@BitStruct(backwardCompatible = false)
	private static class Boat extends Entity {

		@FloatField
		float speed;
	}

	private static class LivingEntity extends Entity {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int maxHealth;
	}

	private static abstract class Enemy extends LivingEntity {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int attackDamage;

		@BitField(optional = true, id = 1)
		@ReferenceField(stable = false, label = "entities")
		LivingEntity target;
	}

	@BitStruct(backwardCompatible = true)
	private static class Zombie extends Enemy {

		@BitField(id = 2)
		boolean wasVillager;

		@BitField(id = 0)
		@ReferenceField(stable = false, label = "positions")
		String targetPosition;
	}

	@BitStruct(backwardCompatible = false)
	private static class PigZombie extends Zombie {

		@BitField
		boolean isAngry;
	}

	@BitStruct(backwardCompatible = false)
	private static class Creeper extends Enemy {}

	@BitStruct(backwardCompatible = false)
	private static class Player extends LivingEntity {

		@BitField
		String name;

		@ReferenceFieldTarget(label = "positions")
		String lastPosition;
	}

	@BitStruct(backwardCompatible = false)
	private static class World {

		@ClassField(root = Entity.class)
		@ReferenceFieldTarget(label = "entities")
		final ArrayList<Entity> entities = new ArrayList<>();
	}

	@Test
	public void testSerializeEntityHierarchy() {
		World world = new World();

		{
			Boat boat = new Boat();
			boat.speed = 1.25f;
			world.entities.add(boat);

			Player player = new Player();
			player.maxHealth = 12;
			player.name = "knokko";
			player.lastPosition = "cool cave";
			world.entities.add(player);

			Zombie zombie = new Zombie();
			zombie.maxHealth = 13;
			zombie.attackDamage = 4;
			zombie.wasVillager = true;
			zombie.target = player;
			zombie.targetPosition = player.lastPosition;
			world.entities.add(zombie);

			PigZombie pigZombie = new PigZombie();
			pigZombie.maxHealth = 14;
			pigZombie.attackDamage = 10;
			pigZombie.targetPosition = player.lastPosition;
			pigZombie.isAngry = true;
			world.entities.add(pigZombie);

			Creeper creeper = new Creeper();
			creeper.maxHealth = 15;
			creeper.target = pigZombie;
			world.entities.add(creeper);
		}

		world = new Bitser().stupidDeepCopy(world);
		assertEquals(5, world.entities.size());

		Boat boat = (Boat) world.entities.get(0);
		assertEquals(1.25f, boat.speed);

		Player player = (Player) world.entities.get(1);
		assertEquals(12, player.maxHealth);
		assertEquals("knokko", player.name);
		assertEquals("cool cave", player.lastPosition);

		Zombie zombie = (Zombie) world.entities.get(2);
		assertEquals(13, zombie.maxHealth);
		assertEquals(4, zombie.attackDamage);
		assertTrue(zombie.wasVillager);
		assertSame(player, zombie.target);
		assertSame(player.lastPosition, zombie.targetPosition);

		PigZombie pigZombie = (PigZombie) world.entities.get(3);
		assertEquals(14, pigZombie.maxHealth);
		assertEquals(10, pigZombie.attackDamage);
		assertFalse(pigZombie.wasVillager);
		assertNull(pigZombie.target);
		assertTrue(pigZombie.isAngry);
		assertSame(player.lastPosition, pigZombie.targetPosition);

		Creeper creeper = (Creeper) world.entities.get(4);
		assertEquals(15, creeper.maxHealth);
		assertEquals(0, creeper.attackDamage);
		assertSame(pigZombie, creeper.target);
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid extends Entity {}

	@Test
	public void testInvalidEntity() {
		World world = new World();
		world.entities.add(new Invalid());
		assertThrows(InvalidBitValueException.class, () -> new Bitser().toBytes(world));
	}
}
