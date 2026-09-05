package com.github.knokko.bitser.test.io;

import com.github.knokko.bitser.*;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.profiler.SampleProfiler;
import com.github.knokko.profiler.storage.FrequencyThreadStorage;
import com.github.knokko.profiler.storage.SampleStorage;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SerializeBenchmark {

	private static final Bitser BITSER = new Bitser();

	private static final IntegerField.Properties VARIABLE_INT = new IntegerField.Properties(
			Integer.MIN_VALUE, Integer.MAX_VALUE, false, 0, new long[0]
	);
	private static final IntegerField.Properties UNIFORM_INT = new IntegerField.Properties(
			Integer.MIN_VALUE, Integer.MAX_VALUE, true, 0, new long[0]
	);
	private static final FloatField.Properties FLOAT = new FloatField.Properties(
			0.0, 0.0, VARIABLE_INT, new double[0]
	);

	@BitStruct(backwardCompatible = true)
	static class RootStruct {

		@BitField(id = 0)
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		final HashMap<String, String> largeMap = new HashMap<>();

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "big")
		BigStruct[] bigBoys;

		void handwrittenSave1(DataOutputStream output) throws IOException {
			output.writeInt(largeMap.size());
			for (Map.Entry<String, String> entry : largeMap.entrySet()) {
				byte[] key = entry.getKey().getBytes(StandardCharsets.UTF_8);
				output.writeInt(key.length);
				output.write(key);
				byte[] value = entry.getValue().getBytes(StandardCharsets.UTF_8);
				output.writeInt(value.length);
				output.write(value);
			}
			output.writeInt(bigBoys.length);
			for (BigStruct big : bigBoys) big.handwrittenSave1(output);
			output.flush();
		}

		void handwrittenRead1(DataInputStream input) throws IOException {
			int largeSize = input.readInt();
			for (int counter = 0; counter < largeSize; counter++) {
				int keyLength = input.readInt();
				byte[] key = new byte[keyLength];
				input.readFully(key);
				int valueLength = input.readInt();
				byte[] value = new byte[valueLength];
				input.readFully(value);
				largeMap.put(new String(key, StandardCharsets.UTF_8), new String(value, StandardCharsets.UTF_8));
			}

			bigBoys = new BigStruct[input.readInt()];
			for (int index = 0; index < bigBoys.length; index++) {
				bigBoys[index] = new BigStruct();
				bigBoys[index].handwrittenRead1(input);
			}
		}

		void handwrittenSave2(BitOutputStream output) throws IOException {
			IntegerBitser.encodeInteger(largeMap.size(), VARIABLE_INT, output);
			for (Map.Entry<String, String> entry : largeMap.entrySet()) {
				StringBitser.encode(entry.getKey(), VARIABLE_INT, output);
				StringBitser.encode(entry.getValue(), VARIABLE_INT, output);
			}
			IntegerBitser.encodeInteger(bigBoys.length, VARIABLE_INT, output);
			for (BigStruct big : bigBoys) big.handwrittenSave2(output);
			output.finish();
		}

		void handwrittenRead2(BitInputStream input) throws IOException {
			int largeSize = (int) IntegerBitser.decodeInteger(VARIABLE_INT, input);
			for (int counter = 0; counter < largeSize; counter++) {
				largeMap.put(StringBitser.decode(VARIABLE_INT, null, input), StringBitser.decode(VARIABLE_INT, null, input));
			}
			bigBoys = new BigStruct[(int) IntegerBitser.decodeInteger(VARIABLE_INT, input)];
			for (int index = 0; index < bigBoys.length; index++) {
				bigBoys[index] = new BigStruct();
				bigBoys[index].handwrittenRead2(input);
			}
		}

		void handwrittenSave3(BitOutputStream output) throws IOException {
			IntegerBitser.encodeInteger(largeMap.size(), VARIABLE_INT, output);
			for (Map.Entry<String, String> entry : largeMap.entrySet()) {
				StringBitser.encode(entry.getKey(), VARIABLE_INT, output);
				StringBitser.encode(entry.getValue(), VARIABLE_INT, output);
			}
			IntegerBitser.encodeInteger(Array.getLength(bigBoys), VARIABLE_INT, output);
			for (int index = 0; index < bigBoys.length; index++) {
				((BigStruct) Array.get(bigBoys, index)).handwrittenSave3(output);
			}
			output.finish();
		}

		void handwrittenRead3(BitInputStream input) throws IOException {
			int largeSize = (int) IntegerBitser.decodeInteger(VARIABLE_INT, input);
			for (int counter = 0; counter < largeSize; counter++) {
				largeMap.put(StringBitser.decode(VARIABLE_INT, null, input), StringBitser.decode(VARIABLE_INT, null, input));
			}
			bigBoys = new BigStruct[(int) IntegerBitser.decodeInteger(VARIABLE_INT, input)];
			for (int index = 0; index < bigBoys.length; index++) {
				bigBoys[index] = new BigStruct();
				bigBoys[index].handwrittenRead3(input);
			}
		}
	}

	@BitStruct(backwardCompatible = true)
	static class BigStruct {

		@BitField(id = 0)
		@StableReferenceFieldId
		@SuppressWarnings("unused")
		UUID id = UUID.randomUUID();

		@BitField(id = 1)
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		final HashSet<SmallStruct> children = new HashSet<>();

		@BitField(id = 2, optional = true)
		@ReferenceField(stable = true, label = "big")
		BigStruct friend;

		void handwrittenSave1(DataOutputStream output) throws IOException {
			output.writeLong(id.getMostSignificantBits());
			output.writeLong(id.getLeastSignificantBits());
			output.writeInt(children.size());
			for (SmallStruct child : children) child.handwrittenSave1(output);
			output.writeBoolean(friend != null);
			if (friend != null) {
				output.writeLong(friend.id.getMostSignificantBits());
				output.writeLong(friend.id.getLeastSignificantBits());
			}
		}

		void handwrittenRead1(DataInputStream input) throws IOException {
			id = new UUID(input.readLong(), input.readLong());
			int numChildren = input.readInt();
			for (int counter = 0; counter < numChildren; counter++) {
				SmallStruct child = new SmallStruct();
				child.handwrittenRead1(input);
				children.add(child);
			}
			if (input.readBoolean()) {
				friend = new BigStruct();
				friend.id = new UUID(input.readLong(), input.readLong());
			}
		}

		void handwrittenSave2(BitOutputStream output) throws IOException {
			IntegerBitser.encodeFullLong(id.getMostSignificantBits(), output);
			IntegerBitser.encodeFullLong(id.getLeastSignificantBits(), output);
			IntegerBitser.encodeInteger(children.size(), VARIABLE_INT, output);
			for (SmallStruct child : children) child.handwrittenSave2(output);
			output.write(friend != null);
			if (friend != null) {
				IntegerBitser.encodeFullLong(friend.id.getMostSignificantBits(), output);
				IntegerBitser.encodeFullLong(friend.id.getLeastSignificantBits(), output);
			}
		}

		void handwrittenRead2(BitInputStream input) throws IOException {
			id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			int numChildren = (int) IntegerBitser.decodeInteger(VARIABLE_INT, input);
			for (int counter = 0; counter < numChildren; counter++) {
				SmallStruct child = new SmallStruct();
				child.handwrittenRead2(input);
				children.add(child);
			}
			if (input.read()) {
				friend = new BigStruct();
				friend.id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			}
		}

		void handwrittenSave3(BitOutputStream output) throws IOException {
			IntegerBitser.encodeFullLong(id.getMostSignificantBits(), output);
			IntegerBitser.encodeFullLong(id.getLeastSignificantBits(), output);
			IntegerBitser.encodeInteger(children.size(), VARIABLE_INT, output);
			for (SmallStruct child : children) child.handwrittenSave3(output);
			output.write(friend != null);
			if (friend != null) {
				IntegerBitser.encodeFullLong(friend.id.getMostSignificantBits(), output);
				IntegerBitser.encodeFullLong(friend.id.getLeastSignificantBits(), output);
			}
		}

		void handwrittenRead3(BitInputStream input) throws IOException {
			id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			int numChildren = (int) IntegerBitser.decodeInteger(VARIABLE_INT, input);
			for (int counter = 0; counter < numChildren; counter++) {
				SmallStruct child = new SmallStruct();
				child.handwrittenRead3(input);
				children.add(child);
			}
			if (input.read()) {
				friend = new BigStruct();
				friend.id = new UUID(IntegerBitser.decodeFullLong(input), IntegerBitser.decodeFullLong(input));
			}
		}
	}

	@BitStruct(backwardCompatible = true)
	static class SmallStruct {

		private static final Field fieldX, fieldY, fieldZ, fieldA, fieldB;

		static {
			try {
				fieldX = SmallStruct.class.getDeclaredField("x");
				fieldY = SmallStruct.class.getDeclaredField("y");
				fieldZ = SmallStruct.class.getDeclaredField("z");
				fieldA = SmallStruct.class.getDeclaredField("a");
				fieldB = SmallStruct.class.getDeclaredField("b");
			} catch (Exception failed) {
				throw new RuntimeException(failed);
			}
		}

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int x;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int y;

		@BitField(id = 2)
		@IntegerField(expectUniform = true)
		int z;

		@BitField(id = 3)
		@FloatField
		float a;

		@BitField(id = 4)
		@FloatField(expectMultipleOf = 0.5)
		float b;

		@SuppressWarnings("unused")
		private SmallStruct() {
			this(0, 0, 0, 0f, 0f);
		}

		SmallStruct(int x, int y, int z, float a, float b) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.a = a;
			this.b = b;
		}

		@Override
		public int hashCode() {
			return BITSER.hashCode(this);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof SmallStruct && BITSER.deepEquals(this, other);
		}

		void handwrittenSave1(DataOutputStream output) throws IOException {
			output.writeInt(x);
			output.writeInt(y);
			output.writeInt(z);
			output.writeFloat(a);
			output.writeFloat(b);
		}

		void handwrittenRead1(DataInputStream input) throws IOException {
			x = input.readInt();
			y = input.readInt();
			z = input.readInt();
			a = input.readFloat();
			b = input.readFloat();
		}

		void handwrittenSave2(BitOutputStream output) throws IOException {
			IntegerBitser.encodeInteger(x, VARIABLE_INT, output);
			IntegerBitser.encodeInteger(y, VARIABLE_INT, output);
			IntegerBitser.encodeInteger(z, UNIFORM_INT, output);
			FloatBitser.encodeFloat(a, false, FLOAT, output);
			FloatBitser.encodeFloat(b, false, FLOAT, output);
		}

		void handwrittenRead2(BitInputStream input) throws IOException {
			x = (int) IntegerBitser.decodeInteger(VARIABLE_INT, input);
			y = (int) IntegerBitser.decodeInteger(VARIABLE_INT, input);
			y = (int) IntegerBitser.decodeInteger(UNIFORM_INT, input);
			a = (float) FloatBitser.decodeFloat(false, FLOAT, input);
			b = (float) FloatBitser.decodeFloat(false, FLOAT, input);
		}

		void handwrittenSave3(BitOutputStream output) throws IOException {
			try {
				IntegerBitser.encodeInteger((Integer) fieldX.get(this), VARIABLE_INT, output);
				IntegerBitser.encodeInteger((Integer) fieldY.get(this), VARIABLE_INT, output);
				IntegerBitser.encodeInteger((Integer) fieldZ.get(this), UNIFORM_INT, output);
				FloatBitser.encodeFloat((Float) fieldA.get(this), false, FLOAT, output);
				FloatBitser.encodeFloat((Float) fieldB.get(this), false, FLOAT, output);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		void handwrittenRead3(BitInputStream input) throws IOException {
			try {
				fieldX.set(this, (int) IntegerBitser.decodeInteger(VARIABLE_INT, input));
				fieldY.set(this, (int) IntegerBitser.decodeInteger(VARIABLE_INT, input));
				fieldZ.set(this, (int) IntegerBitser.decodeInteger(UNIFORM_INT, input));
				fieldA.set(this, (float) FloatBitser.decodeFloat(false, FLOAT, input));
				fieldB.set(this, (float) FloatBitser.decodeFloat(false, FLOAT, input));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		Bitser bitser = new Bitser();

		RootStruct root = new RootStruct();
		for (int counter = 0; counter < 500_000; counter++) {
			root.largeMap.put("test" + counter, "test" + counter + "test");
		}

		Random rng = new Random(12345);

		root.bigBoys = new BigStruct[5_000];
		for (int index = 0; index < root.bigBoys.length; index++) {
			root.bigBoys[index] = new BigStruct();
			if (rng.nextInt(10) == 0) root.bigBoys[index].friend = root.bigBoys[rng.nextInt(index + 1)];
			int numChildren = rng.nextInt(1000);
			for (int counter = 0; counter < numChildren; counter++) {
				root.bigBoys[index].children.add(new SmallStruct(
						rng.nextInt(100), -rng.nextInt(200), rng.nextInt(),
						rng.nextFloat(), 0.5f * rng.nextInt(100)
				));
			}
		}

		SampleStorage<FrequencyThreadStorage> storage = SampleStorage.frequency();
		SampleProfiler profiler = new SampleProfiler(storage);

		long startWrite = System.nanoTime();
		byte[] bytes = bitser.toBytes(root);
		long writeTime = System.nanoTime() - startWrite;

		System.out.println("Needed " + (writeTime / 1000_000) + " ms to write " + bytes.length + " bytes");

		//profiler.start();
		long startRead = System.nanoTime();
		bitser.fromBytes(RootStruct.class, bytes);
		long readTime = System.nanoTime() - startRead;
		//profiler.stop();
		System.out.println("Needed " + (readTime / 1000_000) + " ms");

		ByteArrayOutputStream dummyData = new ByteArrayOutputStream();
		DataOutputStream dummyOutput = new DataOutputStream(dummyData);
		long startDataDump = System.nanoTime();
		for (int counter = 0; counter < bytes.length / 4; counter++) {
			dummyOutput.writeFloat((float) counter);
		}
		long dataDumpTime = System.nanoTime() - startDataDump;
		System.out.println("Needed " + (dataDumpTime / 1000_000) + " ms to write an equal amount using writeFloat");

		DataInputStream dummyInput = new DataInputStream(new ByteArrayInputStream(dummyData.toByteArray()));
		long startDataRead = System.nanoTime();
		for (int counter = 0; counter < bytes.length / 4; counter++) {
			dummyInput.readFloat();
		}
		long dataReadTime = System.nanoTime() - startDataRead;
		System.out.println("Needed " + (dataReadTime / 1000_000) + " ms to read an equal amount using readFloat");

		dummyData.reset();
		long startHandwrittenSave1 = System.nanoTime();
		root.handwrittenSave1(dummyOutput);
		long handwrittenSaveTime1 = System.nanoTime() - startHandwrittenSave1;
		System.out.println("Needed " + (handwrittenSaveTime1 / 1000_000) + " ms to write handwritten (1)");

		dummyInput = new DataInputStream(new ByteArrayInputStream(dummyData.toByteArray()));
		long startHandwrittenRead1 = System.nanoTime();
		new RootStruct().handwrittenRead1(dummyInput);
		long handwrittenReadTime1 = System.nanoTime() - startHandwrittenRead1;
		System.out.println("Needed " + (handwrittenReadTime1 / 1000_000) + " ms to read handwritten (1)");

		dummyData.reset();
		long startHandwrittenSave2 = System.nanoTime();
		root.handwrittenSave2(new BitOutputStream(dummyData));
		long handwrittenSaveTime2 = System.nanoTime() - startHandwrittenSave2;
		System.out.println("Needed " + (handwrittenSaveTime2 / 1000_000) + " ms to write handwritten (2)");

		long startHandwrittenRead2 = System.nanoTime();
		new RootStruct().handwrittenRead2(new BitInputStream(new ByteArrayInputStream(dummyData.toByteArray())));
		long handwrittenReadTime2 = System.nanoTime() - startHandwrittenRead2;
		System.out.println("Needed " + (handwrittenReadTime2 / 1000_000) + " ms to read handwritten (2)");

		dummyData.reset();
		long startHandwrittenSave3 = System.nanoTime();
		root.handwrittenSave3(new BitOutputStream(dummyData));
		long handwrittenSaveTime3 = System.nanoTime() - startHandwrittenSave3;
		System.out.println("Needed " + (handwrittenSaveTime3 / 1000_000) + " ms to write handwritten (3)");

		long startHandwrittenRead3 = System.nanoTime();
		new RootStruct().handwrittenRead3(new BitInputStream(new ByteArrayInputStream(dummyData.toByteArray())));
		long handwrittenReadTime3 = System.nanoTime() - startHandwrittenRead3;
		System.out.println("Needed " + (handwrittenReadTime3 / 1000_000) + " ms to read handwritten (3)");

		long startBackWrite = System.nanoTime();
		byte[] backBytes = bitser.toBytes(root, Bitser.BACKWARD_COMPATIBLE);
		long writeBackTime = System.nanoTime() - startBackWrite;
		System.out.println("Needed " + (writeBackTime / 1000_000) + " ms to write new backward " + backBytes.length + " bytes");

		profiler.start();
		long startBackRead = System.nanoTime();
		bitser.fromBytes(RootStruct.class, backBytes, Bitser.BACKWARD_COMPATIBLE);
		long readBackTime = System.nanoTime() - startBackRead;
		profiler.stop();
		System.out.println("Needed " + (readBackTime / 1000_000) + " ms to read new backward");

		storage.getThreadStorage(Thread.currentThread().getId()).print(System.out, 10, 1.0);
	}
}
