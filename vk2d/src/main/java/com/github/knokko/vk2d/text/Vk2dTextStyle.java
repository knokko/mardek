package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.utilities.ColorPacker;

public record Vk2dTextStyle(FillStyle fill, StrokeStyle stroke) {

	public Vk2dTextStyle multiplyAlpha(float factor) {
		return new Vk2dTextStyle(fill.multiplyAlpha(factor), stroke.multiplyAlpha(factor));
	}

	public Vk2dTextStyle withDifferentFillColor(int newFillColor) {
		return new Vk2dTextStyle(new Vk2dTextStyle.FillStyle(newFillColor), stroke);
	}

	public record FillStyle(int color, float distanceFactor, float distanceBias) {

		public FillStyle(int color) {
			this(color, 1f, 0f);
		}

		public FillStyle multiplyAlpha(float factor) {
			return new FillStyle(ColorPacker.multiplyAlpha(color, factor));
		}

		public FillStyle withManipulatedDistance(float factor, float bias) {
			return new FillStyle(color, factor, bias);
		}

		public Vk2dTextStyle only() {
			return new Vk2dTextStyle(this, StrokeStyle.NONE);
		}
	}

	public record StrokeStyle(int color, float width, boolean behind, float distancePower) {

		public static StrokeStyle NONE = new StrokeStyle(0, 0f, false, 1f);

		public StrokeStyle multiplyAlpha(float factor) {
			return new StrokeStyle(ColorPacker.multiplyAlpha(color, factor), width, behind, distancePower);
		}
	}

	public record Shadowed(Vk2dTextStyle mainStyle, Vk2dTextStyle shadowStyle, float shadowOffset) {}
}
