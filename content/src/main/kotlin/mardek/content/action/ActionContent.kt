package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.SimpleLazyBits
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * This is the root of the *action*-related content, where *actions* can do many things, for instance:
 * - Letting (player) characters talk
 * - Forcing (player) characters to move
 * - Opening up the save crystal dialogue
 * - Starting a boss battle
 *
 * So *actions* are basically anything dialogue-related in the game.
 */
@BitStruct(backwardCompatible = true)
class ActionContent {

    /**
     * This list contains the *global* action sequences: these typically the action sequences that span multiple areas,
     * or none at all.
     *
     * Most action sequences however are strongly related to one particular area, and stored in the `actions` list of
     * the relevant `Area`.
     */
    @BitField(id = 0)
    @ReferenceFieldTarget(label = "action sequences")
    val global = ArrayList<ActionSequence>()

    /**
     * This list contains all the cutscenes. These cutscenes can be used by action nodes.
     */
    @BitField(id = 1)
    @ReferenceFieldTarget(label = "cutscenes")
    val cutscenes = ArrayList<SimpleLazyBits<Cutscene>>()
}
