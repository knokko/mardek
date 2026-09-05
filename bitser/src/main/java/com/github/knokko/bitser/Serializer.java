package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.distributions.FloatDistributionTracker;
import com.github.knokko.bitser.distributions.IntegerDistributionTracker;
import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;
import java.util.Map;

class Serializer {

	final Bitser bitser;
	final BitserCache cache;
	final Object[] withAndOptions;
	final Map<String, Object> withParameters;
	final BitOutputStream output;
	final boolean backwardCompatible;

	final ArrayList<WriteStructJob> structJobs = new ArrayList<>();
	final ArrayList<WriteArrayJob> arrayJobs = new ArrayList<>();
	final ArrayList<WriteStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<WriteArrayReferenceJob> arrayReferenceJobs = new ArrayList<>();
	final ArrayList<WriteLazyJob> lazyJobs = new ArrayList<>();

	final ReferenceTracker references;

	final boolean forbidLazySaving;
	final IntegerDistributionTracker intDistribution;
	final FloatDistributionTracker floatDistribution;

	Serializer(
			Bitser bitser, Object[] withAndOptions,
			Map<String, Object> withParameters,
			BitOutputStream output, boolean backwardCompatible,
			Object rootStruct, boolean forbidLazySaving,
			IntegerDistributionTracker intDistribution, FloatDistributionTracker floatDistribution
	) {
		this.bitser = bitser;
		this.cache = bitser.cache;
		this.withAndOptions = withAndOptions;
		this.withParameters = withParameters;
		this.output = output;
		this.backwardCompatible = backwardCompatible;
		BitStructWrapper<?> rootStructInfo = cache.getWrapper(rootStruct.getClass());
		this.structJobs.add(new WriteStructJob(
				rootStruct, rootStructInfo,
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));
		this.references = new ReferenceTracker(cache);

		this.forbidLazySaving = forbidLazySaving;
		this.intDistribution = intDistribution;
		this.floatDistribution = floatDistribution;
	}

	void run() {
		// Stage 1
		output.setMarker("stage 1: struct jobs & array jobs");
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				WriteStructJob job = structJobs.remove(structJobs.size() - 1);
				output.pushContext(job.node(), null);
				job.write(this);
				output.popContext(job.node(), null);
			}
			if (!arrayJobs.isEmpty()) {
				WriteArrayJob job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					output.pushContext(job.node(), null);
					job.write(this);
					output.popContext(job.node(), null);
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace(null), failed);
				}
			}
		}

		// Stage 2
		output.setMarker("stage 2: with jobs");
		references.handleWithJobs(new FunctionContext(bitser, backwardCompatible, withParameters));

		// Stage 3
		output.setMarker("stage 3: map stable IDs");
		references.mapStableIDs();

		// Stage 4
		output.setMarker("stage 4: reference jobs & lazy reference jobs");
		for (WriteStructReferenceJob referenceJob : structReferenceJobs) {
			try {
				output.pushContext(referenceJob.node(), null);
				referenceJob.save(this);
				output.popContext(referenceJob.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace(null), failed);
			}
		}
		structReferenceJobs.clear();

		for (WriteArrayReferenceJob referenceJob : arrayReferenceJobs) {
			try {
				output.pushContext(referenceJob.node(), null);
				referenceJob.save(this);
				output.popContext(referenceJob.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace(null), failed);
			}
		}
		arrayReferenceJobs.clear();

		for (var job : lazyJobs) {
			try {
				output.pushContext(job.node(), null);
				job.save(this);
				output.popContext(job.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(job.node().generateTrace(null), failed);
			}
		}
		lazyJobs.clear();

		// Stages 5 and later are only needed during DEserialization
	}
}
