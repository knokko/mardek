package com.github.knokko.bitser.io;

import com.github.knokko.bitser.RecursionNode;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.options.AnalyzePerformance;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * A subclass of {@link BitInputStream} that tracks how much time, and how many bits, are spent on each class and each
 * field. Bitser will use this when you pass an {@link AnalyzePerformance} option to
 * {@link com.github.knokko.bitser.Bitser#fromBytes}.
 */
public class AnalysisBitInputStream extends BitInputStream {

	private final Map<RecursionNode, NodeBuilder> mapping = new HashMap<>();
	private final AnalyzePerformance analysisOutput;
	private NodeBuilder mostRecentNode;
	private CurrentStage currentStage;

	private long bitsSoFar;

	/**
	 * Constructs a new {@link AnalysisBitInputStream} that reads bits from {@code byteStream}
	 */
	public AnalysisBitInputStream(InputStream byteStream, AnalyzePerformance analysisOutput) {
		super(byteStream);
		this.analysisOutput = Objects.requireNonNull(analysisOutput);
	}

	@Override
	public boolean read() throws IOException {
		bitsSoFar += 1;
		return super.read();
	}

	@Override
	public int read(int numBits) throws IOException {
		bitsSoFar += numBits;
		return super.read(numBits);
	}

	@Override
	public void read(byte[] destination) throws IOException {
		super.read(destination);
		long bytesSoFar = bitsSoFar / 8;
		if (8 * bytesSoFar != bitsSoFar) bitsSoFar = 8 * (1 + bytesSoFar);
		bitsSoFar += 8L * destination.length;
	}

	private NodeBuilder getNode(RecursionNode node) {
		if (node == null) return null;
		return mapping.computeIfAbsent(node, NodeBuilder::new);
	}

	private void startTimer(NodeBuilder node, long currentTime) {
		if (node.running) {
			throw new UnexpectedBitserException("Node already running: " + node.context.generateTrace(null));
		}
		node.running = true;
		node.finalized = false;
		node.startTime = currentTime;
		node.startBits = bitsSoFar;
	}

	private void stopTimer(NodeBuilder node, long currentTime) {
		if (!node.running) {
			throw new UnexpectedBitserException(node + " is not running");
		}
		node.running = false;
		node.spentBits += bitsSoFar - node.startBits;
		node.spentTime += currentTime - node.startTime;
	}

	@Override
	public void pushContext(RecursionNode context, String fieldName) {
		if (fieldName != null) context = new RecursionNode(context, fieldName);
		var node = getNode(context);

		long currentTime = System.nanoTime();
		if (mostRecentNode != null && !mostRecentNode.finalized) stopTimer(mostRecentNode, currentTime);
		startTimer(node, currentTime);
		mostRecentNode = node;
	}

	@Override
	public void popContext(RecursionNode context, String fieldName) {
		if (fieldName != null) context = new RecursionNode(context, fieldName);
		if (context.parent != null && !mapping.containsKey(context.parent)) {
			throw new UnexpectedBitserException("Parent node of " + context + " missing");
		}

		var node = getNode(context);
		long currentTime = System.nanoTime();
		stopTimer(node, currentTime);
		node.finalized = true;
		mostRecentNode = getNode(node.context.parent);
		if (mostRecentNode != null && !mostRecentNode.finalized) startTimer(mostRecentNode, currentTime);
	}

	@Override
	public void prepareProperty(String fieldName) {
		pushContext(mostRecentNode.context, fieldName);
	}

	@Override
	public void finishProperty() {
		popContext(mostRecentNode.context.parent, mostRecentNode.context.label);
	}

	@Override
	public void setMarker(String marker) {
		nextMarker(marker);
	}

	private void nextMarker(String marker) {
		long currentTime = System.nanoTime();
		if (currentStage != null) {
			analysisOutput.stages.add(new Stage(
					currentStage.name,
					currentTime - currentStage.startTime,
					bitsSoFar - currentStage.startBits
			));
		}
		currentStage = new CurrentStage(marker, currentTime, bitsSoFar);
	}

	@Override
	public void close() throws IOException {
		super.close();
		nextMarker(null);

		var allNodes = mapping.values().stream().sorted(
				(a, b) -> -Integer.compare(a.context.depth, b.context.depth)
		).toList();

		var nodeMap = new HashMap<NodeBuilder, Node>();
		for (NodeBuilder builder : allNodes) {
			if (builder.running) {
				System.out.println(builder);
				stopTimer(builder, System.nanoTime());
				System.out.println(builder);
			}
			if (builder.context.parent != null) {
				getNode(builder.context.parent).children.add(builder);
			}
			for (NodeBuilder child : builder.children) {
				builder.spentBits += child.spentBits;
				builder.spentTime += child.spentTime;
			}
			builder.children.sort((a, b) -> -Long.compare(a.spentBits, b.spentBits));
			builder.startTime = 0L;
			builder.startBits = 0L;
			var node = new Node(
					builder.context, builder.spentTime, builder.spentBits,
					builder.children.stream().map(nodeMap::get).toList()
			);
			nodeMap.put(builder, node);
			if (node.context.depth == 0) analysisOutput.rootNodes.add(node);
		}
	}

	/**
	 * This record represents an analysis node. The analysis will create a node for each field/property of the root
	 * struct and all of its descendants.
	 * @param context The 'context': the field to which this node belongs
	 * @param spentTime The time (in nanoseconds) that was spent to deserialize this field/property
	 * @param spentBits The number of bits that were used to (de)serialize this field/property
	 * @param children The child nodes of this node
	 */
	public record Node(RecursionNode context, long spentTime, long spentBits, List<Node> children) {

		@Override
		public String toString() {
			return String.format(
					Locale.ROOT, "Node(%s, time=%,dns, bits=%,d, #children=%d)",
					context.toString(), spentTime, spentBits, children.size()
			);
		}
	}

	/**
	 * This record represents a (de)serialization stage. The analysis will track how much time and how many bits are
	 * spent on each of the deserialization stages of bitser.
	 * @param name The name & description of the stage
	 * @param spentTime The time that was spent in this stage, in nanoseconds
	 * @param spentBits The number of bits that was spent in this stage, which will be 0 for stages that don't read
	 *                  any bits.
	 */
	public record Stage(String name, long spentTime, long spentBits) {

		@Override
		public String toString() {
			return String.format(Locale.ROOT, "%s: spent %,dns and %,d bits", name, spentTime, spentBits);
		}
	}

	private record CurrentStage(String name, long startTime, long startBits) {}

	private static class NodeBuilder {

		final RecursionNode context;
		final List<NodeBuilder> children = new ArrayList<>();
		long startTime, startBits;
		boolean running;
		boolean finalized;

		long spentBits;
		long spentTime;

		NodeBuilder(RecursionNode context) {
			this.context = context;
		}

		@Override
		public String toString() {
			return String.format(
					Locale.ROOT, "Node(%s, time=%,d, bits=%,d, #children=%d)",
					context.toString(), spentTime, spentBits, children.size()
			);
		}
	}
}
