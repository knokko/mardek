package mardek.content.encyclopedia

import com.github.knokko.bitser.BitEnum
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.ExpressionEncyclopediaPersonValue
import mardek.content.expression.StateExpression
import mardek.content.inventory.ItemType
import mardek.content.portrait.PortraitInfo
import mardek.content.stats.CharacterClass
import mardek.content.stats.CreatureType
import mardek.content.stats.Element
import java.util.UUID

/**
 * Represents an entry of the "People" section of the "Encyclopedia" tab (of the in-game menu).
 */
@BitStruct(backwardCompatible = true)
class EncyclopediaPerson(

	/**
	 * The unique ID of the encyclopedia entry for this person, which is used for (de)serialization.
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * All the 'snapshots' of this encyclopedia person. When the player is viewing the encyclopedia, only one of these
	 * snapshots can be shown.
	 *
	 * Some characters have just one snapshot (e.g. Emela), whereas some other characters have multiple
	 * (e.g. Mardek has one per chapter).
	 *
	 * [chooseSnapshot] should either evaluate to one of these snapshots, or to `null`.
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "encyclopedia people snapshots")
	val snapshots: Array<Snapshot>,

	/**
	 * Chooses the right snapshot from [snapshots] that should be used at this point in the story, or `null` to indicate
	 * that this person should *not* be part of the encyclopedia at this point in the story.
	 */
	@BitField(id = 2)
	@ClassField(root = StateExpression::class)
	val chooseSnapshot: StateExpression<Snapshot?>,
) {

	internal constructor() : this(
		UUID(0, 0), emptyArray(),
		ConstantStateExpression(ExpressionEncyclopediaPersonValue(null)),
	)

	/**
	 * Represents a 'snapshot' of an [EncyclopediaPerson]. Each 'snapshot' determines the content of the encyclopedia
	 * entry when [EncyclopediaPerson.chooseSnapshot] evaluates to that snapshot.
	 *
	 * - Some people (e.g. Emela) have only one snapshot, which means their encyclopedia entry always look the same.
	 * - Other people (e.g. Mardek) have multiple snapshots, which means their encyclopedia entry can change over time
	 * (e.g. Mardek has a different one in each chapter)
	 */
	@BitStruct(backwardCompatible = true)
	class Snapshot(

		/**
		 * The first name of the character
		 */
		@BitField(id = 0)
		val firstName: String,

		/**
		 * The last name of the character, which is only shown when the entry is opened
		 */
		@BitField(id = 1)
		val lastName: String,

		/**
		 * The portrait of the character
		 */
		@BitField(id = 2)
		@ReferenceField(stable = false, label = "portrait info")
		val portrait: PortraitInfo,

		/**
		 * The portrait expression to be used, which is usually "norm"
		 */
		@BitField(id = 3)
		val portraitExpression: String,

		/**
		 * The creature type of this person, which is displayed as the 'Race' when the entry is opened.
		 */
		@BitField(id = 4)
		@ReferenceField(stable = false, label = "creature types")
		val creatureType: CreatureType,

		/**
		 * The gender of the person, which is displayed when the entry is opened, but has no real in-game
		 * consequences.
		 */
		@BitField(id = 5)
		val gender: Gender,

		/**
		 * The age of this person, during chapter 1, or `null` if [overrideAge] is displayed instead.
		 *
		 * In later chapters, the age of the character will be derived from `initialAge`, and the number of years
		 * between that chapter and chapter 1.
		 */
		@BitField(id = 6, optional = true)
		@IntegerField(expectUniform = false)
		val initialAge: Int?,

		/**
		 * If this field is non-null, it is displayed instead of [initialAge]. This can be used to mark people as
		 * deceased (Deugan), or to tell that their age is unknown (Rohoph).
		 */
		@BitField(id = 7, optional = true)
		val overrideAge: String?,

		/**
		 * The place of origin of this person
		 */
		@BitField(id = 8)
		val origin: String,

		/**
		 * The weapon type used by this person.
		 *
		 * For non-player characters, this is often a 'dummy' weapon type that is not used by any real items.
		 */
		@BitField(id = 9, optional = true)
		@ReferenceField(stable = false, label = "item types")
		val weaponType: ItemType,

		/**
		 * The element of this person
		 */
		@BitField(id = 10)
		@ReferenceField(stable = false, label = "elements")
		val element: Element,

		/**
		 * The 'class' of this person.
		 *
		 * This can be a 'dummy class' for people that don't fight, but still need a class name (e.g. 'Mother')
		 */
		@BitField(id = 11)
		@ReferenceField(stable = false, label = "character classes")
		val characterClass: CharacterClass,

		/**
		 * The alignment of this person, which is shown when the encyclopedia entry is opened, but serves no purpose
		 * outside the encyclopedia.
		 */
		@BitField(id = 12)
		val alignment: Alignment,

		/**
		 * The description of this person, which is shown when the encyclopedia entry is opened.
		 */
		@BitField(id = 13)
		val description: String,
	) {

		@Suppress("unused")
		private constructor() : this(
			"", "", PortraitInfo(), "", CreatureType(),
			Gender.None, 0, null, "", ItemType(), Element(),
			CharacterClass(), Alignment.TrueNeutral, "",
		)

		init {
			if ((initialAge == null) == (overrideAge == null)) {
				throw RuntimeException(
					"Exactly 1 of initialAge and overrideAge must be non-null, " +
							"but found $initialAge and $overrideAge"
				)
			}
		}
	}

	/**
	 * The alignments that characters can have (e.g. "Lawful Good" and "Chaotic Evil"). The alignment of each person is
	 * shown on its encyclopedia page, but is not used for anything else in the game.
	 */
	@BitEnum(mode = BitEnum.Mode.Ordinal)
	enum class Alignment(val displayName: String) {
		LawfulGood("Lawful Good"),
		NeutralGood("Neutral Good"),
		ChaoticGood("Chaotic Good"),
		LawfulNeutral("Lawful Neutral"),
		TrueNeutral("True Neutral"),
		ChaoticNeutral("Chaotic Neutral"),
		LawfulEvil("Lawful Evil"),
		NeutralEvil("Neutral Evil"),
		ChaoticEvil("Chaotic Evil"),
		Mixed("Mixed");
	}

	/**
	 * The genders that characters can have. Yeah yeah, MARDEK is 'woke' since it has [None]. That value is mostly
	 * used for genderless alien species.
	 *
	 * The gender of each person is shown on its encyclopedia page, but is not used for anything else in the game.
	 */
	@BitEnum(mode = BitEnum.Mode.Ordinal)
	enum class Gender {
		Male,
		Female,

		/**
		 * Used for characters without gender (e.g. Legion, reptoids, and annunaki)
		 */
		None
	}
}
