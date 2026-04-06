package mardek.content

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.action.ActionContent
import mardek.content.area.AreaContent
import mardek.content.audio.AudioContent
import mardek.content.battle.BattleContent
import mardek.content.characters.PlayableCharacter
import mardek.content.encyclopedia.EncyclopediaContent
import mardek.content.stats.StatsContent
import mardek.content.inventory.ItemsContent
import mardek.content.portrait.PortraitContent
import mardek.content.skill.SkillsContent
import mardek.content.story.StoryContent
import mardek.content.ui.Fonts
import mardek.content.ui.UiSprites
import mardek.content.world.WorldMap
import java.io.File

/**
 * The root class of the `content` module of this repository. The content defines all areas, items, characters,
 * monsters, etc...
 *
 * The whole purpose of the importer is to create and serialize a `Content` instance that mimics the original MARDEK
 * game.
 */
@BitStruct(backwardCompatible = true)
class Content {

	/**
	 * The audio-related part of the content, like the sound effects
	 */
	@BitField(id = 0)
	val audio = AudioContent()

	/**
	 * The stats-related part of the content, like the elements and status effects
	 */
	@BitField(id = 1)
	val stats = StatsContent()

	/**
	 * The skills-related part of the content, like the active skills and passive skills
	 */
	@BitField(id = 2)
	val skills = SkillsContent()

	/**
	 * The items-related part of the content, like the items and dreamstones
	 */
	@BitField(id = 3)
	val items = ItemsContent()

	/**
	 * The battle-related part of the content, like the monsters and battle backgrounds
	 */
	@BitField(id = 4)
	val battle = BattleContent()

	/**
	 * The area-related part of the content, like the areas and the chests
	 */
	@BitField(id = 5)
	val areas = AreaContent()

	/**
	 * The actions-related part of the content, like the dialogue and cutscenes
	 */
	@BitField(id = 6)
	val actions = ActionContent()

	/**
	 * The story-related part of the content, like quests and the timelines
	 */
	@BitField(id = 7)
	val story = StoryContent()

	/**
	 * All the playable characters, like Mardek and Deugan
	 */
	@BitField(id = 8)
	@ReferenceFieldTarget(label = "playable characters")
	val playableCharacters = ArrayList<PlayableCharacter>()

	/**
	 * All world maps in the game. Vanilla MARDEK has only 1 world map: Belfan
	 */
	@BitField(id = 9)
	@ReferenceFieldTarget(label = "world maps")
	val worldMaps = ArrayList<WorldMap>()

	/**
	 * All the character portraits (used mostly during dialogue), and the resources needed by these portraits.
	 */
	@BitField(id = 10)
	val portraits = PortraitContent()

	/**
	 * The fixed/hardcoded sprites that the engine needs for the UI rendering
	 */
	@BitField(id = 11)
	lateinit var ui: UiSprites

	/**
	 * All the fonts used by the (original) game. Note that this rewrite doesn't use all of them yet.
	 */
	@BitField(id = 12)
	lateinit var fonts: Fonts

	/**
	 * Some hardcoded 'saves' that are embedded in the content.
	 *
	 * For instance, the start of chapter 1 is stored at key "chapter1". I intend to add the start of the other
	 * chapters in a similar manner. This map could also be used for other kinds of checkpoints in e.g. the middle of
	 * chapters, or for debug environments.
	 */
	@BitField(id = 13)
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	val checkpoints = HashMap<String, ByteArray>()

	/**
	 * Everything that goes into the encyclopedia
	 */
	@BitField(id = 14)
	val encyclopedia = EncyclopediaContent()

	companion object {

		/**
		 * The directory containing all the main assets/resources (e.g. `content.bits` and `content.vk2d`)
		 */
		val RESOURCES_DIRECTORY = run {
			val workingDirectory = File("./")
			var resourcesDirectory = File("$workingDirectory/resources")
			if (resourcesDirectory.exists()) {
				resourcesDirectory
			} else {
				val parentDirectory = workingDirectory.absoluteFile.parentFile.parentFile!!
				File("$parentDirectory/resources")
			}
		}
	}
}
