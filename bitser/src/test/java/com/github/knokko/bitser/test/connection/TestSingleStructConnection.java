package com.github.knokko.bitser.test.connection;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.connection.BitClient;
import com.github.knokko.bitser.connection.BitServer;
import com.github.knokko.bitser.connection.StructConnectionView;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
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

			void setX(int newX) {
				this.x = newX;
				this.numUpdates += 1;
			}

			void setY(int newY) {
				this.y = newY;
				this.numUpdates += 1;
			}

			void setZ(int newZ) {
				this.z = newZ;
				this.numUpdates += 1;
			}
		}

		var readOnlyClientVector = new ClientVector();

		var readOnlyClientConnection = new BitClient.ReadOnlyStruct(view, 0L);
		readOnlyClientConnection.subscribeValue(Vector3.class, "x", readOnlyClientVector::setX);
		readOnlyClientConnection.subscribeValue(null, "y", readOnlyClientVector::setY);
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

		Thread.sleep(300);
		assertEquals(5, readOnlyClientVector.x);
		assertEquals(106, readOnlyClientVector.y);
		assertEquals(0, readOnlyClientVector.z); // Write-only

		var serverToReadWriteClientOutput = new PipedOutputStream();
		var serverToReadWriteClientInput = new PipedInputStream(serverToReadWriteClientOutput);

		var readWriteClientToServerOutput = new PipedOutputStream();
		var readWriteClientToServerInput = new PipedInputStream(readWriteClientToServerOutput);

		controller.addClient(serverToReadWriteClientOutput, readWriteClientToServerInput);
		Thread.sleep(300);

		var readWriteClientConnection = new BitClient.ReadWriteStruct(
				view, new BitOutputStream(readWriteClientToServerOutput), 1L
		);

		var readWriteClientVector = new ClientVector();
		readWriteClientConnection.subscribeValue(null, "x", false, readWriteClientVector::setX);
		readWriteClientConnection.subscribeValue(null, "y", true, readWriteClientVector::setY);
		readWriteClientConnection.subscribeValue(null, "z", false, z -> fail("Tried to update z to " + z));
		readWriteClientConnection.subscribeValue(null, "z", true, readWriteClientVector::setZ);

		// TODO Subscribe change states

		var readWriteClientThread = new Thread(() -> {
			try {
				readWriteClientConnection.readFromServer(new BitInputStream(serverToReadWriteClientInput));
			} catch (Throwable failed) {
				throw new RuntimeException(failed);
			}
		});
		readWriteClientThread.setDaemon(true);
		readWriteClientThread.start();

		Thread.sleep(300);
		assertEquals(5, readWriteClientVector.x);
		assertEquals(106, readWriteClientVector.y);
		assertEquals(0, readWriteClientVector.z); // Write-only

		readWriteClientConnection.setValue(null, "x", 21);
		readWriteClientConnection.setValue(null, "y", 121);
		readWriteClientConnection.setValue(null, "z", 60);

		assertEquals(5, readWriteClientVector.x); // server-only
		assertEquals(121, readWriteClientVector.y);
		assertEquals(60, readWriteClientVector.z);

		readWriteClientConnection.saveValue(null, "x");
		readWriteClientConnection.saveValue(Vector3.class, "z");

		Thread.sleep(300);
		assertEquals(21, readWriteClientVector.x);
		assertEquals(121, readWriteClientVector.y);
		assertEquals(60, readWriteClientVector.z);

		assertEquals(21, readOnlyClientVector.x);
		assertEquals(106, readOnlyClientVector.y);
		assertEquals(0, readOnlyClientVector.z); // write-only
	}
}
