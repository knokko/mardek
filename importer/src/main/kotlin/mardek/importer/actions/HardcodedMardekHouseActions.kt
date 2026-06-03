package mardek.importer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.action.ActionAddEncyclopediaPerson
import mardek.content.action.ActionEndOfChapter
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionRotate
import mardek.content.action.ActionSequence
import mardek.content.action.ActionSetBackground
import mardek.content.action.ActionSetBackgroundImage
import mardek.content.action.ActionSetMusic
import mardek.content.action.ActionSetOverlayColor
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTargetCustom
import mardek.content.action.ActionTargetData
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionTargetWholeParty
import mardek.content.action.ActionTeleport
import mardek.content.action.ActionTimelineTransition
import mardek.content.action.ActionToArea
import mardek.content.action.ActionToGlobalActions
import mardek.content.action.ActionWalk
import mardek.content.action.ChoiceActionNode
import mardek.content.action.ChoiceEntry
import mardek.content.action.FixedActionNode
import mardek.content.action.ExpressionActionNode
import mardek.content.action.WalkSpeed
import mardek.content.area.Direction
import mardek.content.sprite.NamedSprite
import mardek.content.expression.ConstantStateExpression
import mardek.content.expression.DefinedVariableStateCondition
import mardek.content.expression.IfElseStateExpression
import mardek.content.expression.ExpressionActionNodeValue
import mardek.importer.util.loadBc7Sprite
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal fun hardcodeMardekHouseActions(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val timeOfDay = content.story.customVariables.find { it.name == "TimeOfDay" }!!
	val fallenStarQuest = content.story.quests.find { it.tabName == "The Fallen Star" }!!
	val targetMardek = ActionTargetPartyMember(0)

	val motherRoot = ExpressionActionNode( // TODO CHAP2 Add the chapter 2 cases
		id = UUID.fromString("e724fd84-61cc-4209-bf61-1614e28f27a2"),
		expression = IfElseStateExpression(
			condition = DefinedVariableStateCondition(timeOfDay),
			ifTrue = IfElseStateExpression(
				condition = DefinedVariableStateCondition(fallenStarQuest.wasCompleted),
				ifTrue = ConstantStateExpression(ExpressionActionNodeValue(fixedActionChain(
					actions = arrayOf(
						ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "norm",
							text = "Hello again, dear. You're back a bit late today...",
						),
						ActionTalk(
							speaker = targetMardek,
							expression = "susp",
							text = "I am...? Well, it feels like I've barely been gone any time at all!",
						),
						ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "norm",
							text = "Oh well. Time does fly when you're having fun.",
						),
						ActionTalk(
							speaker = ActionTargetDefaultDialogueObject(),
							expression = "norm",
							text = "But you should go to bed, Mardek! It IS pretty late. " +
									"So nighty night, dear, and sweet dreams!",
						),
					),
					ids = arrayOf(
						UUID.fromString("fc96a905-44b1-4ca2-8e4a-b7e42fc7ecfb"),
						UUID.fromString("90444319-7ee7-4769-bf4e-b069ddd35b2c"),
						UUID.fromString("a02a7a47-5dc9-40a9-b3de-962c81b2c000"),
						UUID.fromString("ce535196-5687-4598-8f4f-740acfb508c3"),
					),
				))),
				ifFalse = ConstantStateExpression(ExpressionActionNodeValue(FixedActionNode(
					id = UUID.fromString("b436e5c5-3163-4576-86d3-df4c435934c6"),
					action = ActionTalk(
						speaker = ActionTargetDefaultDialogueObject(),
						expression = "smile",
						text = "Sweet dreams, dear.",
					),
					next = null
				))),
			),
			ifFalse = ConstantStateExpression(ExpressionActionNodeValue(FixedActionNode(
				id = UUID.fromString("51b51a45-e908-48f3-8ee1-ac55b5a0d32c"),
				action = ActionTalk(
					speaker = ActionTargetDefaultDialogueObject(),
					expression = "norm",
					text = "Oh, good morning dear. You and Deugan are going out on another adventure again, right? " +
							"Have fun with that, but mind you don't hurt yourselves!",
				),
				next = null
			)))
		)
	)

	val targetMother = ActionTargetAreaCharacter(UUID.fromString(
		"3993b751-bd8c-46a1-92ae-18bbda25aa48"
	), persistent = false)

	val enkiBackground = NamedSprite(
		id = UUID.fromString("37881fca-d1cc-451a-bb9e-434f84603a0f"),
		name = "Enki Art",
		sprite = loadBc7Sprite("mardek/importer/actions/enki-art.png")
	)
	content.actions.backgroundImages.add(enkiBackground)
	val afterDragonLairFinalActions = fixedActionChain(
		actions = arrayOf(
			ActionTalk(
				speaker = targetMardek,
				expression = "norm",
				text = "Well, I can sort of feel... that he's not dead? Like he's looking over me but he's not dead?",
			),
			ActionTalk(
				speaker = targetMother,
				expression = "deep",
				text = "Yes, that's what I feel too... " +
						"It must be the spiritual link that we all share, as a family. Because those exist!",
			),
			ActionSetMusic("Enki"),
			ActionSetBackgroundImage(enkiBackground),
			ActionAddEncyclopediaPerson(content.encyclopedia.people.find {
				it.snapshots[0].firstName == "Lilanea"
			}!!),
			ActionAddEncyclopediaPerson(content.encyclopedia.people.find {
				it.snapshots[0].firstName == "Enki"
			}!!),
			ActionTalk(
				speaker = targetMother,
				expression = "norm",
				text = "Ah... Your father... He was a great man. 'Enki The Wanderer', he used to be called, " +
						"before he met me and we settled down and had you."
			),
			ActionTalk(
				speaker = targetMother,
				expression = "smile",
				text = "He was, as I've told you many a time before now, an adventurer of great skill and renown, " +
						"revered throughout the lands. Oh, I fancied him so much before I even met him!"
			),
			ActionTalk(
				speaker = targetMother,
				expression = "norm",
				text = "He was Light, so he thought about others much more than himself. " +
						"All he did was for people he usually didn't even know from Adam..."
			),
			ActionTalk(
				speaker = targetMother,
				expression = "susp",
				text = "He was kind, honest... And yet... Mysterious. " +
						"Did you know that he never told me about his past?",
			),
			ActionTalk(
				speaker = targetMardek,
				expression = "smile",
				text = "Well, you have told me like a million times, mum.",
			),
			ActionTalk(
				speaker = targetMother,
				expression = "norm",
				text = "Well I'll tell you again! He never did tell me about his mystery-shrouded past... " +
						"But though I never knew about it, I never for one moment felt suspicious about him, " +
						"like he might've been something bad once. " +
						"He seemed to radiate this... this *feeling* that he was always a good man, " +
						"and that he had his reasons for hiding things like he did."
			),
			ActionTalk(
				speaker = targetMother,
				expression = "deep",
				text = "Sigh... I miss him so much...",
			),
			ActionTalk(
				speaker = targetMardek,
				expression = "deep",
				text = "Well, I do too mum... I wish he'd get back soon.",
			),
			ActionTalk(
				speaker = targetMother,
				expression = "smile",
				text = "Don't worry, dear. He'll come back one day. One day...",
			),
			ActionSetMusic(null),
			ActionSetBackgroundImage(null),
			ActionTalk(
				speaker = targetMother,
				expression = "smile",
				text = "But it's getting pretty late now, Mardek! You should go to bed."
			),
			ActionTalk(
				speaker = targetMother,
				expression = "smile",
				text = "Sweet dreams, dear. Sleep tight! Don't let the bedbugs randomly battle you during the night!",
			),
			ActionTalk(
				speaker = targetMardek,
				expression = "grin",
				text = "Well, night mum!",
			),
		),
		ids = arrayOf(
			UUID.fromString("641296f8-941e-4885-a285-5923c547e91e"),
			UUID.fromString("a3306860-7880-4dc1-b43e-e89c31acf244"),
			UUID.fromString("f7e17bf0-5758-48c0-91a4-9092e67ffd60"),
			UUID.fromString("b1d28720-39af-4399-a450-f665eab02fbf"),
			UUID.fromString("5f6958c2-f0aa-431a-8db5-4bb15ccef33b"),
			UUID.fromString("fe676af5-18eb-4c5b-823f-aab54494297a"),
			UUID.fromString("5d73a564-1380-48db-b293-6d8c87d1f2f0"),
			UUID.fromString("9bb2978b-c307-4d19-a784-693344f620cb"),
			UUID.fromString("3e18e40f-37b1-4ed4-8d9d-4e705511f823"),
			UUID.fromString("91c99955-db22-4aff-b04c-33a796ec7ebe"),
			UUID.fromString("8cd1dbea-2cfd-4061-86a3-d63b7d1797ae"),
			UUID.fromString("ea7afd98-b95b-4f29-838c-d947c73435aa"),
			UUID.fromString("ef141d06-9365-42b5-b65e-8744a7c0713b"),
			UUID.fromString("06975146-f7c1-4e2e-962d-c5421a0a9523"),
			UUID.fromString("24133a9a-2986-4856-b7f0-2ac11a5b3518"),
			UUID.fromString("f0753f54-2e89-473d-bf90-7df88abefa8c"),
			UUID.fromString("d0655f97-43be-4b0c-9678-543a2df43b50"),
			UUID.fromString("9b2729cc-902e-4213-9af0-f2824e2aff24"),
			UUID.fromString("c2dfc449-6716-41bd-b865-b66f198abf59"),
			UUID.fromString("0aca89f8-d285-4bba-9603-d6e86819533c"),
		),
	)
	val afterDragonLairChoice2 = ChoiceActionNode(
		id = UUID.fromString("5c5d758d-9500-410e-af45-609a54f6cd79"),
		speaker = targetMardek,
		options = arrayOf(
			ChoiceEntry(
				expression = "grin",
				text = "Well, I'm sure he'll get back soon!",
				next = FixedActionNode(
					id = UUID.fromString("204dcb6e-854a-4ba8-b4e9-20e56ed50122"),
					action = ActionTalk(
						speaker = targetMother,
						expression = "smile",
						text = "I hope you're right, dear... It's been so long... " +
								"But I am sure he'll be back eventually; I can *sense* it! Can you sense it too, dear?"
					),
					next = afterDragonLairFinalActions,
				),
			),
			ChoiceEntry(
				expression = "deep",
				text = "When will dad get back...?",
				next = FixedActionNode(
					id = UUID.fromString("1f8626b2-6070-44a4-9a94-dfb8f8bfe181"),
					action = ActionTalk(
						speaker = targetMother,
						expression = "smile",
						text = "I don't know, dear... He's been gone a long time. But I'm so sure he'll be back; " +
								"I can *sense* it! Can you sense it too, dear?"
					),
					next = afterDragonLairFinalActions,
				),
			),
		)
	)
	val afterDragonLairChoice1 = ChoiceActionNode(
		id = UUID.fromString("dcf4e5a7-c6d6-4cdd-aae2-1a0bbe313c90"),
		speaker = targetMardek,
		options = arrayOf(
			ChoiceEntry(
				expression = "grin",
				text = "It was great! We slew a princess and saved a dragon!",
				next = FixedActionNode(
					id = UUID.fromString("98d9949f-3066-4d94-a982-c65ae3749371"),
					action = ActionTalk(
						speaker = targetMother,
						expression = "smile",
						text = "You did? That sounds... unconventional. I mean, nice. Yes, that sounds nice, dear. " +
								"I'm glad you had fun.",
					),
					next = FixedActionNode(
						id = UUID.fromString("b904e6fb-ce16-4fb1-bcd8-47bafde567de"),
						action = ActionTalk(
							speaker = targetMother,
							expression = "deep",
							text = "Sigh... You're growing up to be just like your father... " +
									"Only he slew dragons and never really saved any princesses... Except me... " +
									"He used to call me his 'Queen', you know? I do wonder where he is now though..."
						),
						next = afterDragonLairChoice2,
					)
				),
			),
			ChoiceEntry(
				expression = "norm",
				text = "Well, it was okay.",
				next = FixedActionNode(
					id = UUID.fromString("da9acf72-b32c-47aa-be15-2d8afb1b8ec2"),
					action = ActionTalk(
						speaker = targetMother,
						expression = "smile",
						text = "That sounds nice, dear. I'm glad you had fun.",
					),
					next = FixedActionNode(
						id = UUID.fromString("3895a7f3-390b-4a54-9f99-67227ac03bd2"),
						action = ActionTalk(
							speaker = targetMother,
							expression = "deep",
							text = "Sigh... You're growing up to be just like your father... Always 'adventuring'... " +
									"You have the same adventurous, heroic spirit. Sigh... I wonder where he is now..."
						),
						next = afterDragonLairChoice2,
					)
				),
			),
			ChoiceEntry(
				expression = "deep",
				text = "Well, I wish I could go on a REAL adventure...",
				next = FixedActionNode(
					id = UUID.fromString("27e5b8e5-e27e-4f36-abbb-90e41f6eb3f7"),
					action = ActionTalk(
						speaker = targetMother,
						expression = "smile",
						text = "Hmhm, you certainly do have your father's spirit for true adventure! " +
								"He was never satisfied with just the inane little fetch quests, no... " +
								"He always wanted the big, epic, dragon-slaying quests..."
					),
					next = FixedActionNode(
						id = UUID.fromString("8e1c9681-650d-4403-88c5-b56a80a12ddd"),
						action = ActionTalk(
							speaker = targetMother,
							expression = "deep",
							text = "Sigh... You're growing up to be just like your father... " +
									"You have the same adventurous, heroic spirit. Sigh... I wonder where he is now...",
						),
						next = afterDragonLairChoice2,
					)
				),
			)
		)
	)
	val afterDragonLairRoot = FixedActionNode(
		id = UUID.fromString("be97dd5b-71f8-4315-8291-d57c2e753dcd"),
		action = ActionWalk(
			target = ActionTargetWholeParty(),
			destinationX = 3,
			destinationY = 5,
			speed = WalkSpeed.Normal,
		),
		next = FixedActionNode(
			id = UUID.fromString("c9a68785-fb84-4aa3-8233-6871c84d586a"),
			action = ActionWalk(
				target = ActionTargetWholeParty(),
				destinationX = 3,
				destinationY = 3,
				speed = WalkSpeed.Normal,
			),
			next = FixedActionNode(
				id = UUID.fromString("e866c65f-c622-4300-aed0-71eb46745f59"),
				action = ActionRotate(target = targetMother, newDirection = Direction.Down),
				next = FixedActionNode(
					id = UUID.fromString("141cc686-aa51-4dbf-925f-e9bfa0f97707"),
					action = ActionTalk(
						speaker = targetMardek,
						expression = "grin",
						text = "Well, mum, I'm back!",
					),
					next = FixedActionNode(
						id = UUID.fromString("ba8a71e7-ca45-4cd9-9253-a3ed13fa7489"),
						action = ActionTalk(
							speaker = targetMother,
							expression = "smile",
							text = "Oh, there you are, dear... How did your 'adventure' with Deugan go?"
						),
						next = afterDragonLairChoice1
					)
				),
			)
		)
	)
	hardcoded["gz_Mhouse1"] = mutableListOf(
		ActionSequence(name = "mother", root = motherRoot),
		ActionSequence(name = "AfterDragonLair", root = afterDragonLairRoot),
	)

	val targetDeugan = ActionTargetPartyMember(1)
	val fallingStarRoot = fixedActionChain(
		actions = arrayOf(
			ActionRotate(targetMardek, Direction.Sleep),
			ActionSetOverlayColor(color = rgb(0, 0, 0), transitionTime = 2.seconds),
			ActionToGlobalActions(),
			ActionPlayCutscene(content.actions.cutscenes.find { it.name == "Falling Star" }!!, true),
			ActionToArea("gz_Mhouse2", 0, 1, Direction.Sleep),
			ActionTimelineTransition("MainTimeline", "Searching for the fallen 'star'"),
			ActionTeleport(targetDeugan, 3, 5, Direction.Up),
			ActionWalk(targetDeugan, 3, 1, WalkSpeed.Normal),
			ActionWalk(targetDeugan, 1, 1, WalkSpeed.Normal),
			ActionRotate(targetMardek, Direction.Down),
			ActionTalk(
				speaker = targetDeugan,
				expression = "shok",
				text = "Uh, Mardek! Are you awake...? I just saw the weirdest thing!",
			),
			ActionTalk(
				speaker = targetDeugan,
				expression = "norm",
				text = "There was this star in the sky, right, even though it was daytime - well, morning really, " +
						"but same thing! - and I looked at it because it was weird, " +
						"and then it started like getting BIGGER!"
			),
			ActionTalk(
				speaker = targetDeugan,
				expression = "grin",
				text = "It was falling, Mardek! A falling star! And I saw where it landed, too! " +
						"It landed with a big crash and I saw where it went!"
			),
			ActionTalk(
				speaker = targetDeugan,
				expression = "deep",
				text = "It landed in a clearing just at the other side of the Soothwood, I think!",
			),
			ActionTalk(
				speaker = targetDeugan,
				expression = "smile",
				text = "We should maybe go and find it, Mardek! Who knows what kind of mystery and wonders it holds? " +
						"We've been looking for adventures for ages, and this might be our big chance... " +
						"Let's go and find the \$Fallen Star%!!"
			),
		),
		ids = arrayOf(
			UUID.fromString("8b01da8e-cbc9-48bf-9f6f-c13044012b98"),
			UUID.fromString("22ef1944-b240-48aa-ab35-466586a10dfd"),
			UUID.fromString("55e9cebd-9dcf-46f4-8652-9d774a5cbd23"),
			UUID.fromString("cb4f7c2e-2305-4e8d-bd0e-43603756b9df"),
			UUID.fromString("d34cd938-8256-48eb-b5f0-cee54bd59c07"),
			UUID.fromString("b9348e29-02e7-4424-9e3c-800b7ccad7ac"),
			UUID.fromString("2201ca41-0e06-465a-b743-be268c58092f"),
			UUID.fromString("5f3e802f-2ff2-44e9-bda7-21e99a727e52"),
			UUID.fromString("9c754cd2-e271-4941-9392-9bc17f3ea712"),
			UUID.fromString("4b61beb1-cb52-4e74-b757-7bda206838ec"),
			UUID.fromString("c57d9a79-27ad-4b2b-ac92-c42e6f672870"),
			UUID.fromString("129eb822-3fe1-4acd-83a4-5ea1befabb2d"),
			UUID.fromString("2cc8b5d4-6b18-49b9-b9c5-1e6b59bd32aa"),
			UUID.fromString("6e408c16-aa73-4db2-9040-51848348d8b9"),
			UUID.fromString("1786b4b3-fb04-49b3-94c8-4360177da681"),
		)
	)!!

	// The portrait list can be empty during unit tests
	if (content.battle.monsters.size <= 2) {
		hardcoded["gz_Mhouse2"] = mutableListOf(
			ActionSequence(name = "FallingStarCutscene", root = FixedActionNode()),
			ActionSequence(name = "TalkWithRohophInBed", root = FixedActionNode())
		)
		return
	}

	val childRohophPortrait = content.portraits.info.find { it.flashName == "rm_mardek_child" }!!
	val targetChildRohoph = ActionTargetCustom(ActionTargetData(
		displayName = "Rohoph",
		element = content.stats.elements.find { it.rawName == "LIGHT" }!!,
		portraitInfo = childRohophPortrait,
	))

	fun gdmTarget(name: String, elementName: String): ActionTargetCustom {
		val portrait = content.portraits.info.find { it.flashName.equals(name, ignoreCase = true) }!!
		return ActionTargetCustom(ActionTargetData(
			displayName = name,
			element = content.stats.elements.find { it.rawName == elementName }!!,
			portraitInfo = portrait,
		))
	}

	val targetBalthazar = gdmTarget("Balthazar", "WATER")
	val targetGaspar = gdmTarget("Gaspar", "FIRE")
	val targetMelchior = gdmTarget("Melchior", "AIR")
	val targetQualna = gdmTarget("Qualna", "ETHER")
	val targetMoric = gdmTarget("Moric", "EARTH")
	val targetAnu = gdmTarget("Anu", "DARK")

	val talkWithRohoph = fixedActionChain(arrayOf(
		ActionRotate(target = targetMardek, newDirection = Direction.Sleep),
		ActionTalk(
			speaker = targetMardek,
			expression = "zzz",
			text = ". . ."
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Down),
		ActionTalk(
			speaker = targetMardek,
			expression = "susp",
			text = "You! You weird thing in me! Can you hear me if I talk?",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Possessed),
		ActionSetMusic("Rohoph"),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "Obviously. I have your ears and share your brain now, so I can detect anything that you can, " +
					"including your *internal* monologues, might I add."
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "(But due to my incredible magical prowess and dominance in this 'relationship', " +
					"you can know nothing of MY mental activites. Ahah.)",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Down),
		ActionTalk(
			speaker = targetMardek,
			expression = "susp",
			text = "Huh?",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Possessed),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "Nevermind. You want to ask me something though, Host? I suppose I'll do my best to explain; " +
					"I mean, we WILL surely be spending years together from now on, " +
					"so it'd be best to be on good terms with one another.",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Down),
		ActionTalk(
			speaker = targetMardek,
			expression = "susp",
			text = "Well, I just want to know what you are! What are you?",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Possessed),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "deep",
			text = "Hm... Despite its apparent simplicity, " +
					"that's one of the most complex of queries you could've come up with...",
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "norm",
			text = "In short, I am... A Healer. Yes, that should do. I'm a magic-user of incredible skill, " +
					"specialising in Light-elemental recovery magic and so on. Inhabiting your body as I am, " +
					"I'll be able to lend you some of my power in any battles you may face in future. " +
					"Keep that in mind."
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "susp",
			text = "But also, since I know that wasn't a satisfactory answer to your question, " +
					"I am, uh... An 'Angel'? Is that what you call them here?",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Down),
		ActionTalk(
			speaker = targetMardek,
			expression = "susp",
			text = "You mean one of them things from the sky?",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Possessed),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "Yes... I think. " +
					"That's the word I got for what I am when I searched your brain's vocabulary lexicon, anyway. " +
					"'A being from the sky'. Yes, that seems apt.",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Possessed),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "deep",
			text = "I came here because... Hm, I don't know if I should tell you... I can't see what harm it'd do, " +
					"but I also can't see what GOOD it would do, so it's better to be safe than sorry.",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Down),
		ActionTalk(
			speaker = targetMardek,
			expression = "sad",
			text = "Aww! So you won't even tell me why you're in me?",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Possessed),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "norm",
			text = "Uh. I'm IN you, as you so nicely put it, because I had no choice. " +
					"As I said, my body died when my, uh, (blast! no word for it!)... 'flying thing' crashed here, " +
					"and I was so very close to having my soul wrenched from this plane of existence...",
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "norm",
			text = "O, what luck!, I thought when you entered my, uh... flying thing! I do apologise, " +
					"but the opportunity was just too good to ignore, and when you're in pure soulform, " +
					"you don't have much reasoning capability anyway.",
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "I still have a lot of business to attend to on this plane, though. Yes... " +
					"Why I'm here on this planet of yours relates to that.",
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "deep",
			text = "But I need time to regenerate a bit. Dying really weakened me! Which is understandable.",
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "I'll lie low for a bit here before mentioning anything to you... " +
					"So it's best you just accept my solemn stony silence on matters of my past, " +
					"person or purpose for the next few years and don't ask questions!",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Down),
		ActionTalk(
			speaker = targetMardek,
			expression = "blah",
			text = "Well, you talk too much, Thing!",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Possessed),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "susp",
			text = "My name's Rohoph, not 'Thing'. And hm... I do go on a bit, don't I? " +
					"But I'm the only one here with knowledge enough to provide any kind of exposition! Tsk!",
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "Anyway, I'm going to probably be rather silent for the next few years. " +
					"I'll still be in your body, but I'll keep to myself and work on regaining some of my power.",
		),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "During that time, I'm sure you'll get used to me though, and I to you. " +
					"Yes, it'll be a veritable frenzy of family-friendly fun, I'm sure! " +
					"We'll have all sorts of crazy, zany shenanigans, antics and hijinks, probably!",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Down),
		ActionTalk(
			speaker = targetMardek,
			expression = "grin",
			text = "Well, sounds fun!",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Possessed),
		ActionTalk(
			speaker = targetChildRohoph,
			expression = "smile",
			text = "Yes indeed. But now, rest, host. We can't be having us being sleepy and lethargic tomorrow... " +
					"It's probably going to be a long day...",
		),
		ActionRotate(target = targetMardek, newDirection = Direction.Sleep),
		ActionSetOverlayColor(color = rgb(0, 0, 0), transitionTime = 2.seconds),
		ActionToGlobalActions(),
		ActionSetMusic("GdM"),
		ActionPlayCutscene(content.actions.cutscenes.find { it.name == "GdM intro" }!!, false),
		ActionSetBackground(content.battle.backgrounds.find { it.name == "GdM" }!!),
		ActionTalk(
			speaker = targetBalthazar,
			expression = "blah",
			text = "Rohoph's escaped, you know. " +
					"He flew away in one of the gallopers, myes. YALORT knows where he went.",
		),
		ActionTalk(
			speaker = targetGaspar,
			expression = "angr",
			text = "HE NEEDS TO SANGUINARY WELL BE BURNED!! LET'S BURN HIM!!! " +
					"BURN HIS HEMIC FACE OFF AND ALSO HIS PRATTIN' ROBE! HIS WHITE ROBE!! " +
					"HIS DETRITAL WHITEY ROBE ROBE!! IT MUST BURN!! BUUURRRNNN!!!"
		),
		ActionTalk(
			speaker = targetMelchior,
			expression = "dreamy",
			text = "I like white. I find it goes well with yellow, but only if you want it to. " +
					"If you don't believe it does, then it doesn't. Just like bad smells. " +
					"You only think they're bad if you're told they're bad. I don't like bad smells."
		),
		ActionTalk(
			speaker = targetQualna,
			expression = "susp",
			text = "Ignoring how these caterpillars uttered what they're trying to say, I do agree; " +
					"Rohoph needs to be perish'd. He cannot be left to roam loose with the knowledge he possesses! " +
					"If he rallies enough of a resistance - which it seems he's trying to do - then, why sirs, " +
					"we're well and truly buggered twice and whenceways till sundown, sunup and then some!"
		),
		ActionTalk(
			speaker = targetMoric,
			expression = "deep",
			text = "Yes, kill him. Kill the traitor. Let his blood run free, his flesh turn cold, " +
					"his motor functions degrade and cease and his entire being become uncertain, his body a shell, " +
					"his soul a wanderer. Yeeess, ooohh, I do so want to see his flesh cold and crawling " +
					"with a million maggots, slowly chewing, chewing, chewing on the rot, yeeess, yeeeeesss...",
		),
		ActionTalk(
			speaker = targetGaspar,
			expression = "angr",
			text = "MORIC, YOU'RE A SMELTIN' CREEPY MAN!! AND YET I THINK I TOO MIGHT MONGIN' WELL DELIGHT IN THE " +
					"SIGHT OF THE FRIGHT OF THE WHITE BLIGHT AS I FIGHT AND SMITE HIM FROM THIS PLANE!! BLOODYIN'."
		),
		ActionTalk(
			speaker = targetQualna,
			expression = "grin",
			text = "Why Gaspar, that's awfully poetic for you! I'm amazed!",
		),
		ActionTalk(
			speaker = targetGaspar,
			expression = "glare",
			text = "I'LL HARRY WELL SHOW *YOU* A POETIC MAZE!! I'LL TRAP YOU IN IT AND SCREAM TOFFIN' " +
					"BLOOD-CURDLIN' SCREAMS AS YOU TRY IN VAIN TO EVADE THE PAIN YOU'D BE SURE TO GAIN!! " +
					"YOU'D SLOWLY GO INSANE, AND I'D JUST LAUGH!! LAUGH!!! MOGGIN' LAUGH TRIPIN' HARD!!",
		),
		ActionTalk(
			speaker = targetBalthazar,
			expression = "susp",
			text = "What ARE you talking about? Shut up anyway, it's irrelevant. " +
					"But we DO need to address the Rohoph problem.",
		),
		ActionTalk(
			speaker = targetBalthazar,
			expression = "norm",
			text = "Master, what think you? We should do away with him, myes? But how do we go about it?",
		),
		ActionTalk(
			speaker = targetAnu,
			expression = "glare",
			text = "Yes, underlings. If the deserter, the traitor, Rohoph, is left to run free, " +
					"though he alone could pose no serious threat to us, he could gain the alliance of others, " +
					"which he's frustratingly good at doing.",
		),
		ActionTalk(
			speaker = targetAnu,
			expression = "norm",
			text = "We have to do away with him. And quickly. He's the only one who knows, " +
					"so with him gone we'll be safe as long as we make nothing clear until it's too late for them.",
		),
		ActionTalk(
			speaker = targetBalthazar,
			expression = "blah",
			text = "One of us should go to stop him. But who? Any volunteers?",
		),
		ActionTalk(
			speaker = targetMelchior,
			expression = "dreamy",
			text = "I like volunteers. They taste like cherries, but only if that's what you believe. " +
					"If you prefer bananas, love and luck always taste a bit bananay. 'Bananay'. " +
					"That's a weird word to say. 'Bananay'. 'Bananananany'.",
		),
		ActionTalk(
			speaker = targetGaspar,
			expression = "angr",
			text = "I'M NOT CHARLIE WELL GOIN', 'CAUSE I BOGGIN' WELL CAN'T BE ARSED!!",
		),
		ActionTalk(
			speaker = targetMoric,
			expression = "deep",
			text = "I'll go. Yeeess, if Rohoph is going to die, I would derive much perverse satisfaction from " +
					"seeing the fear on his cyclopic face before he meets the Evereaper... Yeeeess, " +
					"I'd drink up his shock, his horror, and it would FUEL me! " +
					"It's what I live my life for, to see others end theirs!",
		),
		ActionTalk(
			speaker = targetMoric,
			expression = "grin",
			text = "I can't have enough ends. So I'll take my chances to see this one. I hope it's slow and painful; " +
					"the elation I get from such kills is unmatched by anything, yeeeess... " +
					"Just thinking about it makes me... oohhh...",
		),
		ActionTalk(
			speaker = targetAnu,
			expression = "blah",
			text = "Yes. You're creepy. I can't say we'll miss you while you're gone, so it'd be best if you did go. " +
					"We all win that way.",
		),
		ActionTalk(
			speaker = targetAnu,
			expression = "norm",
			text = "Take a battleship and follow Rohoph's trail. " +
					"It should be easy enough to detect where he went and to deal with him. Now go.",
		),
		ActionTalk(
			speaker = targetMoric,
			expression = "norm",
			text = "Yeeess, my Master...",
		),
		ActionTalk(
			speaker = targetAnu,
			expression = "glare",
			text = "When Rohoph's gone and dealt with, there'll be nothing to stop us... No... Soon, " +
					"THE GALAXY WILL BE OURS!!! MWAHAHAHAHAH!!!!",
		),
		ActionTalk(
			speaker = targetMelchior,
			expression = "susp",
			text = "Don't you mean *ours*, sir? We ARE working on this together. There's not just you.",
		),
		ActionTalk(
			speaker = targetAnu,
			expression = "blah",
			text = "Yes, that's what I SAID. Weren't you listening?",
		),
		ActionTalk(
			speaker = targetMelchior,
			expression = "grin",
			text = "What? Eh? No, not at all. I had a nice pretty tune in my head. I was listening to that. " +
					"It went like 'naaaah naaaah nah-nah-naaaah, naaaah naaaah naaaah naaahh...'",
		),
		ActionTalk(
			speaker = targetAnu,
			expression = "blah",
			text = "Sigh...",
		),
		ActionSetOverlayColor(rgb(0, 0, 0), 2.seconds),
		ActionSetMusic(null),
		ActionSetBackground(null),
		ActionSetOverlayColor(0, Duration.ZERO),
		ActionEndOfChapter(),
	), ids = arrayOf(
		UUID.fromString("dabbebeb-c5ad-42cb-aa2d-c834906c85b1"),
		UUID.fromString("fefeb29b-853f-4412-96bd-21b147daa5de"),
		UUID.fromString("f1518291-2373-4445-9a76-470b03e0073e"),
		UUID.fromString("23475dfa-53e1-4ce0-b62e-e5a123034213"),
		UUID.fromString("0e9ada35-8156-46e5-bb0b-05fe702dad8b"),
		UUID.fromString("47de6e5d-42db-4a63-a997-77624dc6c3cf"),
		UUID.fromString("0ab1d16e-a6db-4ca5-96e6-2321cfce2a03"),
		UUID.fromString("95fb0820-0643-4a2a-adcb-895fba93e62c"),
		UUID.fromString("cf3e57c3-2243-4601-8ff2-5315fb2757ab"),
		UUID.fromString("d57bb476-c7fc-4dc6-be31-c50fb35659d1"),
		UUID.fromString("954ba9be-f943-4eb0-a0de-7d968bb4582f"),
		UUID.fromString("3713f5d5-b3c4-4971-aaf6-b5b443b2ccc5"),
		UUID.fromString("45d22ae2-810b-40e8-b8f1-f0acf81606dc"),
		UUID.fromString("613fa6b0-7d7f-4594-95ea-eec4b641f77a"),
		UUID.fromString("cf18c34b-6794-4e55-b53b-b19b7afcda00"),
		UUID.fromString("5dc5db92-5e11-4385-bfd6-154a0263671e"),
		UUID.fromString("04b037b1-b410-4588-aa85-c1c496ede0d5"),
		UUID.fromString("e14cc2d7-763f-4581-babc-f480db9dbe20"),
		UUID.fromString("622831df-4790-49fa-a71a-915fe4eb5899"),
		UUID.fromString("2c9dac08-9d45-474c-9fd1-d19b417f44dc"),
		UUID.fromString("ddaaca8c-f131-4c2c-bb76-83ba9f1b6fd4"),
		UUID.fromString("31de337c-5380-46c7-bc03-7967fafee11f"),
		UUID.fromString("b55825e0-aab1-4418-b6a1-db65f3e39a75"),
		UUID.fromString("9947492c-70b2-4d5c-ba99-ca0280b6bd36"),
		UUID.fromString("324cad98-287a-48b7-8a7d-6ff32e9df5d7"),
		UUID.fromString("8e1b4b23-0924-4c6d-872e-5d27aeb0dae2"),
		UUID.fromString("59d8e47b-c58b-4454-aebd-bdde44446d3f"),
		UUID.fromString("a629a23c-9fb6-4b7e-a655-0bd4ea245761"),
		UUID.fromString("45bc3f9b-e22a-4e87-b0cc-c49a43261d87"),
		UUID.fromString("7f5ee801-8b52-4815-a0e3-445da5fbef27"),
		UUID.fromString("fd91b7cd-a8b1-415b-a4ff-5efec039c6f5"),
		UUID.fromString("29bc75a1-8c89-461b-89d4-4a9e5d20fb07"),
		UUID.fromString("e8194303-3280-4d09-bcc5-4275e3dd39bd"),
		UUID.fromString("0f4c64bb-a717-4ce5-928f-ae4fe773a7a7"),
		UUID.fromString("5e2eba9a-6bdc-456f-a5ae-7e68cf9f99f0"),
		UUID.fromString("d10669ea-6b42-4769-a73d-fb6cfde40785"),
		UUID.fromString("d3f3c592-0d04-48ea-b41f-cda8f5720a4a"),
		UUID.fromString("0f751c7d-4119-496a-8845-e801ca0cd0cd"),
		UUID.fromString("e959485d-4b6b-4490-bc30-06724d74c13a"),
		UUID.fromString("b0790a62-48ee-404f-b76f-e7884346d665"),
		UUID.fromString("50a01073-db8a-4e6d-a7e5-17d2d3c75f6d"),
		UUID.fromString("7feed041-5adc-4695-a8bc-fd4f31859ab9"),
		UUID.fromString("0146155a-8e1d-4f67-88a8-c530960c1371"),
		UUID.fromString("6095e713-4ad7-4c41-8658-655005ce64ed"),
		UUID.fromString("abccf0b0-e895-4173-98c5-55a63846f02e"),
		UUID.fromString("8e7cea3e-10c5-4e00-9260-0be817f9b42e"),
		UUID.fromString("5cf3b505-8034-4284-85ac-75ff9eea6244"),
		UUID.fromString("c1cdcda7-fbc5-45cb-8a88-a8091a775679"),
		UUID.fromString("b6046e50-cbdd-4d5a-a1ca-d167581964f4"),
		UUID.fromString("16f473d8-6e82-45f7-887d-220f2cfb1402"),
		UUID.fromString("ce91dff0-2e41-43e3-a8b5-3d3b7945fbb6"),
		UUID.fromString("fd400932-7b45-4993-9235-c1b20799480c"),
		UUID.fromString("607eccd0-b1be-453c-b9a2-8471f1a43723"),
		UUID.fromString("68a4f803-f9ee-4d0f-bd90-6c5520704340"),
		UUID.fromString("9dd2b1a3-37af-40d9-a560-0a0d39ac5d6c"),
		UUID.fromString("552af97b-e164-4d57-8c3b-bb691750c170"),
		UUID.fromString("63812d12-8b17-4288-a736-f2fb8d3caacf"),
		UUID.fromString("8bd9ea7b-c236-4ba0-a1f7-02b39bf83637"),
		UUID.fromString("4c64cbc8-3710-4833-b7f9-df87673e468f"),
		UUID.fromString("d35b4901-0da5-43fb-9763-b062c1b2c1a9"),
		UUID.fromString("cde33e08-a15f-4d32-ae2b-999fe8326989"),
		UUID.fromString("33ed0ea4-5f46-4656-a9c5-4ef78c010eac"),
		UUID.fromString("42ad0d5c-48f2-4384-8e3b-e8c2e99bc812"),
		UUID.fromString("2deaa806-52e4-40c1-940f-38fd4aaa8ad8"),
		UUID.fromString("6ca07127-0466-4e15-8ae9-ecbadf1a8b88"),
		UUID.fromString("8ca80b0c-4b41-4201-a9c3-e5d10f67b0fc"),
		UUID.fromString("f4cd149c-a38e-407e-9aa7-87e4a0e9480b"),
		UUID.fromString("2c9e0ce2-c2d4-4517-a4ba-6ce055bd1b3b"),
		UUID.fromString("d555a6bb-c638-4c17-8229-e5b88c12fd16"),
		UUID.fromString("75d0ae76-05d2-40e5-9125-94ed465f3bff"),
		UUID.fromString("7d1eb99e-6064-40c1-8818-b184f078a36e"),
		UUID.fromString("35b4f78f-e048-46b9-b7c0-a344ce0cadc3"),
		UUID.fromString("07ac90b2-37cf-4256-914a-6b48d1a13843"),
		UUID.fromString("5ad1fe5f-ccb9-4778-b881-327a0110cdc6"),
		UUID.fromString("e952164b-a4ae-427a-9a91-b3d4505116b5"),
		UUID.fromString("cd3493fb-5cac-41ba-8dac-3861b113a2bd"),
		UUID.fromString("ee266199-b041-40e3-8bc9-8ba7d386587b"),
		UUID.fromString("b44dfb3f-27db-4001-af8a-d7edd714b485"),
	))!!
	hardcoded["gz_Mhouse2"] = mutableListOf(
		ActionSequence(name = "FallingStarCutscene", root = fallingStarRoot),
		ActionSequence(name = "TalkWithRohophInBed", root = talkWithRohoph)
	)
}
