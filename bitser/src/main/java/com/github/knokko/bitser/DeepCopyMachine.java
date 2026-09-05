package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;
import java.util.Map;

class DeepCopyMachine {

	final Bitser bitser;
	final Object destinationRoot;
	final Map<String, Object> withParameters;

	final DeepCopyReferences references = new DeepCopyReferences();
	final ArrayList<DeepCopyStructJob> structJobs = new ArrayList<>();
	final ArrayList<DeepCopyArrayJob> arrayJobs = new ArrayList<>();
	final ArrayList<DeepCopyStructReferenceJob> structReferenceJobs = new ArrayList<>();
	final ArrayList<DeepCopyArrayReferenceJob> arrayReferenceJobs = new ArrayList<>();
	final ArrayList<DeepCopyLazyJob> lazyJobs = new ArrayList<>();
	final ArrayList<PopulateJob> populateJobs = new ArrayList<>();
	final ArrayList<PostInitJob> postInitJobs = new ArrayList<>();

	DeepCopyMachine(Object sourceRoot, Bitser bitser, Map<String, Object> withParameters) {
		this.bitser = bitser;
		this.withParameters = withParameters;
		var rootWrapper = bitser.cache.getWrapper(sourceRoot.getClass());
		this.destinationRoot = rootWrapper.createEmptyInstance();
		structJobs.add(new DeepCopyStructJob(
				rootWrapper, sourceRoot, destinationRoot, new RecursionNode("(root)")
		));
	}

	void run() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				structJobs.remove(structJobs.size() - 1).copy(this);
			}
			if (!arrayJobs.isEmpty()) {
				var job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					job.copy(this);
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace(job.description()), failed);
				}
			}
		}

		for (var lazyJob : lazyJobs) {
			try {
				lazyJob.copy(this);
			} catch (Throwable failed) {
				throw new RecursionException(lazyJob.node().generateTrace(null), failed);
			}
		}
		lazyJobs.clear();

		for (var referenceJob : structReferenceJobs) {
			try {
				referenceJob.resolve(this);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace(referenceJob.classField().getName()), failed);
			}
		}
		structReferenceJobs.clear();

		for (var referenceJob : arrayReferenceJobs) {
			try {
				referenceJob.resolve(this);
			} catch (Throwable failed) {
				throw new RecursionException(referenceJob.node().generateTrace(null), failed);
			}
		}
		arrayReferenceJobs.clear();

		Populator.collectionsAndPostInit(populateJobs, postInitJobs);
	}
}
