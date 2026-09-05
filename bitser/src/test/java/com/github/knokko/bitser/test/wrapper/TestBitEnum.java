package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.EnumField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = false)
public class TestBitEnum {

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.Name)
	private enum Season {
		SUMMER,
		AUTUMN,
		WINTER,
		SPRING
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.Ordinal)
	private enum Direction {
		LEFT,
		RIGHT,
		UP,
		DOWN
	}

	@SuppressWarnings("unused")
	private enum Element {
		WATER,
		FIRE,
		AIR,
		EARTH
	}

	@SuppressWarnings("unused")
	private enum ReverseElement {
		EARTH,
		AIR,
		FIRE,
		WATER
	}

	@BitField
	private Season seasons;

	@BitField
	private Direction direction;

	@BitField(optional = true)
	@EnumField(mode = BitEnum.Mode.Ordinal)
	private Element element;

	@Test
	public void test() {
		var bitser = new Bitser();
		this.seasons = Season.WINTER;
		this.direction = Direction.UP;

		TestBitEnum loaded = bitser.stupidDeepCopy(this);
		assertEquals(Season.WINTER, loaded.seasons);
		assertEquals(Direction.UP, loaded.direction);
		assertNull(loaded.element);
		assertEquals(bitser.hashCode(this), bitser.hashCode(loaded));

		this.element = Element.WATER;
		assertNotEquals(bitser.hashCode(this), bitser.hashCode(loaded));
		loaded = bitser.stupidDeepCopy(this);
		assertEquals(bitser.hashCode(this), bitser.hashCode(loaded));
		assertEquals(Season.WINTER, loaded.seasons);
		assertEquals(Direction.UP, loaded.direction);
		assertEquals(Element.WATER, loaded.element);
	}

	@BitEnum(mode = BitEnum.Mode.Name)
	private static class NonEnumClass {}

	@BitStruct(backwardCompatible = false)
	private static class NonEnumStruct {

		@BitField
		@SuppressWarnings("unused")
		NonEnumClass nope;
	}

	@Test
	public void testNonEnumClass() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().serialize(new NonEnumStruct(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "BitEnum can only be used on enums");
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.Name)
	private enum MissingSeason {
		SUMMER,
		SPRING
	}

	@BitStruct(backwardCompatible = false)
	private static class SeasonStruct {

		@BitField
		@SuppressWarnings("unused")
		final Season season = Season.AUTUMN;
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingSeasonStruct {

		@BitField
		@SuppressWarnings("unused")
		MissingSeason season;
	}

	@Test
	public void testDeletedEnumConstantName() {
		Bitser bitser = new Bitser();
		byte[] bytes = bitser.toBytes(new SeasonStruct());

		String errorMessage = assertThrows(InvalidBitFieldException.class, () -> bitser.fromBytes(
				MissingSeasonStruct.class, bytes
		)).getMessage();
		assertContains(errorMessage, "Missing enum constant AUTUMN");
		assertContains(errorMessage, "-> season");
	}

	@SuppressWarnings("unused")
	@BitEnum(mode = BitEnum.Mode.Ordinal)
	private enum MissingDirection {
		LEFT,
		RIGHT,
		UP
	}

	@BitStruct(backwardCompatible = false)
	private static class DirectionStruct {

		@BitField
		@SuppressWarnings("unused")
		final Direction direction = Direction.DOWN;
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingDirectionStruct {

		@BitField
		@SuppressWarnings("unused")
		MissingDirection direction;
	}

	@Test
	public void testDeletedEnumConstantOrdinal() {
		Bitser bitser = new Bitser();
		byte[] bytes = bitser.toBytes(new DirectionStruct());

		String errorMessage = assertThrows(InvalidBitFieldException.class, () -> bitser.fromBytes(
				MissingDirectionStruct.class, bytes
		)).getMessage();
		assertContains(errorMessage, "Missing enum ordinal 3");
		assertContains(errorMessage, "-> direction");
	}

	@BitStruct(backwardCompatible = false)
	private static class OverruleSeason {

		@BitField
		@EnumField(mode = BitEnum.Mode.Ordinal)
		Season season;
	}

	@Test
	public void testOverruleDefaultMode() {
		Bitser bitser = new Bitser();

		OverruleSeason overrule = new OverruleSeason();
		overrule.season = Season.SUMMER;

		DirectionStruct direction = bitser.fromBytes(
				DirectionStruct.class, bitser.toBytes(overrule)
		);
		assertEquals(Direction.LEFT, direction.direction);
	}

	@BitStruct(backwardCompatible = false)
	private static class Boss {

		@EnumField(mode = BitEnum.Mode.Ordinal)
		Element weakAgainst;

		@EnumField(mode = BitEnum.Mode.Name)
		Element strongAgainst;
	}

	@BitStruct(backwardCompatible = false)
	private static class MixedBoss {

		@EnumField(mode = BitEnum.Mode.Ordinal)
		ReverseElement weakAgainst;

		@EnumField(mode = BitEnum.Mode.Name)
		ReverseElement strongAgainst;
	}

	@Test
	public void testWithoutBitEnum() {
		Bitser bitser = new Bitser();

		Boss original = new Boss();
		original.weakAgainst = Element.FIRE;
		original.strongAgainst = Element.EARTH;

		MixedBoss mixed = bitser.fromBytes(MixedBoss.class, bitser.toBytes(original));
		assertEquals(ReverseElement.AIR, mixed.weakAgainst);
		assertEquals(ReverseElement.EARTH, mixed.strongAgainst);
		assertEquals(bitser.hashCode(original), bitser.hashCode(mixed));
	}

	@BitStruct(backwardCompatible = true)
	private static class ContainsEnumMap {

		@BitField(id = 0)
		final EnumMap<Season, String> nicerNames = new EnumMap<>(Season.class);

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int okay;
	}

	@Test
	public void testEnumMap() {
		Bitser bitser = new Bitser();

		ContainsEnumMap original = new ContainsEnumMap();
		original.nicerNames.put(Season.AUTUMN, "leafs");
		original.okay = 12;

		ContainsEnumMap simple = bitser.stupidDeepCopy(original);
		assertEquals(original.nicerNames, simple.nicerNames);
		assertEquals(12, simple.okay);

		ContainsEnumMap backward = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(original.nicerNames, backward.nicerNames);
		assertEquals(12, backward.okay);
		assertEquals(bitser.hashCode(original), bitser.hashCode(backward));
	}
}
