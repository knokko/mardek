package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.UnexpectedBitserException;

import java.util.*;

class Populator {

	private static void populateUntilStable(ArrayList<PopulateJob> criticalJobs) {
		while (true) {
			boolean changedAnything = false;
			for (PopulateJob job : criticalJobs) {
				if (job instanceof PopulateCollectionJob collectionJob) {
					int newSize = collectionJob.collection.size();
					collectionJob.elements = new Object[newSize];
					int elementIndex = 0;
					for (Object element : collectionJob.collection) {
						collectionJob.elements[elementIndex++] = element;
					}
					for (Object expectedElement : collectionJob.elements) {
						if (!collectionJob.collection.contains(expectedElement)) {
							changedAnything = true;
							break;
						}
					}
					if (changedAnything) {
						collectionJob.collection.clear();
						//noinspection unchecked
						Collections.addAll((Collection<Object>) collectionJob.collection, collectionJob.elements);
						break;
					}
				} else if (job instanceof PopulateMapJob mapJob) {
					int newSize = mapJob.map.size();
					mapJob.keys = new Object[newSize];
					mapJob.values = new Object[newSize];
					int entryIndex = 0;
					for (var entry : mapJob.map.entrySet()) {
						mapJob.keys[entryIndex] = entry.getKey();
						mapJob.values[entryIndex] = entry.getValue();
						entryIndex += 1;
					}
					for (int index = 0; index < mapJob.keys.length; index++) {
						if (mapJob.map.get(mapJob.keys[index]) != mapJob.values[index]) {
							changedAnything = true;
							break;
						}
					}
					if (changedAnything) {
						mapJob.map.clear();
						for (int index = 0; index < mapJob.keys.length; index++) {
							//noinspection unchecked
							((Map<Object, Object>) mapJob.map).put(mapJob.keys[index], mapJob.values[index]);
						}
						break;
					}
				} else {
					throw new UnexpectedBitserException("Unexpected populate job " + job);
				}
			}
			if (!changedAnything) break;
		}
	}

	/**
	 * <p>
	 *     This method is called by {@link Deserializer} and {@link BackDeserializer} to <i>populate</i> all collections
	 *     and maps, and to call the {@link BitPostInit#postInit} method on each post-init struct. This is more
	 *     complicated than one might expect, due to the following problem:
	 * </p>
	 *
	 * <p>
	 *     When we populate a {@link Set} or {@link Map}, the Set/Map implementation expects that we don't mutate the
	 *     Set elements or Map keys after we insert them (e.g. the result of {@link Object#hashCode} can change if we
	 *     mutate an object, which will cause problems if it was already inserted into a {@link HashSet}).
	 *     Unfortunately, we cannot guarantee that we don't mutate elements after inserting them. For instance, when
	 *     we deserialize a {@code HashSet<HashSet<SomeStruct>>}, we need to:
	 * </p>
	 *
	 * <ol>
	 *     <li>Initialize the fields of {@code SomeStruct}, at least the fields used for its {@code hashCode()}</li>
	 *     <li>Insert the {@code SomeStructs}s into the inner {@code HashSet}</li>
	 *     <li>Insert the inner {@code HashSet}s into the outer {@code HashSet}</li>
	 * </ol>
	 *
	 * <p>
	 *     This must happen <b>explicitly in that order</b>, or the hash codes will get messed up. In this example, we
	 *     could solve the problem by first populating all structs, and afterward populate the maps/sets in
	 *     <i>decreasing</i> order, sorted by their <i>depth</i>. However, even this strategy will not work when the
	 *     map keys or set elements are references to maps or sets with a lower depth.
	 * </p>
	 *
	 * <p>
	 *     Since this problem is quite complicated to solve, we simply <i>test</i> whether all set elements and map
	 *     keys are still working, and re-insert them if they are not. We repeat this process until everything works.
	 * </p>
	 *
	 * @param populateJobs All collection/map populate jobs
	 * @param postInitJobs All post-init jobs
	 */
	static void collectionsAndPostInit(ArrayList<PopulateJob> populateJobs, ArrayList<PostInitJob> postInitJobs) {
		ArrayList<PopulateJob> criticalJobs = new ArrayList<>(populateJobs.size());

		for (PopulateJob job : populateJobs) {
			job.populate();
			if (job instanceof PopulateCollectionJob) {
				if (((PopulateCollectionJob) job).collection instanceof List) continue;
			}
			criticalJobs.add(job);
		}
		populateJobs.clear();

		if (!postInitJobs.isEmpty()) {
			// Let's stabilize the Set's and Map's before invoking PostInit's
			populateUntilStable(criticalJobs);

			// Let's post-init the deepest structs first, since I think that makes most sense
			postInitJobs.sort(Comparator.comparingInt(job -> -job.node().depth));
			for (PostInitJob postInitJob : postInitJobs) {
				postInitJob.structObject().postInit(postInitJob.context());
			}
			postInitJobs.clear();
		}

		// If the post init jobs messed with the Set's or Map's, we need to stabilize them again
		populateUntilStable(criticalJobs);
	}
}
