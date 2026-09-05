package com.github.knokko.bitser;

record WriteLazyJob(ReferenceLazyBits<?> lazy, BitStructWrapper<?> valueWrapper, String[] labels, RecursionNode node) {

	void save(Serializer serializer) throws Exception {
		LazyReferenceTargets[] targets = new LazyReferenceTargets[labels.length];
		for (int index = 0; index < labels.length; index++) {
			targets[index] = serializer.references.getLazyTargets(labels[index]);
		}
		var optionList = Bitser.getOptionsWithoutWithObjects(serializer.withAndOptions);
		optionList.add(targets);

		byte[] bytes = serializer.bitser.toBytes(lazy.get(), optionList.toArray());
		serializer.output.prepareProperty("lazy-length");
		IntegerBitser.encodeUnknownLength(bytes.length, serializer.output);
		serializer.output.finishProperty();
		serializer.output.prepareProperty("lazy-bytes");
		serializer.output.write(bytes);
		serializer.output.finishProperty();
	}
}
