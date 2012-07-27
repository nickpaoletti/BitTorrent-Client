package btclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Logger;
/* Peer.java
 * 
 * by Nick Paoletti and Daniel Selmon
 * 
 * This is the Peer object, used primarily by the TrackerInfo object, but also used in
 * Download to update the files downloaded from the peer. It stores each Peers IP, Peer ID,
 * Port, and which files you have downloaded from them in a boolean array called the 'bitfield.'
 */

import btclient.message.Message;

public class Peer {

	private static final Logger log = Logger.getLogger(Peer.class.getName());
	private String ip;
	private byte[] peerId;
	private int port;
	private boolean[] bitfield;
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private boolean isConnected;

	public Peer(String ip, int port, byte[] peerid, int numPieces) {
		this.ip = ip;
		this.peerId = peerid;
		this.port = port;
		// Defaults to "false"
		this.bitfield = new boolean[numPieces];
	}
	
	public void newBitfield(boolean[] bitfield){
		this.bitfield = bitfield;
	}
	
	public String getIP() {
		return ip;
	}

	public boolean getIsConnected(){
		return this.isConnected;
	}
	
	public void changeStatus(boolean status){
		this.isConnected = status;
	}
	
	public void printBitfield(){
		System.out.println("");
		for (int i = 0; i < bitfield.length; i ++){
			System.out.print(i + ":" + bitfield[i] + ",");
		}
	}
	public byte[] getPeerId() {
		return this.peerId;
	}

	public int getPort() {
		return port;
	}

	public boolean[] getBitfield() {
		return this.bitfield;
	}
	
	public void changeBitfield(int index, boolean status){
		bitfield[index] = status;
		return;
	}

	@Override
	public String toString() {
		if (this.peerId != null) {
			try {
				return new String(this.peerId, "US-ASCII") + "@" + this.ip
						+ ":" + this.port;
			} catch (UnsupportedEncodingException e) {
				// Ignored
			}
		}
		return "Peer @ " + this.ip + ":" + this.port;

	}

	/**
	 * Returns the next Message on this peer's input stream, or {@code null} if
	 * no message is available.
	 * 
	 * @return the next message on this peer's input stream, or {@code null} if
	 *         non exists.
	 * @throws IOException
	 *             if an IOException is generated by reading from the
	 *             InputStream
	 */
	@Deprecated
	public Message getNextMessage() throws IOException {
		if (this.in != null && this.in.available() >= 4) {
			return Message.decode(this.in);
		}
		return null;
	}

	/**
	 * Returns the next Message on this peer's input stream, blocking until it
	 * arrives.
	 * 
	 * @return the next message on this peer's input stream, blocking until it
	 *         arrives.
	 * @throws IOException
	 *             if an IOException is generated by reading from the
	 *             InputStream
	 */
	public Message getNextMessageBlocking() throws IOException {
		return Message.decode(this.in);
	}

	public synchronized void sendMessage(Message m) throws IOException {
		Message.encode(m, this.out);
	}

	public static final byte[] HANDSHAKE_TEMPLATE = new byte[] { 19, 'B', 'i',
			't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't',
			'o', 'c', 'o', 'l', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	
	private void initializeStreams() throws UnknownHostException, IOException{
		this.socket = new Socket(ip, port);
		this.in = this.socket.getInputStream();
		this.out = this.socket.getOutputStream();
	}

	/**
	 * Handshakes with the remote peer.
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean handshake(final byte[] infoHash, final byte[] localPeerId)
			throws IOException {
		System.out.println("Handshaking with peer " + toString());
		
		initializeStreams();
		
		byte[] sent = new byte[HANDSHAKE_TEMPLATE.length];
		System.arraycopy(HANDSHAKE_TEMPLATE, 0, sent, 0, sent.length);
		System.arraycopy(infoHash, 0, sent, 28, infoHash.length);
		System.arraycopy(localPeerId, 0, sent, 48, localPeerId.length);

		byte[] received = new byte[sent.length];

		DataOutputStream dout = new DataOutputStream(this.out);
		DataInputStream din = new DataInputStream(this.in);

		dout.write(sent);
		din.readFully(received);
		byte[] sentPstr = Arrays.copyOfRange(sent, 0, 21);
		byte[] recvPstr = Arrays.copyOfRange(received, 0, 21);
		byte[] recvInfo = Arrays.copyOfRange(received, 28, 48);
		byte[] recvPeerId = Arrays.copyOfRange(received, 48, 68);

		if (!Arrays.equals(sentPstr, recvPstr)) {
			log.severe("Protocol string did not match in received handshake from "
					+ this);
			return false;
		}

		if (this.peerId == null) {
			this.peerId = recvPeerId;
		} else if (!Arrays.equals(recvPeerId, this.peerId)) {
			log.severe("Peer ID did not match in received handshake from "
					+ this);
			return false;
		}

		if (!Arrays.equals(recvInfo, infoHash)) {
			log.severe("Info hash did not match in received handshake from "
					+ this);
			return false;
		}

		return true;
	}

	public void disconnect() {

		if (this.in != null) {
			try {
				this.in.close();
			} catch (IOException e) {
				// Ignored
			}
			this.in = null;
		}
		if (this.out != null) {
			try {
				this.out.close();
			} catch (IOException e) {
				// Ignored
			}
			this.out = null;
		}
		if (this.socket != null) {
			try {
				this.socket.close();
			} catch (IOException e) {
				// Ignored
			}
			this.socket = null;
		}
	}
}