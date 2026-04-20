package mardek.importer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.action.ActionAddEncyclopediaPerson
import mardek.content.action.ActionBattle
import mardek.content.action.ActionFlashScreen
import mardek.content.action.ActionPlaySound
import mardek.content.action.ActionRotate
import mardek.content.action.ActionSequence
import mardek.content.action.ActionChangeAmbience
import mardek.content.action.ActionMoveAreaEffect
import mardek.content.action.ActionRemoveAreaEffect
import mardek.content.action.ActionSetMusic
import mardek.content.action.ActionShake
import mardek.content.action.ActionSpawnAreaEffect
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTargetCustom
import mardek.content.action.ActionTargetData
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetWholeParty
import mardek.content.action.ActionTimelineTransition
import mardek.content.action.ActionVanish
import mardek.content.action.ActionWait
import mardek.content.action.ActionWalk
import mardek.content.action.FixedActionNode
import mardek.content.action.WalkSpeed
import mardek.content.animation.ColorTransform
import mardek.content.area.Direction
import mardek.content.battle.Battle
import mardek.content.battle.Enemy
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal fun hardcodeCrashSiteActions(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	// The monster list can be empty during unit tests
	if (content.battle.monsters.size <= 2) {
		hardcoded["crashsite"] = mutableListOf(
			ActionSequence(name = "MugbertActions", root = FixedActionNode())
		)
		hardcoded["saucer"] = mutableListOf(
			ActionSequence(name = "RohophToMardek", root = FixedActionNode()),
			ActionSequence(name = "RohophBody", root = FixedActionNode()),
		)
		return
	}

	val targetMugbert = ActionTargetAreaCharacter(
		UUID.fromString("eaa89077-cb64-4390-8a55-0d22938b010e")
	)

	val mugbertRoot = fixedActionChain(
		actions = arrayOf(
			ActionWalk(ActionTargetWholeParty(), 6, 10, WalkSpeed.Normal),
			ActionAddEncyclopediaPerson(content.encyclopedia.people.find {
				it.snapshots[0].firstName == "Mugbert"
			}!!),
			ActionTalk(
				speaker = targetMugbert,
				expression = "mugb",
				text = "Oy! 'Oos there?",
			),
			ActionTalk(
				speaker = targetMugbert,
				expression = "mugb",
				text = "Ey, it's... uuuuuuhhhhh... you! You lickle childerns frum t' village! I beated you up before!",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "angr",
				text = "Well, shut up!",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "angr",
				text = "Mugbert? You big bully! What are you doing here?",
			),
			ActionTalk(
				speaker = targetMugbert,
				expression = "mugb",
				text = "That i'n't none o' yer business!",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "angr",
				text = "Well, I bet you're here for the Fallen Star!",
			),
			ActionTalk(
				speaker = targetMugbert,
				expression = "mugb",
				text = "*Duh*. An' it's mine! All mine! I got 'ere first, so that lee-gully meks it mine!",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "angr",
				text = "No! Your hands are the wrong hands! Uh... something like this fallen star can't be " +
						"in hands like yours! Wrong hands! You'd probably use it for EVIL or something!",
			),
			ActionTalk(
				speaker = targetMugbert,
				expression = "mugb",
				text = "Yeh, so? Wot're YOU gunna do about it, eh? Eh? Beat me up, eh? Hurhurhur. " +
						"I'd like to see you try!"
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "angr",
				text = "Alright then! C'mon, Mardek! Let's defeat this evil villain and save the world from his evil!"
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "grin",
				text = "I'll kick his evil so hard it'll fall down and prob'ly die next week!",
			),
			ActionTalk(
				speaker = targetMugbert,
				expression = "angr",
				text = "I'll 'ave fun beatin' up you lickle gits! Again!",
			),
			ActionBattle(
				battle = Battle(
					startingEnemies = arrayOf(Enemy(
						monster = content.battle.monsters.find { it.name == "mugbert" }!!,
						level = 3,
					), null, null, null),
					enemyLayout = content.battle.enemyPartyLayouts.find { it.name == "SOLO" }!!,
					music = "BossBattle",
					lootMusic = "VictoryFanfare2",
					background = content.battle.backgrounds.find { it.name == "crashsite" }!!,
					canFlee = false,
					isRandom = false,
				),
				overridePlayers = null,
			),
			ActionTalk(
				speaker = targetMugbert,
				expression = "cry",
				text = "Waaah! You idiots are so MEAN! I'm gunna tell my dad on you! I'm gunna get you done! Waaah!",
			),
			ActionWalk(
				target = targetMugbert,
				destinationX = 7,
				destinationY = 9,
				speed = WalkSpeed.Run,
			),
			ActionWalk(
				target = targetMugbert,
				destinationX = 7,
				destinationY = 13,
				speed = WalkSpeed.Run,
			),
			ActionWalk(
				target = targetMugbert,
				destinationX = 6,
				destinationY = 13,
				speed = WalkSpeed.Run,
			),
			ActionWalk(
				target = targetMugbert,
				destinationX = 6,
				destinationY = 22,
				speed = WalkSpeed.Run,
			),
			ActionVanish(targetMugbert),
			ActionTimelineTransition("MainTimeline", "Defeated Mugbert"),
		),
		ids = arrayOf(
			UUID.fromString("f0a1cd07-5616-4636-adf6-f43a1b4d70d4"),
			UUID.fromString("00218b3c-de6f-4f74-be4a-4a6b6764be3f"),
			UUID.fromString("c5eebe5f-4310-41e3-9428-05fcb896dabe"),
			UUID.fromString("caad71fa-51c0-45bb-b182-08b16461d6bb"),
			UUID.fromString("57279d3d-ebff-443b-8b16-fa823a9271e1"),
			UUID.fromString("19c0b643-c9de-42fd-b9d9-3faf7b96ff89"),
			UUID.fromString("77b9677d-406c-4759-b93b-6e022b846cad"),
			UUID.fromString("8fa91f41-dfe6-4b3d-89de-341d30abf4b8"),
			UUID.fromString("a0ed4eae-7ae7-4755-96c9-4abf63220cb9"),
			UUID.fromString("6ec01045-c5cd-4d4c-8297-e84658507d8a"),
			UUID.fromString("71433079-3e14-4798-a728-fb0b7e755198"),
			UUID.fromString("96ed010d-bca6-4641-b167-d79e49e3cc16"),
			UUID.fromString("80ba3b2d-e89f-478e-9bdc-a1121b97fb54"),
			UUID.fromString("0fad648a-a9ed-419a-a0ac-0d44f7afec5c"),
			UUID.fromString("88b3f32e-34e2-4ce2-ae32-80a13f10f923"),
			UUID.fromString("571e07e6-7b99-470e-93fd-4e309f1aba7a"),
			UUID.fromString("d3ebdf4c-0a12-47c6-9f50-1170c17a2529"),
			UUID.fromString("8f5f2309-9d9d-46f0-ae49-26da133fc4c5"),
			UUID.fromString("378a1aa2-b7c9-4745-9aea-0b3f3e727d6b"),
			UUID.fromString("0cff204b-e98c-47b3-99c5-78b4411a6b10"),
			UUID.fromString("5ad31e59-d019-4eae-ae1a-bf884968a1a6"),
			UUID.fromString("55bf3aea-45f8-421f-b66b-bc8acb09cb82"),
		),
	)

	hardcoded["crashsite"] = mutableListOf(
		ActionSequence(name = "MugbertActions", root = mugbertRoot!!)
	)

	val slamSound = content.audio.fixedEffects.battle.critical
	val rohophPortrait = content.portraits.info.find { it.flashName == "rm_mardek_child" }!!
	val targetUnknownRohoph = ActionTargetCustom(ActionTargetData(
		displayName = "????",
		element = content.stats.elements.find { it.rawName == "LIGHT" }!!,
		portraitInfo = rohophPortrait,
	))
	val targetRohoph = ActionTargetCustom(ActionTargetData(
		displayName = "Rohoph",
		element = content.stats.elements.find { it.rawName == "LIGHT" }!!,
		portraitInfo = rohophPortrait,
	))
	val rohophBody = ActionTargetAreaCharacter(
		UUID.fromString("3a6d4748-3603-4716-b936-600286df20bd")
	)

	val rohophSoul = ActionSpawnAreaEffect.Instance(
		UUID.fromString("438f1314-a005-44cf-8bff-16ee1e73009e"),
		content.actions.areaEffects.find { it.name == "RohophSoul" }!!
	)
	val rohophSoulAbsorb = ActionSpawnAreaEffect.Instance(
		UUID.fromString("5f35ba3a-eb50-4d6d-b7e5-5d879e03418c"),
		content.actions.areaEffects.find { it.name == "RohophSoulAbsorb" }!!
	)

	val rohophRoot = fixedActionChain(
		actions = arrayOf(
			ActionRotate(ActionTargetPartyMember(1), Direction.Up),
			ActionFlashScreen(rgb(1f, 1f, 1f)),
			ActionPlaySound(slamSound),
			ActionChangeAmbience(ColorTransform(
				addColor = 0,
				multiplyColor = rgb(0.2f, 0.6f, 1f),
				subtractColor = 0,
			), 0.milliseconds),
			ActionWait(1300.milliseconds),
			ActionShake(rohophBody, 3, 50.milliseconds, 800.milliseconds),
			ActionWait(150.milliseconds),
			ActionFlashScreen(rgb(1f, 1f, 1f)),
			ActionPlaySound(slamSound),
			ActionSpawnAreaEffect(rohophSoul, 40, 36),
			ActionWait(1600.milliseconds),
			ActionMoveAreaEffect(rohophSoul, 40, 68, 1000.milliseconds),
			ActionPlaySound(slamSound),
			ActionRemoveAreaEffect(rohophSoul),
			ActionSpawnAreaEffect(rohophSoulAbsorb, 40, 68),
			ActionChangeAmbience(null, 2600.milliseconds),
			ActionSetMusic("Rohoph"),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "shok",
				text = "What the? Mardek! What just happened?!? Are you okay?"
			),
			ActionAddEncyclopediaPerson(
				content.encyclopedia.people.find { it.snapshots[0].firstName == "Rohoph" }!!
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "zzz",
				text = ". . .",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "susp",
				text = "...Mardek?",
			),
			ActionRotate(
				target = ActionTargetPartyMember(0),
				newDirection = Direction.Possessed,
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "deep",
				text = "Ah... Good, it worked... Unfortunate choice of body, though. But ah well. Can't be helped. " +
						"It was the only compatible elemental type host around.",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "shok",
				text = "...Mardek?",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "norm",
				text = "Hm. I say, you there. Inhabitant. Where is this? No wait... Do you know of the Annunaki? " +
						"The Lingons? The Astrostles Alliance?",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "shok",
				text = "Huh? What? Mardek, you're scaring me...!",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "shok",
				text = "Well, I'm starting to scare me too! " +
						"I don't know what I'm doing! That's not me saying those th-",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "deep",
				text = "Silence, host. So... this is not a Developed World then? Interesting... " +
						"I might be safe here... at least for a little while. " +
						"But at the same time, I'll be so very vulnerable...",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "sad",
				text = "Uhm... If you're pretending, Mardek, then stop it... It's not very funny...",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "smile",
				text = "I suppose you're pretty confused, Inhabitants! " +
						"*Underdeveloped* and *HUMAN* Inhabitants, might I add...",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "norm",
				text = "I'll just be blunt and explain. You see, I happen to have crashed my ga-... " +
						"uh, my 'flying chariot' on this planet of yours, and it appears my other body " +
						"couldn't take the impact and died. A pity."
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "smile",
				text = "But I still have business in this plane, so I transferred my soul to this host. " +
						"So now I'm sharing its body. You don't need to *understand* it; you just need to accept it, " +
						"because I'm going nowhere, and you'd have to kill this creature if you wanted to get at me.",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "shok",
				text = "Urk!",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "deep",
				text = "But despite my prior tone, I mean you no harm at all! " +
						"No... harm is what I'm against, which is why I'm here now...",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "norm",
				text = "And I'm sorry to have inconvenienced with you with my presence, but... " +
						"just be glad I didn't expel this creature's soul from its body, " +
						"because I COULD have done that and taken full control! " +
						"I'm not cruel enough though, so I'd rather share.",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "norm",
				text = "You'll have to put up with me for a while though, I'm afraid, " +
						"because I can only get out of this body when it's ter- ...Uh. Never mind that, actually.",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "smile",
				text = "I suppose I might as well try to settle in immediately. So tell me, creatures, " +
						"what you call yourselves! What are your names? You're larvae, correct?",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "shok",
				text = "I'm not a caterpillar! And... And I don't know what's going on! I'm confused! " +
						"This is... too *weird*...",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "susp",
				text = "But you're some kind of... thing... in Mardek's body that can talk out of his mouth, " +
						"but you're not him?",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "grin",
				text = "That is correct, little creature! I'm surprised you followed what I was " +
						"saying so well seeing as you're but a youngling, and an Underdeveloped one at that!",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "smile",
				text = "But my, I'm being so rude! I haven't let my host speak in ages! Do forgive me.",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "shok",
				text = "Mmph! Huh? Well, I couldn't speak! You were doing that, weren't you? " +
						"Don't do that! Get out of me!",
			),
			ActionTalk(
				speaker = targetUnknownRohoph,
				expression = "smile",
				text = "I'm afraid I can't do that, O Host. I need to perpetuate myself in this plane, " +
						"and the bond I just made to this body is rather permanent, " +
						"so you and I will be together for a long time! " +
						"You'll just have to get used to having me sharing your body.",
			),
			ActionTalk(
				speaker = targetRohoph,
				expression = "smile",
				text = "My name's Rohoph, by the way. A pleasure it is to make your acquaintance, " +
						"Larval Human Inhabitants of this world.",
			),
			ActionTalk(
				speaker = targetRohoph,
				expression = "deep",
				text = "You two seem understandably distressed. If you want, I can be silent for a while? " +
						"I need to mull over my thoughts anyway. I suggest you two rest a bit and calm down. " +
						"We can talk more later.",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(0),
				expression = "deep",
				text = "Yeh, shut up you weird thing! I... I need to go and lie down...",
			),
			ActionTalk(
				speaker = ActionTargetPartyMember(1),
				expression = "deep",
				text = "Mardek, I don't know exactly what just happened or what it all means, " +
						"but I think we should head back to the village now... So come on, let's go...",
			),
			ActionTimelineTransition("MainTimeline", "After the conversation in Rohophs saucer is finished")
		),
		ids = arrayOf(
			UUID.fromString("9cd2ec52-ee36-4176-bc93-d48e99ceefb3"),
			UUID.fromString("d6ded936-ccd3-4731-98ae-b6dadb756e23"),
			UUID.fromString("65bbfedb-b0c1-4dd5-a40c-a8a2b5bc3765"),
			UUID.fromString("b4e5d05f-e9a3-4128-bb9b-f43331497003"),
			UUID.fromString("6a038290-5633-469b-8577-02b9974578d5"),
			UUID.fromString("29cc4e73-3d34-49b8-8f6d-3622787ead26"),
			UUID.fromString("f6256afe-4b2b-4c01-a4d9-e8915dae40fc"),
			UUID.fromString("1d6b166c-81ef-439f-b6f1-9c35ca073520"),
			UUID.fromString("0d3f9569-cd76-4444-ac08-9b41f86d262c"),
			UUID.fromString("6c594515-f0b3-4647-92f9-de7cc87ba755"),
			UUID.fromString("8d89f586-3144-4f0b-a1e8-4623aceb634a"),
			UUID.fromString("c9464107-4ee0-458a-a9a1-071384b11964"),
			UUID.fromString("835e3e16-31a7-4169-b56b-cc83d0cec823"),
			UUID.fromString("ca17b93d-77bf-43f0-9d5e-f4b3909cb603"),
			UUID.fromString("f01e0431-e4e4-4f3c-b804-cee05a7dede0"),
			UUID.fromString("b56fde93-eeb8-49a2-aadc-a6d23d80fec6"),
			UUID.fromString("ace4db6b-b104-4cd7-a1db-c2dd79a48fd4"),
			UUID.fromString("5aa64568-4b55-4587-bb72-042942d1df06"),
			UUID.fromString("28b60d10-35ec-45e7-9fb4-fd1e46779f4b"),
			UUID.fromString("fe5247d5-446a-4def-8e90-3f9f1700db1d"),
			UUID.fromString("25c2538f-ed3e-4e5c-b380-aabee8490b3e"),
			UUID.fromString("b215c995-bfff-4efe-959b-3bba3737c106"),
			UUID.fromString("3f960dba-9ae0-4e15-b96e-99fc45c0ffdb"),
			UUID.fromString("3deab709-46d2-4e23-9b15-6076a74719e1"),
			UUID.fromString("e48fd26c-436d-417f-8d9d-1ee26f572e61"),
			UUID.fromString("462356ef-df1f-48f2-8c02-6869ad527141"),
			UUID.fromString("7d4585b5-43de-4a05-a047-a51c10c8a3f1"),
			UUID.fromString("74e28f21-4424-40f7-8951-4d2585949d2a"),
			UUID.fromString("81c41a53-3ef8-4306-8604-5b8c54eb1fa4"),
			UUID.fromString("13373ba5-69cd-43db-acfc-393f1c3b0b30"),
			UUID.fromString("1209e2d7-de93-4e36-8e1e-e3fbebe8775c"),
			UUID.fromString("98fe6514-e122-4a01-921a-df4ddadb9b80"),
			UUID.fromString("3ab2f9a0-f371-4b7d-8d9f-7f0339ae6665"),
			UUID.fromString("e5974b41-ba2c-49a7-a585-23a692cc1cae"),
			UUID.fromString("ce70a8a7-15a0-4d88-8a07-b526aef54057"),
			UUID.fromString("60f26579-f914-49ce-a364-19fab452d8db"),
			UUID.fromString("c9d3bd02-a0d6-4199-b361-a36032cacea6"),
			UUID.fromString("a2436553-8672-44cc-876e-cb1b368d7723"),
			UUID.fromString("63a2c259-a4ca-421b-be45-e95ba8d6f601"),
			UUID.fromString("09f66fc2-b304-466e-9ef7-148f4e551267"),
			UUID.fromString("f27e0ad1-8c7f-4c3a-a23e-3ac9cc53682c"),
			UUID.fromString("798de121-b755-4c8e-8d4e-c94d246452bb"),
			UUID.fromString("9c6a4593-8d21-4fad-9bae-c563302cc766"),
			UUID.fromString("eabed858-b12c-49c2-845f-8b64a3188738"),
			UUID.fromString("4c964c0b-a4be-41e7-b23c-b7356abd7c9f"),
			UUID.fromString("d61a1540-615d-4c21-803c-45a692e4d65b"),
			UUID.fromString("99020d76-c736-41a4-9821-cb313f55488b"),
			UUID.fromString("4a4eaa3a-20a6-4d01-8bd8-0c11089b4578"),
		)
	)

	val bodyRoot = FixedActionNode(
		id = UUID.fromString("e4028a23-6270-42f6-8409-7aa69a789d87"),
		action = ActionTalk(
			speaker = targetRohoph,
			expression = "deep",
			text = "Ah, the old corpse... It served me well for many hundreds of years, but alas, now it's ruined.",
		),
		next = null,
	)

	hardcoded["saucer"] = mutableListOf(
		ActionSequence(name = "RohophToMardek", root = rohophRoot!!),
		ActionSequence(name = "RohophBody", root = bodyRoot),
	)
}
