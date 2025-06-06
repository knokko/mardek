package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.blue
import com.github.knokko.boiler.utilities.ColorPacker.green
import com.github.knokko.boiler.utilities.ColorPacker.red
import com.github.knokko.boiler.utilities.ColorPacker.rgba

fun changeAlpha(color: Int, alpha: Int) = rgba(red(color), green(color), blue(color), alpha.toByte())
