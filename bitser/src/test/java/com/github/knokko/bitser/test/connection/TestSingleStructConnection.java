package com.github.knokko.bitser.test.connection;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.connection.BitClient;
import com.github.knokko.bitser.connection.BitServer;
import com.github.knokko.bitser.connection.StructConnectionView;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestSingleStructConnection {

	@BitStruct(backwardCompatible = false)
	private static class Vector3 implements Serializable {

		@IntegerField(expectUniform = false)
		final int x;

		@IntegerField(expectUniform = false, minValue = 100)
		final int y;

		@IntegerField(expectUniform = false, maxValue = 100)
		final int z;

		Vector3(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@SuppressWarnings("unused")
		private Vector3() {
			this(0, 0, 0);
		}
	}

	@Test
	public void testVectorConnection() throws IOException, InterruptedException {
		var bitser = new Bitser();
		var protocol = bitser.getProtocol(Vector3.class);

		var view = new StructConnectionView(protocol);
		view.markAllSimpleFields(true, true);
		view.markSimpleField(null, "y", true, false);
		view.markSimpleField(Vector3.class, "z", false, true);
		view.finishRegistration();

		class ClientVector {

			int x, y, z;
			int numUpdates;
		}

		var readOnlyClientVector = new ClientVector();

		var readOnlyClientConnection = new BitClient.ReadOnlyStruct(view, 0L);
		readOnlyClientConnection.<Integer>subscribeValue(Vector3.class, "x", x -> {
			readOnlyClientVector.x = x;
			readOnlyClientVector.numUpdates += 1;
		});
		readOnlyClientConnection.<Integer>subscribeValue(null, "y", y -> {
			readOnlyClientVector.y = y;
			readOnlyClientVector.numUpdates += 1;
		});
		readOnlyClientConnection.subscribeValue(null, "z", z -> fail("Tried to update z to " + z));

		var serverToReadOnlyClientOutput = new PipedOutputStream();
		var serverToReadOnlyClientInput = new PipedInputStream(serverToReadOnlyClientOutput);

		var readOnlyClientToServerOutput = new PipedOutputStream();
		var readOnlyClientToServerInput = new PipedInputStream(readOnlyClientToServerOutput);

		var readOnlyClientThread = new Thread(() -> {
			try {
				readOnlyClientConnection.readFromServer(new BitInputStream(serverToReadOnlyClientInput));
			} catch (Throwable failed) {
				throw new RuntimeException(failed);
			}
		});
		readOnlyClientThread.setDaemon(true);
		readOnlyClientThread.start();

		var serverVector = new Vector3(5, 106, 7);
		var controller = new BitServer.StructController<>(123L, serverVector, view);
		controller.addClient(serverToReadOnlyClientOutput, readOnlyClientToServerInput);

		Thread.sleep(500);
		assertEquals(5, readOnlyClientVector.x);
		assertEquals(106, readOnlyClientVector.y);
		assertEquals(0, readOnlyClientVector.z); // Write-only

		var serverToReadWriteClientOutput = new PipedOutputStream();
		var serverToReadWriteClientInput = new PipedInputStream(serverToReadWriteClientOutput);

		var readWriteClientToServerOutput = new PipedOutputStream();
		var readWriteClientToServerInput = new PipedInputStream(readWriteClientToServerOutput);

		controller.addClient(serverToReadWriteClientOutput, serverToReadWriteClientInput);
		Thread.sleep(100);
	}
}
