package mardek.assets.area

import mardek.assets.area.objects.AreaObjects

class ParsedArea(
	val width: Int,
	val height: Int,
	val tilesheetName: String,
	private val tileGrid: IntArray,
	val objects: AreaObjects,
	// TODO Loot
	val randomBattles: RandomAreaBattles?,
	val flags: AreaFlags,
	val properties: AreaProperties,
) {

	fun getTileId(x: Int, y: Int): Int? {
		if (x < 0 || y < 0 || x >= width || y >= height) return null
		return tileGrid[x + y * width]
	}
}

class AreaProperties(
	val rawName: String,
	val displayName: String,
	/**
	 * Format is {ra:100,rb:0,ga:80,gb:0,ba:70,bb:0,aa:100,ab:0}, it looks like some foreground gradient between
	 * colors (red=ra, green=ga, blue=ba, alpha=aa) to (red=rb, green=gb, blue=bb, alpha=ab).
	 *
	 * It's used in, among others, Goznor night, Goznor zombie outbreak, Sauls dungeon, and Goldfish
	 */
	val ambience: AreaAmbience?,
	val musicTrack: String?,
	/**
	 * All areas with the same "dungeon" will share their switch gate/platform state
	 */
	val dungeon: String?,
	/**
	 * Upon visiting this area, the place with this name will be discovered in the encyclopedia
	 */
	val encyclopediaName: String?,
	val dreamType: AreaDreamType,
	val chestType: AreaChestType,
	val snowType: AreaSnowType,
	// TODO Dreamshrine Entrance has some GlowTiles stuff
)

class BattleEnemySelection(val name: String, val enemyNames: List<String?>) {
	init {
		if (enemyNames.size != 4) throw IllegalArgumentException("There must be exactly 4 enemy names")
	}

	override fun toString() = "EnemySelection(name=$name, enemies=$enemyNames)"

	override fun equals(other: Any?) = other is BattleEnemySelection && name == other.name &&
			enemyNames == other.enemyNames

	override fun hashCode() = name.hashCode() + 31 * enemyNames.hashCode()
}

class RandomAreaBattles(
	val ownEnemies: List<BattleEnemySelection>?,
	val monstersTableName: String?,
	val ownLevelRange: Pair<Int, Int>?,
	val levelRangeName: String?,
	val minSteps: Int,
	val chance: Int, // Percentage after taking 1 step?
	val specialBackground: String?
) {
	init {
		if ((ownEnemies == null) == (monstersTableName == null)) {
			throw IllegalArgumentException("Exactly 1 of ownEnemies and monstersTableName must be null")
		}
		if ((ownLevelRange == null) == (levelRangeName == null)) {
			throw IllegalArgumentException("Exactly 1 of ownLevelRange and levelRangeName must be null")
		}
	}

	private fun monstersString() = monstersTableName ?: ownEnemies!!.toString()

	private fun levelsString() = levelRangeName ?: ownLevelRange!!.toString()

	override fun toString(): String {
		return "RandomBattles(monsters=${monstersString()}, levels=${levelsString()}, " +
				"minSteps=$minSteps, chance=$chance%, specialBackground=$specialBackground"
	}

	override fun equals(other: Any?) = other is RandomAreaBattles && monstersTableName == other.monstersTableName &&
			ownEnemies == other.ownEnemies && levelRangeName == other.levelRangeName &&
			ownLevelRange == other.ownLevelRange && minSteps == other.minSteps && chance == other.chance &&
			specialBackground == other.specialBackground

	override fun hashCode(): Int {
		var result = ownEnemies?.hashCode() ?: 0
		result = 31 * result + (monstersTableName?.hashCode() ?: 0)
		result = 31 * result + (ownLevelRange?.hashCode() ?: 0)
		result = 31 * result + (levelRangeName?.hashCode() ?: 0)
		result = 31 * result + minSteps
		result = 31 * result + chance
		result = 31 * result + (specialBackground?.hashCode() ?: 0)
		return result
	}
}

class AreaFlags(
	/**
	 * Whether the player is allowed to use the Warp spell
	 */
	val canWarp: Boolean, // TODO Special case is start of chapter 3
	/**
	 * Hm... I'm not quite sure what this does... this flag is true in all kinds of places, and 162 / 258 areas
	 */
	val hasClearMap: Boolean,
	/**
	 * Whether the camera should be fixed rather than following the player? It's true in the Aeropolis theatre and
	 * the Earth Temple puzzle.
	 */
	val noMovingCamera: Boolean,
	/**
	 * Whether most of the party should be hidden? This is true in the Aeropolis theatre and the Earth Temple puzzle.
	 */
	val hideParty: Boolean,
	/**
	 * This flag is true when you're in the Astral Tunnel. It means that you can't switch to party members,
	 * despite already having unlocked them.
	 */
	val noSwitch: Boolean,
	/**
	 * This flag is true when you're in the Astral Tunnel. It means that you can't discover anything on the area map.
	 */
	val noMap: Boolean,
	/**
	 * This flag is true in the miasmal citadel maps. It probably means that we should display the miasma effect.
	 */
	val miasma: Boolean,
	/**
	 * This flag is true in Dragon's Lair. It probably means that the player can't access the item storage via save
	 * crystals.
	 */
	val noStorage: Boolean,
) {

	override fun toString() = "AreaFlags(canWarp=$canWarp, clearMap=$hasClearMap, noMovingCamera=$noMovingCamera, " +
			"hideParty=$hideParty, noSwitch=$noSwitch, noMap=$noMap, miasma=$miasma, noStorage=$noStorage"

	override fun equals(other: Any?) = other is AreaFlags && canWarp == other.canWarp &&
			hasClearMap == other.hasClearMap && noMovingCamera == other.noMovingCamera &&
			hideParty == other.hideParty && noSwitch == other.noSwitch && noMap == other.noMap &&
			miasma == other.miasma && noStorage == other.noStorage

	override fun hashCode() = canWarp.hashCode() + 2 * hasClearMap.hashCode() + 4 * noMovingCamera.hashCode() +
			8 * hideParty.hashCode() + 16 * noSwitch.hashCode() + 32 * noMap.hashCode() + 64 * miasma.hashCode() +
			128 * noStorage.hashCode()
}

enum class AreaChestType(val code: Int) {
	Default(0),
	MoricSaucer(1),
	DarkTemple(2),
	DreamRealm(3),
	BoneDemonAndRedDragon(5)
}

enum class AreaSnowType(val code: Int) {
	None(0),
	Lifewood(1),
	Dreamwood(2)
}

enum class AreaDreamType(
	/**
	 * Basically the hex code for the background color of the area
	 */
	val code: String
) {
	None(""),
	/**
	 * Uses 4849778, or #4A0072
	 */
	DreamcaveAndDreamshrineAndGrottoAndEarthTemple("true"),
	// TODO Find color of serpent cave
	SerpentCave("1"),
	/**
	 * No dream border
	 */
	DreamwoodAndCanoniaShaman("2"),
	AstralTunnel("19000"),
	DreamwoodCave("3342387")
}
