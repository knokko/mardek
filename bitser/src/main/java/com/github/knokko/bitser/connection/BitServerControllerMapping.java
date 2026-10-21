package com.github.knokko.bitser.connection;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BitServerControllerMapping {

	record ViewPair(Object targetObject, BitConnectionView view) {}

	private final Map<BitServerController, Long> controllerToID = new IdentityHashMap<>();
	private final Map<Long, BitServerController> idToController = new HashMap<>();
	private final Map<Object, BitServerController> targetToController = new IdentityHashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private long nextID = 0L;

	public void addIfNotPresent(
			BitServerContext context,
			Collection<?> objects,
			List<? extends BitConnectionView> views
	) {
		lock.writeLock().lock();
		try {
			var remainingViews = new ArrayDeque<ViewPair>();

			for (var object : objects) {
				for (var view : views.reversed()) remainingViews.push(new ViewPair(object, view));
			}

			while (!remainingViews.isEmpty()) {
				var nextViewPair = remainingViews.pop();

				var controller = targetToController.get(nextViewPair.targetObject);
				if (controller == null) {
					controller = nextViewPair.view.initializeController(context, nextViewPair.targetObject);
					controllerToID.put(controller, nextID);
					idToController.put(nextID, controller);
					targetToController.put(controller.getTargetObject(), controller);
					nextID += 1;
				}

				boolean didModify = controller.addViewIfNotPresent(nextViewPair.view);
				if (didModify) {
					controller.getChildViews(remainingViews);
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public long getIdForController(BitServerController controller) {
		lock.readLock().lock();
		try {
			return controllerToID.get(controller);
		} finally {
			lock.readLock().unlock();
		}
	}

	public BitServerController getControllerById(long id) {
		lock.readLock().lock();
		try {
			return Objects.requireNonNull(idToController.get(id));
		} finally {
			lock.readLock().unlock();
		}
	}

	public BitServerController getControllerByTarget(Object target) {
		lock.readLock().lock();
		try {
			return Objects.requireNonNull(targetToController.get(target));
		} finally {
			lock.readLock().unlock();
		}
	}

	public <T> BitServerStructListController<T> getListController(List<T> target) {
		//noinspection unchecked
		return (BitServerStructListController<T>) getControllerByTarget(target);
	}
}
