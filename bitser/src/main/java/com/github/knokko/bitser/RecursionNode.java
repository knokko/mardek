package com.github.knokko.bitser;

/**
 * This class is used to generate fake stack traces for {@link com.github.knokko.bitser.exceptions.RecursionException}.
 * Furthermore, it is used by {@link com.github.knokko.bitser.io.AnalysisBitInputStream} to show which objects cost a
 * lot of time and space.
 */
public class RecursionNode {

	/**
	 * The parent/previous node, or {@code null} if this is the first/root node.
	 */
	public final RecursionNode parent;

	/**
	 * The label of this node, which is typically the name of a field or method.
	 */
	public final String label;

	/**
	 * The depth of this node, which is the number of nodes between this node and the root node.
	 */
	public final int depth;

	private final int hashCode;

	/**
	 * Constructs a new {@link RecursionNode} with the given label and parent node.
	 */
	public RecursionNode(RecursionNode parent, String label) {
		this.parent = parent;
		this.label = label;
		this.depth = 1 + parent.depth;
		this.hashCode = 13 * parent.hashCode - 17 * label.hashCode();
	}

	/**
	 * Constructs a new <b>root</b> {@link RecursionNode} with the given label
	 */
	public RecursionNode(String label) {
		this.parent = null;
		this.label = label;
		this.depth = 0;
		this.hashCode = label.hashCode();
	}

	/**
	 * Generates a fade/debug stacktrace from the root node to which node, containing every {@link #label} on the way.
	 * @param top An optional label that should come after {@code this.label}. This parameter is convenient for bitser
	 *            internally to avoid some object allocations.
	 */
	public String generateTrace(String top) {
		String[] labels = new String[depth + 1];

		RecursionNode next = this;
		while (next != null) {
			labels[next.depth] = next.label;
			next = next.parent;
		}

		StringBuilder builder = new StringBuilder();
		int numArrows = labels.length;
		if (top == null) numArrows -= 1;
		for (String label : labels) {
			builder.append(label);
			if (numArrows-- > 0) builder.append(" -> ");
		}
		if (top != null) builder.append(top);
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RecursionNode otherNode) {
			if (this.depth != otherNode.depth) return false;

			var myNode = this;
			while (myNode != null) {
				if (myNode == otherNode) return true;
				if (myNode.hashCode != otherNode.hashCode || !myNode.label.equals(otherNode.label)) return false;
				myNode = myNode.parent;
				otherNode = otherNode.parent;
			}

			return true;
		} else return false;
	}

	@Override
	public String toString() {
		return "(label=" + label + ", depth=" + depth + ")";
	}
}
