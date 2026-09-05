package com.github.knokko.bitser.connection;

//import com.github.knokko.bitser.BitEnum;
//import com.github.knokko.bitser.BitStruct;
//import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
//import com.github.knokko.bitser.field.BitField;
//import com.github.knokko.bitser.field.IntegerField;
//import com.github.knokko.bitser.io.BitInputStream;
//import com.github.knokko.bitser.Bitser;
//import com.github.knokko.bitser.wrapper.BitFieldWrapper;
//import com.github.knokko.bitser.wrapper.StructFieldWrapper;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Consumer;

public class BitListConnection<T> extends BitConnection {

//	private final Bitser bitser;
//	public final List<T> list;
//	private final List<T> myList;
//	private final List<BitConnection> connectionList;
//	private final BitFieldWrapper elementWrapper;
//	private final Consumer<BitStructConnection.ChangeListener> reportChanges;
//
//	public BitListConnection(
//			Bitser bitser, List<T> list, BitFieldWrapper elementWrapper,
//			Consumer<BitStructConnection.ChangeListener> reportChanges
//	) {
//		this.bitser = bitser;
//		this.list = list;
//		this.myList = new ArrayList<>(list);
//		this.elementWrapper = elementWrapper;
//		this.reportChanges = reportChanges;
//
//		if (bitser.needsChildConnection(elementWrapper)) {
//			connectionList = new ArrayList<>(myList.size());
//			for (T element : myList) connectionList.add(createChildConnection(element));
//		} else connectionList = null;
//	}
//
//	private BitConnection createChildConnection(T element) {
//		return bitser.createChildConnection(
//				element, elementWrapper, listener -> reportNestedChanges(listener, element)
//		);
//	}
//
//	private void reportNestedChanges(BitStructConnection.ChangeListener listener, T element) {
//		synchronized (list) {
//			checkForChanges();
//			int index = myList.indexOf(element);
//			postModification(new Modification<>(element, index, listener));
//		}
//	}
//
//	private synchronized void postModification(Modification<T> modification) {
//		reportChanges.accept(output -> {
//			bitser.serialize(modification, output);
//			if (modification.changeListener != null) {
//				modification.changeListener.report(output);
//			} else if (modification.action != Action.REMOVE) {
////				AbstractCollectionFieldWrapper.writeElement(
////						modification.element, elementWrapper,
////						new WriteJob(bitser, output, null, new HashMap<>(), null),
////						"This BitListConnection must not contain null values"
////				);
//			}
//			return 1;
//		});
//	}
//
//	public void addDelayed(T element) {
//		postModification(new Modification<>(element, -1, Action.ADD));
//	}
//
//	public void addDelayed(int index, T element) {
//		postModification(new Modification<>(element, index, Action.ADD));
//	}
//
//	public void replaceDelayed(int index, T element) {
//		postModification(new Modification<>(element, index, Action.REPLACE));
//	}
//
//	public void removeDelayed(int index) {
//		postModification(new Modification<>(null, index, Action.REMOVE));
//	}
//
//	@Override
//	public void checkForChanges() {
//		synchronized (list) {
//			if (list.size() != myList.size()) throw new UnexpectedBitserException("Detected external modification");
//			if (connectionList != null && connectionList.size() != myList.size()) throw new UnexpectedBitserException("Should not happen");
//		}
//	}
//
//	@Override
//	public void handleChanges(BitInputStream input) throws IOException {
//		checkForChanges();
//		Modification<?> modification = bitser.deserialize(Modification.class, input);
//		if (modification.action == Action.EDIT) {
//			synchronized (list) {
//				if (modification.index < myList.size()) {
//					connectionList.get(modification.index).handleChanges(input);
//				} else {
//					bitser.createChildConnection(null, elementWrapper, null).handleChanges(input);
//				}
//			}
//		} else if (modification.action == Action.REMOVE) {
//			synchronized (list) {
//				if (modification.index < myList.size()) {
//					myList.remove(modification.index);
//					list.remove(modification.index);
//					if (connectionList != null) connectionList.remove(modification.index);
//				}
//			}
//		}
////		else {
////			elementWrapper.read(new ReadJob(bitser, input, null, new HashMap<>(), false), rawElement -> {
////				@SuppressWarnings("unchecked") T element = (T) rawElement;
////				synchronized (list) {
////					if (modification.action == Action.ADD) {
////						if (modification.index >= 0) {
////							list.add(modification.index, element);
////							myList.add(modification.index, element);
////							if (connectionList != null) {
////								connectionList.add(modification.index, createChildConnection(element));
////							}
////						} else {
////							list.add(element);
////							myList.add(element);
////							if (connectionList != null) {
////								connectionList.add(createChildConnection(element));
////							}
////						}
////					} else if (modification.action == Action.REPLACE) {
////						if (modification.index < myList.size()) {
////							myList.set(modification.index, element);
////							list.set(modification.index, element);
////							if (connectionList != null) {
////								connectionList.set(modification.index, createChildConnection(element));
////							}
////						}
////					} else throw new UnexpectedBitserException("Unexpected action " + modification.action);
////				}
////			});
////		}
//	}
//
//	@Override
//	Object getState() {
//		return list;
//	}
//
//	public <C> BitStructConnection<C> getChildStruct(int index) {
//		if (connectionList == null || !(elementWrapper instanceof StructFieldWrapper)) {
//			throw new UnexpectedBitserException("This is not a list of structs");
//		}
//		//noinspection unchecked
//		return (BitStructConnection<C>) connectionList.get(index);
//	}
//
//	public <C> BitListConnection<C> getChildList(int index) {
//		if (connectionList == null || !List.class.isAssignableFrom(elementWrapper.field.type)) {
//			throw new UnexpectedBitserException("This is not a nested list");
//		}
//		//noinspection unchecked
//		return (BitListConnection<C>) connectionList.get(index);
//	}
//
//	@BitEnum(mode = BitEnum.Mode.Ordinal)
//	private enum Action {
//		ADD,
//		REPLACE,
//		REMOVE,
//		EDIT
//	}
//
//	@BitStruct(backwardCompatible = false)
//	private static class Modification<T> {
//
//		// This is not an @BitField because we don't know its type
//		final T element;
//		final BitStructConnection.ChangeListener changeListener;
//
//		@BitField
//		@IntegerField(expectUniform = false, minValue = -1)
//		final int index;
//
//		@BitField
//		final Action action;
//
//		Modification(T element, int index, Action action) {
//			this.element = element;
//			this.index = index;
//			this.action = action;
//			this.changeListener = null;
//		}
//
//		Modification(T element, int index, BitStructConnection.ChangeListener changeListener) {
//			this.element = element;
//			this.index = index;
//			this.action = Action.EDIT;
//			this.changeListener = changeListener;
//		}
//
//		@SuppressWarnings("unused")
//		Modification() {
//			this(null, 0, Action.ADD);
//		}
//	}
}
