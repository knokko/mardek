package mardek.state.saves

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import java.util.UUID
import kotlin.time.Duration

/**
 * This class contains some information about a save file that is used to display information about that save file in
 * the UI. This information is stored at the beginning of each save file, allowing it to be deserialized without
 * deserializing the entire save file.
 */
@BitStruct(backwardCompatible = true)
class SaveInfo(
	/**
	 * The name of the area where this save was made. When this save was made outside an area, it should be an empty
	 * string.
	 */
	@BitField(id = 0)
	val areaName: String,

	/**
	 * The UUID's of the party members (playable characters) when this save was made.
	 */
	@BitField(id = 1)
	@NestedFieldSetting(path = "c", optional = true)
	val party: Array<UUID?>,

	/**
	 * The value of `campaignState.totalTime`
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = true, minValue = 0)
	val playTime: Duration,

	/**
	 * The level of Mardek when this save was made
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 1)
	val partyLevel: Int,

	/**
	 * The chapter in which this save was made
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 1)
	val chapter: Int,
) {

	@Suppress("unused")
	private constructor() : this(
		"", arrayOf(null, null, null, null),
		Duration.ZERO, 0, 0,
	)
}
