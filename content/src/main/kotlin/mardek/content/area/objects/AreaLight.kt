package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

/**
 * Describes a source of light in an area.
 */
@BitStruct(backwardCompatible = true)
class AreaLight(

	/**
	 * The light color, encoded using `ColorPacker`
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = true, commonValues=[
		-1771983880, -1769538982, -1771769861, -1772002818, 1342478066, 1358823424, 1694498700
	])
	val color: Int,

	/**
	 * The vertical offset between the upper edge of the tile where this light is placed, and the center of the light
	 * emission. An offset of 16 means that the light center is 1 tile 'lower'.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, commonValues = [5])
	val offsetY: Int
) {

	@Suppress("unused")
	private constructor() : this(0, 0)
}
