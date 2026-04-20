package mardek.game.portrait

import mardek.content.animation.SpecialAnimationNode
import mardek.game.TestingInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull

object TestSpecialPortraitNodes {

	fun testPresenceAndAbsence(instance: TestingInstance) {
		val portraitInfoDeugan = instance.heroDeugan.portraitInfo
		val portraitInfoPrincess = instance.content.portraits.info.find { it.flashName == "princess" }!!
		assertEquals("deugan_hero", portraitInfoDeugan.flashName)
		assertEquals("hum", portraitInfoDeugan.rootSkin)
		assertEquals("deugan_hero", portraitInfoDeugan.faceSkin)
		assertEquals("deugan_hero", portraitInfoDeugan.hairSkin)
		assertEquals("green", portraitInfoDeugan.eyeSkin)
		assertEquals("deugan_hero", portraitInfoDeugan.eyeBrowSkin)
		assertEquals("", portraitInfoDeugan.mouthSkin)
		assertEquals("deugan_hero", portraitInfoDeugan.armorSkin)
		assertNull(portraitInfoDeugan.robeSkin)
		assertNull(portraitInfoDeugan.faceMask)
		assertEquals("1", portraitInfoDeugan.ethnicitySkin)
		assertNull(portraitInfoDeugan.voiceStyle)
		assertNull(portraitInfoDeugan.elementalBackground)

		val portraitDeugan = instance.content.portraits.animations.skins[portraitInfoDeugan.rootSkin]!!
		val portraitPrincess = instance.content.portraits.animations.skins[portraitInfoPrincess.rootSkin]!!
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
			assertFalse(portraitDeugan.get().hasSpecialNode(unexpected))
			assertFalse(portraitPrincess.get().hasSpecialNode(unexpected))
		}

		for (expected in arrayOf(
			SpecialAnimationNode.PortraitExpressions,
			SpecialAnimationNode.PortraitMouthExpressions,
			SpecialAnimationNode.PortraitFace,
			SpecialAnimationNode.PortraitHair,
			SpecialAnimationNode.PortraitEye,
			SpecialAnimationNode.PortraitEyeBrow,
			SpecialAnimationNode.PortraitEthnicity,
			SpecialAnimationNode.PortraitArmor,
		)){
			assertTrue(portraitDeugan.get().hasSpecialNode(expected))
			assertTrue(portraitPrincess.get().hasSpecialNode(expected))
		}

		// Mouth is only for humanoid females, and basically just for Zombie Shaman
		assertFalse(portraitDeugan.get().hasSpecialNode(SpecialAnimationNode.PortraitMouth))
		assertTrue(portraitPrincess.get().hasSpecialNode(SpecialAnimationNode.PortraitMouth))
	}
}
