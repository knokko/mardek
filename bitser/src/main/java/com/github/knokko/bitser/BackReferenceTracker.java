package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.legacy.LegacyReference;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.legacy.WithReference;

import java.util.*;

class BackReferenceTracker extends AbstractReferenceTracker {

	private final HashMap<String, LabelTargets> labels = new HashMap<>();
	private final IdentityHashMap<Object, Object> legacyToModern = new IdentityHashMap<>();

	BackReferenceTracker(BitserCache cache) {
		super(cache);
	}

	@Override
	void registerTarget(String label, Object target) {
		labels.computeIfAbsent(label, LabelTargets::new).registerWith(target);
	}

	@Override
	LazyReferenceTargets getLazyTargets(String label) {
		var targets = labels.get(label);
		if (targets == null) return null;
		if (targets.lazy == null) {
			targets.lazy = new LazyReferenceTargets(label, targets.idsToLegacyOrWith, new HashMap<>());
		}
		return targets.lazy;
	}

	void setLazyTargets(List<LazyReferenceTargets> targetsList) {
		for (var targets : targetsList) {
			if (targets == null) continue;
			var labelTargets = labels.computeIfAbsent(targets.label, LabelTargets::new);
			targets.idsToUnstable.forEach(labelTargets::registerWith);
		}
	}

	void setWithObjects(List<Object> withObjects) {
		for (Object withObject : withObjects) {
			BitStructWrapper<?> structInfo = cache.getWrapper(withObject.getClass());
			RecursionNode node = new RecursionNode("with " + withObject.getClass().getSimpleName());
			structJobs.add(new WithStructJob(withObject, structInfo, node));
		}
	}

	void registerLegacyTarget(String label, Object target) {
		labels.computeIfAbsent(label, LabelTargets::new).registerLegacy(target);
	}

	void handleWithJobs(FunctionContext functionContext) {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				structJobs.remove(structJobs.size() - 1).register(this, functionContext);
			}
			if (!arrayJobs.isEmpty()) arrayJobs.remove(arrayJobs.size() - 1).register(this);
		}
	}

	void registerModern(Object legacyTarget, Object modernTarget) {
		if (legacyToModern.put(legacyTarget, modernTarget) != null) {
			throw new LegacyBitserException("Multiple legacy reference targets have identity " + legacyTarget);
		}
	}

	void mapStableIDs() {
		for (LabelTargets targets : labels.values()) {
			for (Object rawTarget : targets.idsToLegacyOrWith) {
				UUID stableID = null;
				if (rawTarget instanceof LegacyReference legacyTarget) {
					Object targetObject = legacyTarget.reference();
					if (targetObject instanceof LegacyStructInstance targetStruct) stableID = targetStruct.stableID;
				} else if (rawTarget instanceof WithReference withTarget) {
					Object withObject = withTarget.reference();
					BitStructWrapper<?> maybeWrapper = cache.getWrapperOrNull(withObject.getClass());
					if (maybeWrapper != null && maybeWrapper.hasStableId()) {
						stableID = maybeWrapper.getStableId(withObject);
					}
				} else throw new UnexpectedBitserException("Unexpected reference target " + rawTarget);

				if (stableID != null) {
					if (targets.stable.put(stableID, rawTarget) != null) {
						throw new ReferenceBitserException("Multiple legacy stable targets have ID " + stableID);
					}
				}
			}
		}
	}

	Object getModernFromRaw(Object rawLegacyReference) {
		if (rawLegacyReference instanceof LegacyReference legacyReference) {
			Object inner = legacyReference.reference();
			Object modernReference = legacyToModern.get(inner);
			if (modernReference == null) throw new LegacyBitserException(
					"Reference target for " + inner + " no longer exists"
			);
			return modernReference;
		} else if (rawLegacyReference instanceof WithReference withReference) {
			return withReference.reference();
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + rawLegacyReference + " to reference");
		}
	}

	Object getWithOrLegacy(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
		LabelTargets targets = labels.get(referenceWrapper.label);
		if (targets == null) {
			throw new ReferenceBitserException(
					"Can't find legacy @ReferenceFieldTarget with label " + referenceWrapper.label
			);
		}
		return targets.getWithOrLegacy(referenceWrapper, input);
	}

	static class LabelTargets {

		private final String myLabel;

		private final HashMap<UUID, Object> stable = new HashMap<>();
		final IdentityHashMap<Object, Integer> legacyOrWithToIDs = new IdentityHashMap<>();
		final ArrayList<Object> idsToLegacyOrWith = new ArrayList<>();

		LazyReferenceTargets lazy;

		LabelTargets(String label) {
			this.myLabel = label;
		}

		void registerLegacy(Object target) {
			if (legacyOrWithToIDs.put(target, legacyOrWithToIDs.size()) != null) {
				throw new ReferenceBitserException("Multiple legacy unstable targets have identity " + target);
			}
			idsToLegacyOrWith.add(new LegacyReference(target));
		}

		void registerWith(Object target) {
			if (legacyOrWithToIDs.put(target, legacyOrWithToIDs.size()) != null) {
				throw new ReferenceBitserException("Multiple legacy unstable targets have identity " + target);
			}
			idsToLegacyOrWith.add(new WithReference(target));
		}

		Object getWithOrLegacy(ReferenceFieldWrapper referenceWrapper, BitInputStream input) throws Throwable {
			if (referenceWrapper.stable) return getStable(input);
			else return getUnstable(input);
		}

		private Object getStable(BitInputStream input) throws Throwable {
			input.prepareProperty("legacy-stable-id");
			UUID id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			input.finishProperty();
			Object value = stable.get(id);
			if (value == null) {
				throw new ReferenceBitserException(
						"Can't find legacy stable reference target with label " + myLabel + " and ID " + id
				);
			}
			return value;
		}

		private Object getUnstable(BitInputStream input) throws Throwable {
			input.prepareProperty("legacy-unstable-id");
			int index = (int) IntegerBitser.decodeUniformInteger(0, legacyOrWithToIDs.size() - 1, input);
			input.finishProperty();
			return idsToLegacyOrWith.get(index);
		}
	}
}
