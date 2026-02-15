package mardek.importer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.Content
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionRotate
import mardek.content.action.ActionSequence
import mardek.content.action.ActionSetBackgroundImage
import mardek.content.action.ActionSetMusic
import mardek.content.action.ActionSetOverlayColor
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetAreaCharacter
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
import mardek.content.action.WalkSpeed
import mardek.content.area.Direction
import mardek.content.sprite.NamedSprite
import mardek.importer.util.loadBc7Sprite
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal fun hardcodeMardekHouseActions(
	content: Content, hardcoded: MutableMap<String, MutableList<ActionSequence>>
) {
	val motherRoot = FixedActionNode( // TODO CHAP1 Change this, depending on the timeline state
		id = UUID.fromString("b436e5c5-3163-4576-86d3-df4c435934c6"),
		action = ActionTalk(
			speaker = ActionTargetDefaultDialogueObject(),
			expression = "smile",
			text = "Sweet dreams, dear."
		),
		next = null,
	)

	val targetMardek = ActionTargetPartyMember(0)
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
			// TODO CHAP1 Add Enki to encyclopedia
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
		// TODO CHAP1 Add Lilanea to encyclopedia
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
			ActionPlayCutscene(content.actions.cutscenes.find { it.name == "Falling Star" }!!),
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
	hardcoded["gz_Mhouse2"] = mutableListOf(
		ActionSequence(name = "FallingStarCutscene", root = fallingStarRoot),
	)
}
