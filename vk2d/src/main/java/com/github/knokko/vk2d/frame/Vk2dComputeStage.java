package com.github.knokko.vk2d.frame;

import com.github.knokko.boiler.commands.CommandRecorder;

public abstract class Vk2dComputeStage extends Vk2dStage {

	public abstract void record(CommandRecorder recorder);
}
