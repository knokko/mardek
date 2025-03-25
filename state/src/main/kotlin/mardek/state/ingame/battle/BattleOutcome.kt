package mardek.state.ingame.battle

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class BattleOutcome {
    Busy,
    Victory,
    GameOver,
    RanAway
}
