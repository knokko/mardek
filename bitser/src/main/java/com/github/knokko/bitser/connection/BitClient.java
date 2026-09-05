package com.github.knokko.bitser.connection;

//import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
//import com.github.knokko.bitser.io.BitInputStream;
//import com.github.knokko.bitser.io.BitOutputStream;
//import com.github.knokko.bitser.Bitser;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//
//import static com.github.knokko.bitser.connection.ConnectionHelper.*;

public class BitClient<T> {

//	public static <T> BitClient<T> tcp(Bitser bitser, Class<T> rootStructClass, String host, int port) throws IOException {
//		@SuppressWarnings("resource") Socket socket = new Socket(host, port);
//
//		DataInputStream input = new DataInputStream(socket.getInputStream());
//		T rootStruct = bitser.deserializeFromBytes(rootStructClass, readPacket(input));
//
//		return new BitClient<>(bitser, rootStruct, input, new DataOutputStream(socket.getOutputStream()), socket::close);
//	}
//
//	@FunctionalInterface
//	public interface CloseMethod {
//
//		void close() throws IOException;
//	}
//
//	public final BitStructConnection<T> root;
//	private final BlockingQueue<byte[]> pendingChanges = new LinkedBlockingQueue<>();
//	private final DataInputStream input;
//	private final DataOutputStream output;
//	private final CloseMethod close;
//
//	public BitClient(Bitser bitser, T rootStruct, DataInputStream input, DataOutputStream output, CloseMethod close) {
//		this.root = bitser.createStructConnection(rootStruct, listener -> {
//			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
//			BitOutputStream bitOutput = new BitOutputStream(byteOutput);
//			try {
//				listener.report(bitOutput);
//				bitOutput.finish();
//			} catch (IOException shouldNotHappen) {
//				throw new UnexpectedBitserException("ByteArrayOutputStream threw IOException?");
//			}
//			pendingChanges.add(byteOutput.toByteArray());
//		});
//		this.input = input;
//		this.output = output;
//		this.close = close;
//
//		new Thread(this::inputThread).start();
//		new Thread(this::outputThread).start();
//	}
//
//	private void inputThread() {
//		try {
//			while (true) {
//				root.handleChanges(new BitInputStream(new ByteArrayInputStream(readPacket(input))));
//			}
//		} catch (IOException io) {
//			System.out.println("Client input thread encountered IO exception: " + io.getMessage());
//		} finally {
//			pendingChanges.add(STOP_SIGN);
//		}
//	}
//
//	private void outputThread() {
//		try {
//			while (true) {
//				byte[] nextChanges = pendingChanges.take();
//				if (nextChanges == STOP_SIGN) return;
//				sendPacket(nextChanges, output);
//			}
//		} catch (IOException io) {
//			System.out.println("Client output thread encountered IO exception: " + io.getMessage());
//		} catch (InterruptedException e) {
//			throw new UnexpectedBitserException("interrupted");
//		} finally {
//			try {
//				close.close();
//			} catch (IOException alreadyClosed) {
//				// Already closed, nothing more we can do
//			}
//		}
//	}
//
//	public void close() {
//		pendingChanges.add(STOP_SIGN);
//	}
}
