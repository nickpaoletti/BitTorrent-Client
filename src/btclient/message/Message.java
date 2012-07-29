package btclient.message;

import java.io.*;
import java.nio.*;
import java.security.*;
import java.util.Arrays;
import java.util.logging.Logger;

import btclient.Download;
import btclient.FileManager;
import btclient.Peer;
import btclient.TrackerInfo;

/* Message.java
 * 
 * by Nick Paoletti and Daniel Selmon

 * This is the Message class, which handles basic functionality for the BitTorrent messages needed
 * during this part of the project. It will take in messages read from the Peer, and give back to the peer
 * a corresponding message that works well as a response.
 */

public class Message {

	private static final Logger log = Logger.getLogger(Message.class.getName());

	public static final byte TYPE_CHOKE = 0;
	public static final byte TYPE_UNCHOKE = 1;
	public static final byte TYPE_INTERESTED = 2;
	public static final byte TYPE_UNINTERESTED = 3;
	public static final byte TYPE_HAVE = 4;
	public static final byte TYPE_BITFIELD = 5;
	public static final byte TYPE_REQUEST = 6;
	public static final byte TYPE_PIECE = 7;

	/**
	 * A choke message.
	 */
	public static final Message CHOKE = new Message(1, TYPE_CHOKE);
	/**
	 * An unchoke message.
	 */
	public static final Message UNCHOKE = new Message(1, TYPE_UNCHOKE);
	/**
	 * A Keep-Alive message.
	 */
	public static final Message KEEP_ALIVE = new Message(0, (byte) 0);
	/**
	 * An interested message.
	 */
	public static final Message INTERESTED = new Message(1, TYPE_INTERESTED);
	/**
	 * An uninterested message.
	 */
	public static final Message UNINTERESTED = new Message(1, TYPE_UNINTERESTED);

	/**
	 * Length of the remaining message, in octets.
	 */
	protected final int length;

	/**
	 * The ID of the message.
	 */
	protected final byte type;

	protected Message(final int length, final byte type) {
		this.length = length;
		this.type = type;
	}

	// When given a message from the Peer, respond with a byte array that is a
	// fitting response.
	// This is not the approach which will remain in future parts of the
	// project, and is very
	// limited in functionality.
	public static Message decode(final InputStream in) throws IOException {
		DataInputStream din = new DataInputStream(in);
		int length = din.readInt();
		if (length == 0) {
			return KEEP_ALIVE;
		}
		byte type = din.readByte();
		switch (type) {
		case TYPE_CHOKE:
			return CHOKE;
		case TYPE_UNCHOKE:
			return UNCHOKE;
		case TYPE_INTERESTED:
			return INTERESTED;
		case TYPE_UNINTERESTED:
			return UNINTERESTED;
		case TYPE_HAVE: {
			int index = din.readInt();
			return new HaveMessage(index);
		}
		case TYPE_BITFIELD: {
			byte[] byteBitfield = new byte[length - 1];
			din.readFully(byteBitfield);
			boolean[] bitfield = bitfield(byteBitfield,
					FileManager.bitfield.length);
			return new BitfieldMessage(bitfield);
		}
		case TYPE_REQUEST: {
			int index = din.readInt();
			int offset = din.readInt();
			int pieceLength = din.readInt();
			return new RequestMessage(index, offset, pieceLength);
		}
		case TYPE_PIECE: {
			int index = din.readInt();
			int offset = din.readInt();
			byte[] pieceData = new byte[length - 9];
			din.readFully(pieceData);
			return new PieceMessage(index, offset, pieceData);
		}

		default:
			log.severe("Unknown message type " + ('0' + type));
			throw new IOException("Unknown message type " + ('0' + type));
		}
	}

	public static void encode(final Message msg, final OutputStream out)
			throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		dos.writeInt(msg.length);
		if (msg.length > 0) {
			dos.writeByte(msg.type);
			switch (msg.type) {
			case TYPE_HAVE:
				dos.writeInt(((HaveMessage)msg).getIndex());
				break;
			case TYPE_BITFIELD: {
				//TO DO
				break;
			}
			case TYPE_REQUEST: {
				dos.writeInt(((RequestMessage)msg).getIndex());
				dos.writeInt(((RequestMessage)msg).getOffset());
				dos.writeInt(((RequestMessage)msg).getPieceLength());
				break;
			}
			case TYPE_PIECE: {
				dos.writeInt(((PieceMessage)msg).getIndex());
				dos.writeInt(((PieceMessage)msg).getOffset());
				dos.write(((PieceMessage)msg).getPieceData());
				break;
			}
			//For Choke, Unchoke, Interested, and Uninterested, you have nothing special to write. Woo.
			}

		}
		dos.flush();
	}

	/*
	 * Given a bitfield message from the peer, it creates a boolean bitfield by
	 * splitting up each byte into four 'boolean' bits. This is stored within
	 * the Peer, and updated upon successful downloading of pieces of the file.
	 */
	private static boolean[] bitfield(byte[] message, int numbits)
			throws IOException {
		System.out.println("Peer sent bitfield.");
		// For each byte in the bitfield reach from the file, there are 8 bits.

		/*
		 * Code for byte to bit conversion (mostly) taken from Rob Moore's
		 * BitToBoolean.java located at
		 * https://sakai.rutgers.edu/portal/site/e07619c5
		 * -a492-4ebe-8771-179dfe450ae4/page/0a7200cf-0538-479a-a197-8d398c438484
		 */

		boolean[] bitfield = new boolean[numbits];
		int bitcounter = 0;

		for (int i = 0; i < message.length ; ++i) {
			for (int bitIndex = 7; bitIndex >= 0; --bitIndex) {
				if (bitcounter >= numbits) {
					break;
				}

				if ((message[i] >> bitIndex & 0x01) == 1) {
					bitfield[bitcounter] = true;
				} else {
					bitfield[bitcounter] = false;
				}
				bitcounter++;
			}
		}

		return bitfield;
	}

	public byte getType() {
		return type;
	}

}
