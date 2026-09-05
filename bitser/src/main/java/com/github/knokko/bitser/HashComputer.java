package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

class HashComputer {

	final Bitser bitser;
	final MessageDigest digest;

	final ArrayList<HashStructJob> structJobs = new ArrayList<>();
	final ArrayList<HashArrayJob> arrayJobs = new ArrayList<>();

	HashComputer(Object root, Bitser bitser) {
		this.bitser = bitser;
		try {
			this.digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new UnexpectedBitserException("No SHA-256 support?");
		}
		structJobs.add(new HashStructJob(
				root, bitser.cache.getWrapper(root.getClass()), new RecursionNode("root")
		));
	}

	void feedHash() {
		while (!structJobs.isEmpty() || !arrayJobs.isEmpty()) {
			if (!structJobs.isEmpty()) {
				structJobs.remove(structJobs.size() - 1).hash(this);;
			}
			if (!arrayJobs.isEmpty()) {
				var job = arrayJobs.remove(arrayJobs.size() - 1);
				try {
					job.hash(this);
				} catch (Throwable failed) {
					throw new RecursionException(job.node().generateTrace(job.description()), failed);
				}
			}
		}
	}
}
