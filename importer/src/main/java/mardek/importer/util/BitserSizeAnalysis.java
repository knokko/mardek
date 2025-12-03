package mardek.importer.util;

import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class BitserSizeAnalysis extends BitOutputStream {

	public final Node root = new Node(null, "(root)");

	private Node current = root;
	private long bitsSoFar = 0L;

	public BitserSizeAnalysis(OutputStream byteStream) {
		super(byteStream);
		root.startTime = System.nanoTime();
	}

	@Override
	public void write(boolean value) throws IOException {
		super.write(value);
		bitsSoFar += 1L;
	}

	@Override
	public void write(int value, int numBits) throws IOException {
		super.write(value, numBits);
		bitsSoFar += numBits;
	}

	@Override
	public void write(byte[] values) throws IOException {
		super.write(values);
		bitsSoFar += 8L * values.length;
	}

	@Override
	public void pushContext(String context, int counter) {
		if (!current.children.containsKey(context)) {
			current.children.put(context, new Node(current, context));
		}

		current = current.children.get(context);
		current.startTime = System.nanoTime();
		current.startBits = bitsSoFar;
	}

	@Override
	public void popContext(String context, int counter) {
		current.spentTime += System.nanoTime() - current.startTime;
		current.spentBits += bitsSoFar - current.startBits;
		current = current.parent;
	}

	@Override
	public void prepareProperty(String fieldName, int counter) {
		pushContext(fieldName, counter);
	}

	@Override
	public void finishProperty() {
		popContext(null, 0);
	}

	@Override
	public void finish() {
		popContext(null, 0);
	}

	public static class Node {

		public final Node parent;
		public final String name;
		public long spentBits = 0L;
		public long spentTime = 0L;

		private long startTime = 0L;
		private long startBits = 0L;

		public final Map<String, Node> children = new HashMap<>();

		public Node(Node parent, String name) {
			this.parent = parent;
			this.name = name;
		}

		@Override
		public String toString() {
			return name + "(" + (spentBits / 1000.0) + " kbits, " + (spentTime / 1000_000.0) + "ms)";
		}
	}
}
