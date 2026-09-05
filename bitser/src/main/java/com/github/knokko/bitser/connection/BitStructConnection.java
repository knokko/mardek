package com.github.knokko.bitser.connection;

//import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
//import com.github.knokko.bitser.io.BitInputStream;
//import com.github.knokko.bitser.io.BitOutputStream;
//import com.github.knokko.bitser.Bitser;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.Consumer;
//
//import static com.github.knokko.bitser.IntegerBitser.*;

public class BitStructConnection<T> extends BitConnection {

//	private final Bitser bitser;
//	private final List<BitFieldWrapper> fields;
//	private final Map<String, BitFieldWrapper> nameToChildMapping;
//	private T referenceState;
//	public final T state;
//	private final Consumer<ChangeListener> reportChanges;
//	private final BitConnection[] childConnections;
//
//	public BitStructConnection(
//			Bitser bitser, List<BitFieldWrapper> fields, Map<String, BitFieldWrapper> nameToChildMapping,
//			T state, Consumer<ChangeListener> reportChanges) {
//		this.bitser = bitser;
//		this.fields = fields;
//		this.nameToChildMapping = nameToChildMapping;
//		this.referenceState = state != null ? bitser.shallowCopy(state) : null;
//		this.state = state;
//		this.reportChanges = reportChanges;
//		this.childConnections = new BitConnection[fields.size()];
//		for (int index = 0; index < fields.size(); index++) updateChildConnection(fields.get(index), index);
//	}
//
//	@Override
//	public void checkForChanges() {
//		synchronized (getLock()) {
//			try {
//				if (findAndWriteChanges(null) == 0) return;
//			} catch (IOException shouldNotHappen) {
//				throw new UnexpectedBitserException(shouldNotHappen.getMessage());
//			}
//			reportChanges.accept(output -> {
//				output.write(false);
//				return findAndWriteChanges(output);
//			});
//			assert state != null;
//			referenceState = bitser.shallowCopy(state);
//		}
//	}
//
//	private Object getLock() {
//		return state != null ? state : this;
//	}
//
//	@Override
//	public void handleChanges(BitInputStream input) throws IOException {
//		synchronized (getLock()) {
//			if (input.read()) {
//				int index = (int) decodeUniformInteger(0, fields.size() - 1, input);
//				BitFieldWrapper field = fields.get(index);
//				updateChildConnection(field, index);
//				if (childConnections[index] != null) childConnections[index].handleChanges(input);
//				else bitser.createChildConnection(field.field.getValue.apply(state), field, null).handleChanges(input);
//			} else {
////				for (BitFieldWrapper fieldWrapper : fields) {
////					if (input.read()) {
////						fieldWrapper.read(new ReadJob(bitser, input, null, new HashMap<>(), false),value -> {
////							if (state != null) fieldWrapper.field.setValue.accept(state, value);
////						});
////					}
////				}
//				referenceState = state != null ? bitser.shallowCopy(state) : null;
//			}
//		}
//	}
//
//	@Override
//	Object getState() {
//		return state;
//	}
//
//	private void writeNestedChange(Object actualChild, int index, ChangeListener listener) {
//		synchronized (getLock()) {
//			BitConnection childConnection = updateChildConnection(fields.get(index), index);
//			if (childConnection == null || childConnection.getState() != actualChild) return;
//			reportChanges.accept(output -> {
//				output.write(true);
//				encodeUniformInteger(index, 0, fields.size() - 1, output);
//				return listener.report(output);
//			});
//		}
//	}
//
//	private int findAndWriteChanges(BitOutputStream output) throws IOException {
//		int numChanges = 0;
//		for (int index = 0; index < fields.size(); index++) {
//			BitFieldWrapper fieldWrapper = fields.get(index);
//			Class<?> fieldType = fieldWrapper.field.type;
//			Object originalValue = fieldWrapper.field.getValue.apply(referenceState);
//			Object modifiedValue = fieldWrapper.field.getValue.apply(state);
//
//			boolean hasChanged;
//			if (fieldType.isPrimitive() || Number.class.isAssignableFrom(fieldType) || fieldType == Boolean.class) {
//				hasChanged = !Objects.equals(modifiedValue, fieldWrapper.field.getValue.apply(referenceState));
//			} else {
//				hasChanged = modifiedValue != originalValue;
//			}
//
//			if (output != null) output.write(hasChanged);
//
//			if (hasChanged) {
//				numChanges += 1;
//
//				if (output != null) {
//					//fieldWrapper.write(state, new WriteJob(bitser, output, null, new HashMap<>(), null));
//					updateChildConnection(fieldWrapper, index);
//				}
//			}
//		}
//
//		return numChanges;
//	}
//
//	public <C> BitListConnection<C> getChildList(String fieldName) {
//		//noinspection unchecked
//		return (BitListConnection<C>) getChildConnection(fieldName);
//	}
//
//	public <C> BitStructConnection<C> getChildStruct(String fieldName) {
//		//noinspection unchecked
//		return (BitStructConnection<C>) getChildConnection(fieldName);
//	}
//
//	private BitConnection getChildConnection(String fieldName) {
//		BitFieldWrapper result = nameToChildMapping.get(fieldName);
//		if (result != null) return updateChildConnection(result, fields.indexOf(result));
//		throw new UnexpectedBitserException("Can't find field with name " + fieldName);
//	}
//
//	private BitConnection updateChildConnection(BitFieldWrapper fieldWrapper, int index) {
//		if (bitser.needsChildConnection(fieldWrapper)) {
//			synchronized (getLock()) {
//				BitConnection oldConnection = childConnections[index];
//				BitConnection newConnection = oldConnection;
//				Object currentState = state != null ? fieldWrapper.field.getValue.apply(state) : null;
//				if (currentState == null) {
//					newConnection = null;
//				} else if (oldConnection == null || oldConnection.getState() != currentState) {
//					newConnection = bitser.createChildConnection(
//							currentState, fieldWrapper, listener -> writeNestedChange(currentState, index, listener)
//					);
//				}
//				childConnections[index] = newConnection;
//				return newConnection;
//			}
//		} else return null;
//	}
//
//	@FunctionalInterface
//	public interface ChangeListener {
//		int report(BitOutputStream output) throws IOException;
//	}
}
