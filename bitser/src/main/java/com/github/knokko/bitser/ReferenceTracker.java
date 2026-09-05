package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.util.*;

class ReferenceTracker extends AbstractReferenceTracker {

	private final HashMap<String, LabelTargets> labels = new HashMap<>();

	ReferenceTracker(BitserCache cache) {
		super(cache);
	}

	@Override
	void registerTarget(String label, Object target) {
		LabelTargets entries = labels.computeIfAbsent(label, key -> new LabelTargets(key, cache));
		entries.register(target);
	}

	@Override
	LazyReferenceTargets getLazyTargets(String label) {
		var targets = labels.get(label);
		if (targets == null) return null;
		return new LazyReferenceTargets(label, targets.idsToUnstable, targets.stable);
	}

	void setLazyTargets(List<LazyReferenceTargets> targetsList) {
		for (var targets : targetsList) {
			if (targets == null) continue;
			var labelTargets = labels.computeIfAbsent(targets.label, key -> new LabelTargets(key, cache));
			targets.stable.values().forEach(stableTarget -> {
				if (!labelTargets.potentialStableSet.add(stableTarget)) {
					throw new ReferenceBitserException("Multiple stable targets have identity " + stableTarget);
				}
			});
			targets.idsToUnstable.forEach(labelTargets::registerUnstable);
		}
	}

	void setWithObjects(List<Object> withObjects) {
		for (Object withObject : withObjects) {
			BitStructWrapper<?> structInfo = cache.getWrapper(withObject.getClass());
			RecursionNode node = new RecursionNode("with " + withObject.getClass().getSimpleName());
			structJobs.add(new WithStructJob(withObject, structInfo, node));
		}
	}

	void handleWithJobs(FunctionContext functionContext) {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				structJobs.remove(structJobs.size() - 1).register(this, functionContext);
			}
			if (!arrayJobs.isEmpty()) arrayJobs.remove(arrayJobs.size() - 1).register(this);
		}
	}

	void mapStableIDs() {
		for (LabelTargets targets : labels.values()) {
			for (Object target : targets.potentialStableSet) targets.registerStable(target);
			targets.potentialStableSet.clear();
		}
	}

	Object get(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
		return get(referenceWrapper).get(referenceWrapper, input);
	}

	void save(ReferenceFieldWrapper referenceWrapper, Object reference, BitOutputStream output) throws Throwable {
		get(referenceWrapper).save(referenceWrapper, reference, output);
	}

	private LabelTargets get(ReferenceFieldWrapper referenceWrapper) {
		if (referenceWrapper.stable) cache.requireStableID(referenceWrapper.field.type);

		LabelTargets targets = labels.get(referenceWrapper.label);
		if (targets == null) {
			throw new ReferenceBitserException("Can't find @ReferenceFieldTarget with label " + referenceWrapper.label);
		}
		return targets;
	}

	static class LabelTargets {

		final IdentityHashMap<Object, Integer> unstableToIDs = new IdentityHashMap<>();
		final ArrayList<Object> idsToUnstable = new ArrayList<>();
		final Set<Object> potentialStableSet = Collections.newSetFromMap(new IdentityHashMap<>());
		final HashMap<UUID, Object> stable = new HashMap<>();

		final String myLabel;
		final BitserCache cache;

		private LabelTargets(String myLabel, BitserCache cache) {
			this.myLabel = myLabel;
			this.cache = cache;
		}

		private void registerUnstable(Object target) {
			if (unstableToIDs.put(target, unstableToIDs.size()) != null) {
				throw new ReferenceBitserException("Multiple unstable targets have identity " + target);
			}
			idsToUnstable.add(target);
		}

		private void registerStable(Object target) {
			BitStructWrapper<?> maybeWrapper = Objects.requireNonNull(cache.getWrapperOrNull(target.getClass()));
			UUID id = maybeWrapper.getStableId(target);
			Object withSameID = stable.put(id, target);
			if (withSameID != null) {
				throw new ReferenceBitserException(
						"Multiple stable targets have ID " + id + ": " + target + " and " + withSameID
				);
			}
		}

		void register(Object target) {
			BitStructWrapper<?> maybeWrapper = cache.getWrapperOrNull(target.getClass());
			if (maybeWrapper != null && maybeWrapper.hasStableId()) {
				if (!potentialStableSet.add(target)) {
					throw new ReferenceBitserException("Multiple stable targets have identity " + target);
				}
			}
			registerUnstable(target);
		}

		void save(ReferenceFieldWrapper referenceWrapper, Object reference, BitOutputStream output) throws Throwable {
			if (referenceWrapper.stable) {
				BitStructWrapper<?> valueInfo = cache.getWrapperOrNull(reference.getClass());
				UUID id = valueInfo.getStableId(reference);
				output.prepareProperty("stable-id");
				IntegerBitser.encodeFullLong(id.getMostSignificantBits(), output);
				IntegerBitser.encodeFullLong(id.getLeastSignificantBits(), output);
				output.finishProperty();

				Object expectedTarget = stable.get(id);
				if (expectedTarget == null) {
					throw new ReferenceBitserException("Can't find stable reference target with ID " + id + " and label " + myLabel);
				}
				if (expectedTarget != reference) {
					throw new ReferenceBitserException("Expected reference with ID " + id + " to be " + expectedTarget + ", but was " + reference);
				}
			} else {
				Integer index = unstableToIDs.get(reference);
				if (index == null) {
					throw new ReferenceBitserException("Can't find unstable reference target " + reference + " with label " + myLabel);
				}
				output.prepareProperty("unstable-id");
				IntegerBitser.encodeUniformInteger(index, 0, unstableToIDs.size() - 1, output);
				output.finishProperty();
			}
		}

		Object get(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
			if (referenceWrapper.stable) return getStable(input);
			else return getUnstable(input);
		}

		private Object getStable(BitInputStream input) throws Throwable {
			input.prepareProperty("stable-id");
			UUID id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			input.finishProperty();
			Object value = stable.get(id);
			if (value == null) {
				throw new ReferenceBitserException("Can't find stable reference target with label " + myLabel + " and ID " + id);
			}
			return value;
		}

		private Object getUnstable(BitInputStream input) throws Throwable {
			input.prepareProperty("unstable-id");
			int index = (int) IntegerBitser.decodeUniformInteger(0, idsToUnstable.size() - 1, input);
			input.finishProperty();
			return idsToUnstable.get(index);
		}
	}
}
