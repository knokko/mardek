package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

class InstanceCollector {

	final Map<Class<?>, Collection<Object>> destination;
	final Map<Class<?>, Collection<Object>> referencesDestination;
	final Bitser bitser;
	final Map<String, Object> withObjects;

	final ArrayList<CollectFromStructJob> structJobs = new ArrayList<>();
	final ArrayList<CollectFromArrayJob> arrayJobs = new ArrayList<>();

	InstanceCollector(
			Object rootStruct, Map<Class<?>, Collection<Object>> destination,
			Map<Class<?>, Collection<Object>> referencesDestination,
			Bitser bitser, Map<String, Object> withObjects
	) {
		this.destination = destination;
		this.referencesDestination = referencesDestination;
		this.bitser = bitser;
		this.withObjects = withObjects;
		this.structJobs.add(new CollectFromStructJob(
				rootStruct, bitser.cache.getWrapper(rootStruct.getClass()), new RecursionNode("(root)")
		));
		register(rootStruct);
	}

	void run() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				structJobs.remove(structJobs.size() - 1).collect(this);
			}
			if (!arrayJobs.isEmpty()) {
				var job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					job.collect(this);
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace(null), failed);
				}
			}
		}
	}

	void register(Object value) {
		destination.forEach((candidateClass, instances) -> {
			if (candidateClass.isAssignableFrom(value.getClass())) {
				instances.add(value);
			}
		});
	}

	void registerReference(Object value) {
		referencesDestination.forEach((candidateClass, instances) -> {
			if (candidateClass.isAssignableFrom(value.getClass())) {
				instances.add(value);
			}
		});
	}
}
