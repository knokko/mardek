package com.github.knokko.bitser.test.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestInheritanceBackwardCompatibility {

	private static class OldAnimal {

		@SuppressWarnings("unused")
		private static final Class<?>[] BITSER_HIERARCHY = { OldBird.class, OldFish.class };

		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 0)
		int numLegs;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewAnimal {

		@SuppressWarnings("unused")
		private static final Class<?>[] BITSER_HIERARCHY = {
				NewBird.class, NewFish.class, NewAnimal.class, NewReptile.class, NewSnake.class
		};

		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 0)
		int numLegs;

		@BitField(id = 1)
		boolean isHungry;
	}

	@BitStruct(backwardCompatible = true)
	private static class OldBird extends OldAnimal {

		@BitField(id = 0)
		@FloatField(expectMultipleOf = 0.1)
		float flySpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewBird extends NewAnimal {

		@BitField(id = 0, optional = true)
		@FloatField(expectMultipleOf = 0.5)
		Float flySpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class OldFish extends OldAnimal {

		@BitField(id = 5)
		boolean canJump;

		@BitField(id = 3)
		@FloatField(expectMultipleOf = 0.5)
		double swimSpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewFish extends NewAnimal {

		@BitField(id = 5)
		boolean canJump;

		@BitField(id = 3)
		@FloatField(expectMultipleOf = 0.5)
		float swimSpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewSnake extends NewAnimal {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		boolean isVenomous;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewReptile extends NewAnimal {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		boolean isMortal;
	}

	@BitStruct(backwardCompatible = true)
	private static class OldZoo {

		@BitField(id = 0)
		@ClassField(root = OldAnimal.class)
		OldAnimal[] animals;
	}

	@BitStruct(backwardCompatible = true)
	private static class NewZoo {

		@BitField(id = 0)
		@ClassField(root = NewAnimal.class)
		NewAnimal[] animals;
	}

	@Test
	public void testSimpleInheritanceBackwardCompatibility() {
		Bitser bitser = new Bitser();
		OldFish fish1 = new OldFish();
		fish1.canJump = false;
		fish1.swimSpeed = 3.5;

		OldFish fish2 = new OldFish();
		fish2.canJump = true;
		fish2.swimSpeed = 0.75;

		OldBird bird = new OldBird();
		bird.flySpeed = 20;
		bird.numLegs = 2;

		OldZoo oldZoo = new OldZoo();
		oldZoo.animals = new OldAnimal[] { fish1, bird, fish2 };

		NewZoo newZoo = bitser.fromBytes(NewZoo.class, bitser.toBytes(
				oldZoo, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(3, newZoo.animals.length);

		NewFish newFish1 = (NewFish) newZoo.animals[0];
		assertFalse(newFish1.canJump);
		assertEquals(3.5, newFish1.swimSpeed);
		assertEquals(0, newFish1.numLegs);

		NewBird newBird = (NewBird) newZoo.animals[1];
		assertEquals(20, newBird.flySpeed);
		assertEquals(2, newBird.numLegs);
		assertFalse(newBird.isHungry);

		NewFish newFish2 = (NewFish) newZoo.animals[2];
		assertTrue(newFish2.canJump);
		assertEquals(0.75f, newFish2.swimSpeed);

		OldZoo back = bitser.fromBytes(OldZoo.class, bitser.toBytes(
				newZoo, Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE);
		assertEquals(3, back.animals.length);

		OldFish backFish1 = (OldFish) back.animals[0];
		assertFalse(backFish1.canJump);
		assertEquals(3.5, backFish1.swimSpeed);
		assertEquals(0, backFish1.numLegs);

		OldBird backBird = (OldBird) back.animals[1];
		assertEquals(20, backBird.flySpeed);
		assertEquals(2, backBird.numLegs);

		OldFish backFish2 = (OldFish) back.animals[2];
		assertTrue(backFish2.canJump);
		assertEquals(0, backFish2.numLegs);
		assertEquals(0.75, backFish2.swimSpeed);

		newZoo.animals[0] = new NewReptile();
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						OldZoo.class,
						bitser.toBytes(newZoo, Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "unknown subclass");
		assertContains(errorMessage, "OldAnimal[]");
		assertContains(errorMessage, "OldZoo.animals");
	}

	@BitStruct(backwardCompatible = true)
	private static class FewerOptionsAnimal {

		@SuppressWarnings("unused")
		private static final Class<?>[] BITSER_HIERARCHY = { FewerOptionsBird.class };

		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 0)
		int numLegs;

		@BitField(id = 1)
		@SuppressWarnings("unused")
		boolean isHungry;
	}

	@BitStruct(backwardCompatible = true)
	private static class FewerOptionsBird extends FewerOptionsAnimal {

		@SuppressWarnings("unused")
		@BitField(id = 0, optional = true)
		@FloatField(expectMultipleOf = 0.5)
		Float flySpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class FewerOptionsZoo {

		@BitField(id = 0)
		@ClassField(root = FewerOptionsAnimal.class)
		FewerOptionsAnimal[] animals;
	}

	@Test
	public void testWithFewerOptionsValid() {
		Bitser bitser = new Bitser();

		OldBird oldBird = new OldBird();
		oldBird.numLegs = 5;

		OldZoo oldZoo = new OldZoo();
		oldZoo.animals = new OldAnimal[] { oldBird };

		byte[] bytes = bitser.toBytes(oldZoo, Bitser.BACKWARD_COMPATIBLE);

		FewerOptionsZoo newZoo = bitser.fromBytes(
				FewerOptionsZoo.class, bytes, Bitser.BACKWARD_COMPATIBLE
		);
		FewerOptionsBird newBird = (FewerOptionsBird) newZoo.animals[0];
		assertEquals(5, newBird.numLegs);
	}

	@Test
	public void testWithFewerOptionsInvalid() {
		Bitser bitser = new Bitser();

		OldZoo oldZoo = new OldZoo();
		oldZoo.animals = new OldAnimal[] { new OldFish() };

		byte[] bytes = bitser.toBytes(oldZoo, Bitser.BACKWARD_COMPATIBLE);

		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				FewerOptionsZoo.class, bytes, Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "-> animals -> elements");
		assertContains(errorMessage, "unknown subclass");
		assertContains(errorMessage, "TestInheritanceBackwardCompatibility$FewerOptionsAnimal");
		assertContains(errorMessage, "TestInheritanceBackwardCompatibility$FewerOptionsZoo.animals");
	}

	@BitStruct(backwardCompatible = true)
	private static class IncompatibleAnimal {

		@SuppressWarnings("unused")
		private static final Class<?>[] BITSER_HIERARCHY = { IncompatibleBird.class };

		@BitField(id = 0)
		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false, minValue = 0)
		int numLegs;

		@BitField(id = 1)
		@SuppressWarnings("unused")
		boolean isHungry;
	}

	@BitStruct(backwardCompatible = true)
	private static abstract class IncompatibleFlyingAnimal extends IncompatibleAnimal {

		@SuppressWarnings("unused")
		@BitField(id = 0, optional = true)
		@FloatField(expectMultipleOf = 0.5)
		Float flySpeed;
	}

	@BitStruct(backwardCompatible = true)
	private static class IncompatibleBird extends IncompatibleFlyingAnimal {}

	@BitStruct(backwardCompatible = true)
	private static class IncompatibleZoo {

		@BitField(id = 0)
		@ClassField(root = IncompatibleAnimal.class)
		IncompatibleAnimal[] animals;
	}

	@Test
	public void testWithIncompatibleHierarchy1() {
		Bitser bitser = new Bitser();

		IncompatibleZoo zoo = new IncompatibleZoo();
		zoo.animals = new IncompatibleAnimal[] { new IncompatibleBird() };

		byte[] bytes = bitser.toBytes(zoo, Bitser.BACKWARD_COMPATIBLE);
		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				OldZoo.class, bytes, Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "hierarchy size changed from 4 to 3");
		assertContains(errorMessage, "-> animals");
	}

	@Test
	public void testWithIncompatibleHierarchy2() {
		Bitser bitser = new Bitser();

		OldZoo zoo = new OldZoo();
		zoo.animals = new OldAnimal[] { new OldBird() };

		byte[] bytes = bitser.toBytes(zoo, Bitser.BACKWARD_COMPATIBLE);
		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				IncompatibleZoo.class, bytes, Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "hierarchy size changed from 3 to 4");
		assertContains(errorMessage, "-> animals");
	}
}
