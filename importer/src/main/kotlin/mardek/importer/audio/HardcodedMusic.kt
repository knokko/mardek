package mardek.importer.audio

import mardek.content.audio.AudioContent
import mardek.content.audio.MusicCategory
import mardek.content.audio.MusicTrack
import mardek.importer.area.parseFlashString
import mardek.importer.util.classLoader
import mardek.importer.util.loadBc7Sprite
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptResource
import mardek.importer.util.projectFolder
import java.io.File
import java.lang.Float.parseFloat
import java.util.Scanner
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private fun category(displayName: String, fileName: String, content: AudioContent): MusicCategory {
	val icon = loadBc7Sprite("mardek/importer/audio/categories/$fileName.png")
	val category = MusicCategory(displayName, icon)
	content.musicCategories.add(category)
	return category
}

private fun parseLooping(): MutableMap<String, Duration> {
	val mapping = mutableMapOf<String, Duration>()
	val scanner = Scanner(classLoader.getResourceAsStream("mardek/importer/audio/loops.txt")!!)
	while (scanner.hasNextLine()) {
		val pairs = scanner.nextLine().split(":")
		if (pairs.size == 2 && pairs[1].endsWith(',')) {
			mapping[pairs[0]] = parseFloat(pairs[1].substring(0, pairs[1].length - 1)).toDouble().seconds
		}
	}
	scanner.close()
	return mapping
}

internal fun hardcodeMusic(content: AudioContent) {
	val mapping = mapOf(
		Pair("AREA", category("Area Music", "AreaMusic", content)),
		Pair("BATL", category("Battle Music", "BattleMusic", content)),
		Pair("CHAR", category("Character Themes", "CharacterMusic", content)),
		Pair("DUNG", category("Dungeon Music", "DungeonMusic", content)),
		Pair("JING", category("Jingles", "Jingles", content)),
		Pair("MOOD", category("Mood Music", "MoodMusic", content)),
		Pair("MISC", category("Miscellaneous Pieces", "Miscellaneous", content)),
	)

	val musicIDs = arrayOf(
		UUID.fromString("9b45ba88-f88c-4f8c-8694-6ffbc2550ac3"),
		UUID.fromString("1e30de3d-9431-4ae7-9f32-5ca10d003f40"),
		UUID.fromString("babe2f86-9db8-4ff7-af63-a124d5b9a561"),
		UUID.fromString("6cecc7e4-301e-4772-993e-7e14aed009b9"),
		UUID.fromString("6bbd579b-4ffe-41ef-a0ee-a544f9cd4df1"),
		UUID.fromString("5578fdf2-c81e-4667-83ad-57faefb90bd3"),
		UUID.fromString("4a2a1b70-adf4-4885-b5c1-8ed94af8b770"),
		UUID.fromString("43de2587-6cff-4189-9315-572babd50b61"),
		UUID.fromString("e23f0d34-88ff-4fa8-b087-9a3a7359e900"),
		UUID.fromString("5685c8ac-a279-4a46-9052-05e5ad76aee9"),
		UUID.fromString("c9743783-6331-4a27-9120-d4091d45b858"),
		UUID.fromString("f5132ca1-ab37-4b47-931f-3b78b209056c"),
		UUID.fromString("e39f84cf-0e20-434c-bbb0-5118e8125c3b"),
		UUID.fromString("635e6ab7-78bb-421c-a8e8-4849bedc4b03"),
		UUID.fromString("82e9ad2d-82f1-4811-9e59-337593db66d1"),
		UUID.fromString("7f08f919-2178-4e47-89f3-b1e308487ad6"),
		UUID.fromString("adc89d71-45b3-4996-acce-8618b19e3a17"),
		UUID.fromString("4531ab27-6ce9-4ca4-a367-64f909f62c02"),
		UUID.fromString("3cde5ae5-b32c-4854-8381-2214b1cf3ebb"),
		UUID.fromString("02c65116-838c-4121-8936-6c7829100f8f"),
		UUID.fromString("c6c06362-dc52-4bb4-9c73-fd9972f3f49d"),
		UUID.fromString("7bb7d5f5-1b6c-40a9-958d-69b37533fabf"),
		UUID.fromString("54aa27d0-4e5f-4620-8dd9-310e730af205"),
		UUID.fromString("2d66b073-2cc7-4f3b-aeee-c740c13c8188"),
		UUID.fromString("9157ca49-8e67-4360-812c-397d778128bd"),
		UUID.fromString("cce856d5-d5cc-467b-a049-9e69d871c161"),
		UUID.fromString("faa2db5a-7783-4333-9294-21e6075bbb03"),
		UUID.fromString("92376038-ad07-45b1-a711-746972cea8db"),
		UUID.fromString("14b3319c-1b39-422d-a21c-d0e7172f20aa"),
		UUID.fromString("ba625e71-348c-4732-8078-5ead512522c4"),
		UUID.fromString("1dbaa5e4-bf15-4b8a-aa4e-91c2aacad4f8"),
		UUID.fromString("afef6dd8-de30-414c-a2d9-1390073d6595"),
		UUID.fromString("c9e56864-896e-4423-b860-2a7848c70b57"),
		UUID.fromString("f50a2537-cd0c-4eca-afa8-e0129bd9152f"),
		UUID.fromString("31b70934-8aa1-4afd-b312-8fd9c252069a"),
		UUID.fromString("0fbf35dc-6c2b-4981-b657-7a837eb28d3b"),
		UUID.fromString("44bd2e03-e7be-4f36-b32f-4ea76008d41a"),
		UUID.fromString("7026da35-069a-4b35-98ca-6eb1d79360df"),
		UUID.fromString("14d47099-abfa-4657-809d-f0b0755423f9"),
		UUID.fromString("8b565a55-96ba-4c0c-9291-06b065ae29e1"),
		UUID.fromString("2e51fa81-fe99-4ea0-a0f8-f7deff933116"),
		UUID.fromString("943ec70a-10bc-4793-8aae-4a66f295db79"),
		UUID.fromString("6d7d7407-2f33-4fa0-a8f5-35e6f1ca61f3"),
		UUID.fromString("b789da94-fd4e-4f94-834a-cf91aa408a20"),
		UUID.fromString("09d6884c-db4a-46d1-8528-d1df1dd08de2"),
		UUID.fromString("1fbe4fde-3528-4725-88e9-f04607fd0102"),
		UUID.fromString("e46ebf01-ae37-4d99-afff-5e0c1092256a"),
		UUID.fromString("770001d1-b0b3-4626-a22b-50bfb1c71472"),
		UUID.fromString("3b9f824d-d186-440d-a519-03ed2942acd1"),
		UUID.fromString("03d760dc-9108-431f-bf27-fb650637f68c"),
		UUID.fromString("33079eb3-8bde-492f-a4f5-e49358e8bffa"),
		UUID.fromString("40f622c9-3e00-49fe-b564-eb0189637a2b"),
		UUID.fromString("de2442f6-c7dd-4e73-a9ab-b4ef7ddec660"),
		UUID.fromString("3b534789-ec86-4a71-9886-a7ddd81d0996"),
		UUID.fromString("060050b0-8c66-49c7-ab54-e22676607786"),
		UUID.fromString("ae8f954c-da62-4170-9cb4-3f21e1319dac"),
		UUID.fromString("94703bea-8b43-4e11-87b1-7645707fa0f7"),
		UUID.fromString("5cb2c158-aa21-48da-98e5-47845a18c7ae"),
		UUID.fromString("fea13429-c860-418e-8f30-39051e47a1c3"),
		UUID.fromString("2e0eaec0-7098-4f94-81b5-2d35bff0560d"),
		UUID.fromString("e41c011e-685c-4fe5-a887-7d350c2581ca"),
		UUID.fromString("3677a1c0-abf3-47ee-9455-f19d0e48e09d"),
		UUID.fromString("1bb75c1d-e378-42b4-adbd-b7b50635e19c"),
		UUID.fromString("db1d32e4-1eab-4b2c-87df-23115577c428"),
		UUID.fromString("cc1ed162-9f69-4bfd-beb0-28021bd488a1"),
		UUID.fromString("e45fbf19-7517-4cc1-be34-9ed12c778f09"),
		UUID.fromString("a79469c6-e6ec-49cb-a808-29e55a4e695b"),
		UUID.fromString("412fd59d-b5d4-4023-8749-df4492dc2e21"),
		UUID.fromString("bc46df29-361a-4ca3-9d2b-2b4027613f5d"),
		UUID.fromString("c0ba3463-126b-4910-8543-df274d5811f3"),
		UUID.fromString("a5e5a225-8e0a-40aa-8df1-39d1ef3616cb"),
		UUID.fromString("b34a0da2-833f-4ff3-833a-e3e1bea6b251"),
		UUID.fromString("1183b58e-c4d7-4975-99e9-261da653762a"),
		UUID.fromString("aa318aea-8c5f-4236-b7f5-6a5ef512dbfa"),
		UUID.fromString("b7ade30b-6cc9-481b-905b-8786e89d893d"),
		UUID.fromString("c050d560-e126-457c-b7d7-7d0cf67facb5"),
		UUID.fromString("d142f258-0f6d-4c5e-b5f2-c440a8ee7a52"),
	)

	val musicLoopMap = parseLooping()

	val flashCode = parseActionScriptResource("mardek/importer/audio/categories/Categories.txt")
	@Suppress("UNCHECKED_CAST")
	val musicCategoryAssignments = parseActionScriptNestedList(
		flashCode.variableAssignments["MPTracks"]!!
	) as ArrayList<ArrayList<String>>

	for ((index, musicEntry) in musicCategoryAssignments.withIndex()) {
		val displayName = parseFlashString(musicEntry[0], "music display name")!!
		val fileName = parseFlashString(musicEntry[1], "music file name")!!
		val rawCategory = parseFlashString(musicEntry[2], "music category")!!
		val category = mapping[rawCategory]!!

		val looping = musicLoopMap.remove(fileName) ?: Duration.ZERO
		content.musicTracks.add(MusicTrack(musicIDs[index], displayName, fileName, looping, category))

		val expectedFilePath = File("$projectFolder/resources/music/$fileName.ogg.zstd")
		if (!expectedFilePath.exists()) throw RuntimeException("Cannot find music $expectedFilePath")
	}

	if (musicLoopMap.isNotEmpty()) throw RuntimeException("Didn't consume $musicLoopMap")
	if (content.musicTracks.size != musicIDs.size) throw RuntimeException("Didn't consume all IDs")
}
