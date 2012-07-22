import java.io.*;
import java.nio.*;
import java.security.*;

/* Message.java
 * 
 * by Nick Paoletti and Daniel Selmon

 * This is the Message class, which handles basic functionality for the BitTorrent messages needed
 * during this part of the project. It will take in messages read from the Peer, and give back to the peer
 * a corresponding message that works well as a response.
 */


public class Message {
	//Declare the Interested message. Sent after the user gives you a bitfield. 
	final static byte[] INTERESTED = {0, 0, 0, (byte)1, (byte)2};
	
	//When given a message from the Peer, respond with a byte array that is a fitting response.
	//This is not the approach which will remain in future parts of the project, and is very
	//limited in functionality.
	public static byte[] decode(byte[] message, Peer designatedPeer, TrackerInfo tracker) throws IOException, NoSuchAlgorithmException{
		switch ((int)message[0]){
			case 0:
				//Choke
			case 1:
				return unchoke(message, designatedPeer, tracker);
			case 2: 
				//Interested
			case 3:
				//Not Interested
			case 4:
				//Have
			case 5:
				return bitfield(message, designatedPeer);
			case 6:
				//Request
			case 7:
				return piece(message, designatedPeer, tracker);
			case 8:
				//Cancel
			case 9:
				//Port
			break;
		}
		return new byte[8];
	}
	
	/*
	 * Given a bitfield message from the peer, it creates a boolean bitfield by splitting
	 * up each byte into four 'boolean' bits. This is stored within the Peer, and updated
	 * upon successful downloading of pieces of the file. 
	 */
	private static byte[] bitfield(byte[] message, Peer designatedPeer){
		//Gives the length of the bitfield in bytes
		int length = message.length - 1;

		//For each byte in the bitfield reach from the file, there are 8 bits.
		boolean[] bitfield = new boolean[(length * 8)];
		for (int i = 1; i < length; i++){
			//THIS MIGHT NOT WORK. I haven't been able to test it fully.
			//Convert each bit into a boolean value, starting with the left most bit.
			//This code adapted from http://stackoverflow.com/questions/3058157/convert-a-byte-into-a-boolean-array-of-length-4-in-java
			bitfield[(i-1) * 8] = ((message[i] & 128) != 0); 
			bitfield[((i-1) * 8) + 1] = ((message[i] & 64) != 0); 
			bitfield[((i-1) * 8) + 2] = ((message[i] & 32) != 0); 
			bitfield[((i-1) * 8) + 3] = ((message[i] & 16) != 0); 
			bitfield[((i-1) * 8) + 4] = ((message[i] & 8) != 0); 
			bitfield[((i-1) * 8) + 5] = ((message[i] & 4) != 0); 
			bitfield[((i-1) * 8) + 6] = ((message[i] & 2) != 0); 
			bitfield[((i-1) * 8) + 7] = ((message[i] & 1) != 0); 
		}
		
		designatedPeer.setBitfield(bitfield);
		//After decoding this bitfield, let the peer know you are interested.
		return INTERESTED;
	}
	
	//After you send your interest, the peer will give you an unchoke message.
	//When the peer unchokes you, you can request your first file.
	private static byte[] unchoke(byte[] message, Peer peer, TrackerInfo tracker) throws IOException{
		return makeRequest(peer, tracker);
	}
	
	/*This method is responsible for finding the first piece you do not have, and requesting it.
	 * It returns a byte array in which has a message requesting that piece. 
	 */
	public static byte[] makeRequest(Peer peer, TrackerInfo tracker) throws IOException{
		//The ByteArrayOutput stream helps so that you can construct a Byte array piece by piece,
		//in conjuction with a DataOutput stream.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		//Create the byte array which will store the request message.
		byte[] request = new byte[17];
		
		//Write length of 13 - the length of a Request message.
		dos.writeInt(13);
		dos.flush();
		
		//Write ID = 6 - what identifies this message as a Request.
		dos.write((byte)6);
		dos.flush();
		
		/*
		 * For the amount of piece hashes, check if that piece has been downloaded from the Peers
		 * bitfield. If it hasn't, send out a request to download that piece.
		 */
		for (int i = 0; i < tracker.getTorrentInfo().piece_hashes.length; i++){
			if (peer.getBitfield()[i] == false){
				//Write the index we want
				dos.writeInt(i);
				dos.flush();
				//Write the offset we want
				dos.writeInt(0);
				dos.flush();
				
				//I'm sorry about the hack, I want to get this working
				/*Write in the length of the piece you want.
				 * If it is the last piece, it will be a smaller size than 32kB.
				 */
				if (i == tracker.getTorrentInfo().piece_hashes.length - 1){
					dos.writeInt(tracker.getTorrentInfo().file_length - (tracker.getTorrentInfo().piece_length)*(tracker.getTorrentInfo().piece_hashes.length - 1));
				}
				else {
					dos.writeInt(32768);
				}
				dos.flush();
				
				//Store the request into a byte array.
				request = baos.toByteArray();
				
				dos.close();
				baos.close();
				return request; 
			}
		}
		
		baos.close();
		dos.close();
		//If each piece has been downloaded, there is nothing to return.
		//An exception will be caught which will let the Download class know to quit. 
		throw new EOFException("Got all pieces");
	}
	
	private static byte[] piece(byte[] message, Peer peer, TrackerInfo tracker) throws IOException, NoSuchAlgorithmException{
		//Store the piece index.
		int index = ByteBuffer.wrap(message, 1, 4).getInt();
		//The beginning index is not really used, but perhaps it will be later. I will store it anyway.
		int begin = ByteBuffer.wrap(message, 5, 4).getInt();
		int piecesize = message.length - 9;
		
		//Create a byte array in which is the size of the piece you are downloading.
		byte[] filepiece = new byte[piecesize];

		//Take the actual data of the piece, and store it. 
		System.arraycopy(message, 9, filepiece, 0, piecesize);
		
		//Check the SHA-1 hash of the piece that is downloaded.
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		digest.update(filepiece);
		byte[] shahash = digest.digest();
	
		//Verify the SHA-1 Hash of the downloaded piece.
		if ( !(ByteBuffer.wrap(shahash).equals(tracker.getTorrentInfo().piece_hashes[index]))){
			throw new IOException("Hash pieces don't match.");
		}
		
		//Mark that this piece has been obtained, and store it within the Metadata.
		peer.getBitfield()[index] = true; 
		tracker.getPieces()[index] = filepiece;
		
		//Return a has message with the index you downloaded, which will be sent to the peer
		//to notify you have the piece. 
		return has(index);
	}
	
	/*
	 * Create a message to send to the peer to let you know you have successfully downloaded that index. 
	 */
	private static byte[] has(int index) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		byte[] has = new byte[9];
		
		//Length of "have" is 4.
		dos.writeInt(5);
		dos.flush();
		
		//ID of "Have" is 4.
		dos.write((byte)4);
		dos.flush();
		
		//Write in that you have that piece.
		dos.writeInt(index);
		dos.flush();
		
		has = baos.toByteArray();
		
		dos.close();
		baos.close();
		
		return has;
		
	}
}
