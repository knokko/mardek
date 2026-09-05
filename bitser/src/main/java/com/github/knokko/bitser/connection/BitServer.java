package com.github.knokko.bitser.connection;

//import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
//import com.github.knokko.bitser.io.BitInputStream;
//import com.github.knokko.bitser.Bitser;
//
//import java.io.*;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.*;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//
//import static com.github.knokko.bitser.connection.ConnectionHelper.*;

public class BitServer<T> {

//	public static <T> BitServer<T> tcp(Bitser bitser, T rootStruct, int port) throws IOException {
//		@SuppressWarnings("resource") ServerSocket serverSocket = new ServerSocket(port);
//
//		BitServer<T> server = new BitServer<>(bitser, rootStruct, serverSocket.getLocalPort(), () -> {
//			try {
//				serverSocket.close();
//			} catch (IOException alreadyClosed) {
//				// Socket is already closed, so there is nothing more we can do
//			}
//		});
//
//		new Thread(() -> {
//			while (!serverSocket.isClosed()) {
//				try {
//					Socket next = serverSocket.accept();
//					server.addConnection(next.getInputStream(), next.getOutputStream());
//				} catch (IOException io) {
//					System.out.println("Failed to open a connection with a client: " + io.getMessage());
//				}
//			}
//			server.stop();
//		}).start();
//
//		return server;
//	}
//
//	private final Bitser bitser;
//	public final BitStructConnection<T> root;
//	private final Collection<Connection> connections = new ArrayList<>();
//	private final BlockingQueue<byte[]> changesToServer = new LinkedBlockingQueue<>();
//	public final int port;
//	private final Runnable stopCallback;
//
//	public BitServer(Bitser bitser, T rootStruct, int port, Runnable stopCallback) {
//		this.bitser = bitser;
//		this.root = bitser.createStructConnection(rootStruct, null);
//		this.port = port;
//		this.stopCallback = stopCallback;
//
//		new Thread(this::runUpdateLoop).start();
//	}
//
//	public void stop() {
//		changesToServer.add(STOP_SIGN);
//	}
//
//	private void runUpdateLoop() {
//		try {
//			while (true) {
//				byte[] changes = changesToServer.take();
//
//				synchronized (this) {
//					for (Connection connection : connections) connection.changesToClient.add(changes);
//					if (changes == STOP_SIGN) break;
//
//					try {
//						root.handleChanges(new BitInputStream(new ByteArrayInputStream(changes)));
//					} catch (IOException shouldNotHappen) {
//						throw new UnexpectedBitserException(shouldNotHappen.getMessage());
//					}
//				}
//			}
//		} catch (InterruptedException shouldNotHappen) {
//			throw new UnexpectedBitserException(shouldNotHappen.getMessage());
//		} finally {
//			stopCallback.run();
//		}
//	}
//
//	private synchronized void addConnection(InputStream input, OutputStream output) throws IOException {
//		byte[] encodedRootState = bitser.serializeToBytes(root.state);
//		connections.add(new Connection(new DataInputStream(input), new DataOutputStream(output), encodedRootState));
//	}
//
//	private class Connection {
//
//		final BlockingQueue<byte[]> changesToClient = new LinkedBlockingQueue<>();
//
//		final DataInputStream input;
//		final DataOutputStream output;
//		byte[] encodedRootState;
//
//		Connection(DataInputStream input, DataOutputStream output, byte[] encodedRootState) {
//			this.input = input;
//			this.output = output;
//			this.encodedRootState = encodedRootState;
//
//			new Thread(this::inputLoop).start();
//			new Thread(this::outputLoop).start();
//		}
//
//		private void inputLoop() {
//			try {
//				while (true) {
//					changesToServer.add(readPacket(input));
//				}
//			} catch (IOException io) {
//				System.out.println("Connection input thread encountered IO exception: " + io.getMessage());
//			}
//		}
//
//		private void outputLoop() {
//			try {
//				sendPacket(encodedRootState, output);
//				encodedRootState = null;
//				while (true) {
//					byte[] nextChanges = changesToClient.take();
//					if (nextChanges == STOP_SIGN) break;
//					sendPacket(nextChanges, output);
//				}
//				output.close();
//			} catch (IOException io) {
//				System.out.println("Connection output thread encountered IO exception: " + io.getMessage());
//			} catch (InterruptedException e) {
//				throw new UnexpectedBitserException("interrupted");
//			}
//		}
//	}
}
