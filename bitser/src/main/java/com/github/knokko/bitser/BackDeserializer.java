package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;
import java.util.Map;

class BackDeserializer {

	final Bitser bitser;
	final BitserCache cache;
	final BitInputStream input;
	final LegacyClasses legacy;
	final CollectionSizeLimit sizeLimit;
	final Map<String, Object> withParameters;
	final Object rootStruct;

	final ArrayList<BackReadStructJob> structJobs = new ArrayList<>();
	final ArrayList<BackReadArrayJob> arrayJobs = new ArrayList<>();
	final ArrayList<BackReadStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<BackReadArrayReferenceJob> arrayReferenceJobs = new ArrayList<>();
	final ArrayList<ReadLazyJob> lazyJobs = new ArrayList<>();
	final ArrayList<BackConvertStructJob> convertStructJobs = new ArrayList<>();
	final ArrayList<BackConvertArrayJob> convertArrayJobs = new ArrayList<>();
	final ArrayList<BackConvertLazyToArrayJob> convertLazyToArrayJobs = new ArrayList<>();
	final ArrayList<BackConvertStructReferenceJob> convertStructReferenceJobs = new ArrayList<>();
	final ArrayList<BackConvertArrayReferenceJob> convertArrayReferenceJobs = new ArrayList<>();
	final ArrayList<BackPopulateStructJob> populateStructJobs = new ArrayList<>();
	final ArrayList<PopulateJob> populateJobs = new ArrayList<>();
	final ArrayList<PostInitJob> postInitJobs = new ArrayList<>();

	final BackReferenceTracker references;

	BackDeserializer(
			Bitser bitser, BitInputStream input, LegacyClasses legacy,
			CollectionSizeLimit sizeLimit,
			Map<String, Object> withParameters,
			BitStructWrapper<?> rootStructInfo
	) {
		this.bitser = bitser;
		this.cache = bitser.cache;
		this.input = input;
		this.legacy = legacy;
		this.sizeLimit = sizeLimit;
		this.withParameters = withParameters;
		this.rootStruct = rootStructInfo.createEmptyInstance();

		LegacyStructInstance legacyRootStruct = legacy.getRoot().constructEmptyInstance(0);
		this.structJobs.add(new BackReadStructJob(
				legacyRootStruct, legacy.getRoot(), rootStructInfo.constructor.getDeclaringClass(),
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));
		legacyRootStruct.modernObject = rootStruct;
		this.convertStructJobs.add(new BackConvertStructJob(
				rootStructInfo, legacyRootStruct,
				new RecursionNode(rootStructInfo.constructor.getDeclaringClass().getSimpleName())
		));

		this.references = new BackReferenceTracker(cache);
	}

	void run() {
		input.setMarker("stage 1: struct jobs & array jobs");
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				BackReadStructJob structJob = structJobs.remove(structJobs.size() - 1);
				input.pushContext(structJob.node(), null);
				structJob.read(this);
				input.popContext(structJob.node(), null);
			}
			if (!arrayJobs.isEmpty()) {
				BackReadArrayJob job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					input.pushContext(job.node(), null);
					job.read(this);
					input.popContext(job.node(), null);
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace(null), failed);
				}
			}
		}

		input.setMarker("stage 2: with jobs");
		references.handleWithJobs(new FunctionContext(bitser, true, withParameters));

		input.setMarker("stage 3: map stable IDs");
		references.mapStableIDs();

		input.setMarker("stage 4: reference jobs & lazy reference jobs");
		for (BackReadStructReferenceJob referenceJob : structReferenceJobs) {
			try {
				input.pushContext(referenceJob.node(), null);
				referenceJob.resolve(this);
				input.popContext(referenceJob.node(), null);
			} catch (Throwable failed) {
				String topContext = "field/function " + referenceJob.legacyValuesIndex();
				throw new RecursionException(referenceJob.node().generateTrace(topContext), failed);
			}
		}
		structReferenceJobs.clear();

		for (BackReadArrayReferenceJob referenceJob : arrayReferenceJobs) {
			try {
				input.pushContext(referenceJob.node(), null);
				referenceJob.resolve(this);
				input.popContext(referenceJob.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace("elements"), failed);
			}
		}
		arrayReferenceJobs.clear();

		for (var job : lazyJobs) {
			try {
				input.pushContext(job.node(), null);
				job.read(input, sizeLimit, references);
				input.popContext(job.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(job.node().generateTrace(null), failed);
			}
		}

		input.setMarker("stage 5: convert struct & array jobs");
		while (!convertStructJobs.isEmpty() || !convertArrayJobs.isEmpty()) {
			if (!convertStructJobs.isEmpty()) {
				BackConvertStructJob job = convertStructJobs.remove(convertStructJobs.size() - 1);
				input.pushContext(job.node(), null);
				job.convert(this);
				input.popContext(job.node(), null);
			}
			if (!convertArrayJobs.isEmpty()) {
				BackConvertArrayJob job = convertArrayJobs.remove(convertArrayJobs.size() - 1);
				try {
					input.pushContext(job.node(), null);
					job.convert(this);
					input.popContext(job.node(), null);
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace("elements"), failed);
				}
			}
		}

		input.setMarker("stage 6: convert reference jobs & convert lazy jobs");
		for (BackConvertStructReferenceJob job : convertStructReferenceJobs) {
			try {
				input.pushContext(job.node(), null);
				job.convert(this);
				input.popContext(job.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(job.node().generateTrace(null), failed);
			}
		}
		convertStructReferenceJobs.clear();

		for (BackConvertArrayReferenceJob job : convertArrayReferenceJobs) {
			try {
				input.pushContext(job.node(), null);
				job.convert(this);
				input.popContext(job.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(job.node().generateTrace(null), failed);
			}
		}
		convertArrayReferenceJobs.clear();

		for (var job : lazyJobs) {
			try {
				input.pushContext(job.node(), null);
				job.convert(this);
				input.popContext(job.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(job.node().generateTrace(null), failed);
			}
		}
		lazyJobs.clear();

		input.setMarker("stage 7: populate struct jobs");
		for (var populateJob : populateStructJobs) populateJob.populate();
		populateStructJobs.clear();

		input.setMarker("stage 8: evaluate lazy jobs");
		for (var job : convertLazyToArrayJobs) {
			try {
				input.pushContext(job.node(), null);
				job.evaluate(this);
				input.popContext(job.node(), null);
			} catch (Throwable failed) {
				throw new RecursionException(job.node().generateTrace(null), failed);
			}
		}
		convertLazyToArrayJobs.clear();

		input.setMarker("stage 9 to 11: collection population & post init");
		Populator.collectionsAndPostInit(populateJobs, postInitJobs);
	}
}
