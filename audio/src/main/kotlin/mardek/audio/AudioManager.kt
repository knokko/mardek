package mardek.audio

import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.EXTThreadLocalContext.alcSetThreadContext
import org.lwjgl.stb.STBVorbis.*
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.IntBuffer

private fun getResource(path: String): ByteBuffer {
	val input = AudioManager::class.java.getResourceAsStream(path) ?: throw RuntimeException("Can't find resource $path")
	val byteArray = input.readAllBytes()
	input.close()

	val buffer = memCalloc(byteArray.size)
	buffer.put(0, byteArray)
	return buffer
}

private fun readVorbis(path: String?, byteArray: ByteArray?, alBuffer: Int, stack: MemoryStack) {
	val byteBuffer = if (byteArray != null) {
		val buffer = memCalloc(byteArray.size)
		buffer.put(0, byteArray)
		buffer
	} else getResource(path!!)
	val error = stack.callocInt(1)

	val decoder = stb_vorbis_open_memory(byteBuffer, error, null)
	if (error[0] != VORBIS__no_error) throw AudioException("stb_vorbis_open_memory($path) caused error ${error[0]}")
	if (decoder == 0L) throw Error("stb_vorbis_open_memory failed")

	val info = STBVorbisInfo.calloc(stack)
	stb_vorbis_get_info(decoder, info)

	val decodedAudio = memCallocShort(stb_vorbis_stream_length_in_samples(decoder) * info.channels())
	stb_vorbis_get_samples_short_interleaved(decoder, info.channels(), decodedAudio)
	stb_vorbis_close(decoder)

	alBufferData(alBuffer, if (info.channels() == 1) AL_FORMAT_MONO16 else AL_FORMAT_STEREO16, decodedAudio, info.sample_rate())
	assertAlSuccess("alBufferData($path)")
	memFree(decodedAudio)
}

internal class AudioManager {

	private val device = alcOpenDevice(null as ByteBuffer?)
	private val context: Long
	private val musicSource: Int
	private val soundSources = IntArray(3)
	private var nextSoundSource = 0

	private val buffers = mutableListOf<Int>()

	init {
		if (device == 0L) throw AudioException("alcOpenDevice returned 0")

		val caps = ALC.createCapabilities(device)
		assertAlcSuccess(device, "ALC.createCapabilities")

		this.context = assertAlcSuccess(device, alcCreateContext(device, null as IntBuffer?), "alcCreateContext")

		assertAlcSuccess(device, alcSetThreadContext(context), "alcSetThreadContext")
		AL.createCapabilities(caps)
		assertAlSuccess("AL.createCapabilities")

		this.musicSource = assertAlSuccess(alGenSources(), "alGenSources")

		stackPush().use { stack ->
			val pSoundSources = stack.callocInt(soundSources.size)
			alGenSources(pSoundSources)
			pSoundSources.get(soundSources)
		}
	}

	fun add(path: String?, bytes: ByteArray?) = stackPush().use { stack ->
		val pBuffer = stack.callocInt(1)
		alGenBuffers(pBuffer)
		assertAlSuccess("alGenBuffers")

		val buffer = pBuffer.get(0)
		readVorbis(path, bytes, buffer, stack)
		buffers.add(buffer)
		buffer
	}

	fun playMusic(audio: Int) = play(musicSource, audio)

	fun playSound(audio: Int) {
		play(soundSources[nextSoundSource], audio)
		nextSoundSource = (nextSoundSource + 1) % soundSources.size
	}

	private fun play(source: Int, audio: Int) {
		val currentAudio = alGetSourcei(source, AL_BUFFER)
		assertAlSuccess("alGetSourcei")
		val currentState = assertAlSuccess(alGetSourcei(source, AL_SOURCE_STATE), "alGetSourcei")
		if (currentState == AL_PLAYING) {
			if (audio == currentAudio) return
			alSourceStop(source)
			assertAlSuccess("alSourceStop")
		}

		if (audio != currentAudio) {
			alSourcei(source, AL_BUFFER, audio)
			assertAlSuccess("alSourcei")
		}

		alSourcePlay(source)
		assertAlSuccess("alSourcePlay")
	}

	fun destroy() {
		for (soundSource in soundSources) alSourceStop(soundSource)
		alSourceStop(musicSource)
		assertAlSuccess("alSourceStop")

		stackPush().use { stack ->
			val pSources = stack.callocInt(1 + soundSources.size)
			pSources.put(0, musicSource)
			pSources.put(1, soundSources)

			alDeleteSources(pSources)
			assertAlSuccess("alDeleteSources")

			val pBuffers = stack.callocInt(buffers.size)
			for (buffer in buffers) pBuffers.put(buffer)
			pBuffers.flip()

			alDeleteBuffers(pBuffers)
			assertAlSuccess("alDeleteBuffers")
		}

		assertAlcSuccess(device, alcSetThreadContext(0L), "alcSetThreadContext")
		alcDestroyContext(context)
		assertAlcSuccess(device, "alcDestroyContext")
		if (!alcCloseDevice(device)) throw AudioException("alcCloseDevice returned false")
	}
}
