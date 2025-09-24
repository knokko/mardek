package mardek.game.portrait

import mardek.content.animation.SpecialAnimationNode
import mardek.game.TestingInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull

object TestSpecialPortraitNodes {

	fun testPresenceAndAbsence(instance: TestingInstance) {
		val portraitInfo = instance.heroDeugan.portraitInfo
		assertEquals("deugan_hero", portraitInfo.flashName)
		assertEquals("hum", portraitInfo.rootSkin)
		assertEquals("deugan_hero", portraitInfo.faceSkin)
		assertEquals("deugan_hero", portraitInfo.hairSkin)
		assertEquals("green", portraitInfo.eyeSkin)
		assertEquals("deugan_hero", portraitInfo.eyeBrowSkin)
		assertEquals("norm", portraitInfo.mouthSkin)
		assertEquals("deugan_hero", portraitInfo.armorSkin)
		assertNull(portraitInfo.robeSkin)
		assertNull(portraitInfo.faceMask)
		assertEquals("1", portraitInfo.ethnicitySkin)
		assertNull(portraitInfo.voiceStyle)
		assertNull(portraitInfo.elementalBackground)

		val portrait = instance.content.portraits.animations.skins[portraitInfo.rootSkin]!!
		for (unexpected in arrayOf(
			SpecialAnimationNode.HitPoint,
			SpecialAnimationNode.StrikePoint,
			SpecialAnimationNode.StatusEffectPoint,
			SpecialAnimationNode.TargetingCursor,
			SpecialAnimationNode.OnTurnCursor,
			SpecialAnimationNode.Core,
			SpecialAnimationNode.Exclaim,
			SpecialAnimationNode.ElementalSwing,
			SpecialAnimationNode.ElementalCastingCircle,
			SpecialAnimationNode.ElementalCastingSparkle,
			SpecialAnimationNode.ElementalCastingBackground,
			SpecialAnimationNode.Weapon,
			SpecialAnimationNode.Shield,
			SpecialAnimationNode.PortraitRobe,
		)) {
			assertFalse(portrait.hasSpecialNode(unexpected))
		}

		for (expected in arrayOf(
			SpecialAnimationNode.PortraitExpressions,
			SpecialAnimationNode.PortraitFace,
			SpecialAnimationNode.PortraitHair,
			SpecialAnimationNode.PortraitEye,
			SpecialAnimationNode.PortraitEyeBrow,
			SpecialAnimationNode.PortraitMouth,
			SpecialAnimationNode.PortraitEthnicity,
			SpecialAnimationNode.PortraitArmor,
		)){
			assertTrue(portrait.hasSpecialNode(expected))
		}
	}
}
