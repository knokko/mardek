package mardek.audio

import org.lwjgl.openal.AL10.AL_NO_ERROR
import org.lwjgl.openal.AL10.alGetError
import org.lwjgl.openal.ALC10.ALC_NO_ERROR
import org.lwjgl.openal.ALC10.alcGetError

internal fun assertAlcSuccess(device: Long, result: Long, functionName: String): Long {
	assertAlcSuccess(device, functionName)
	if (result == 0L) throw AudioException("$functionName returned 0")
	return result
}

internal fun assertAlcSuccess(device: Long, result: Boolean, functionName: String) {
	assertAlcSuccess(device, functionName)
	if (!result) throw AudioException("$functionName returned false")
}

internal fun assertAlcSuccess(device: Long, functionName: String) {
	val error = alcGetError(device)
	if (error != ALC_NO_ERROR) throw AudioException("$functionName caused error $error")
}

internal fun assertAlSuccess(result: Int, functionName: String): Int {
	assertAlSuccess(functionName)
	if (result == 0) throw AudioException("$functionName returned 0")
	return result
}

internal fun assertAlSuccess(functionName: String) {
	val error = alGetError()
	if (error != AL_NO_ERROR) throw AudioException("$functionName caused error $error")
}
