package btclient;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

import btclient.bencoding.ToolKit;
import btclient.message.BitfieldMessage;
import btclient.message.HaveMessage;
import btclient.message.Message;
import btclient.message.PieceMessage;
import btclient.message.RequestMessage;

/* Download.java
 * 
 * by Nick Paoletti and Daniel Selmon

 * This is the Download class, which given the TrackerInfo (and also the Metadata, so that announce URLs can be
 * constructed), will find the correct peer (Rob), initiate a handshake, and start downloading. It continues to
 * download until all the required pieces have been obtained. 
 */
public class Download implements Runnable{
	
	private static final Logger log = Logger.getLogger(Download.class.getName());
	
	/*
	 * This is the method that will Download the file, given the info held in the tracker.
	 * It first starts by establishing the handshake, then reading and sending messages until
	 * the entire piece has been downloaded. 
	 */
	
	public void run(){
		try {
			downloadFile();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void downloadFile() throws Exception{
		TrackerInfo tracker = FileManager.tracker;
		TorrentInfo info = FileManager.info;
		Peer designatedPeer = findApprovedPeers(tracker);
		designatedPeer.handshake(FileManager.info.info_hash.array(), tracker.getUserPeerId());
		
		//Tell the tracker I started downloading.
		new URL(FileManager.tracker.makeURL(info, "started"));
		
		
		if (FileManager.havePieces){
			designatedPeer.sendMessage(makeBitfield());
		}
		
		
		//Keep looping until file is completed download. 
		while (RUBTClient.keepRunning){
			//System.out.println(designatedPeer + " is going through the loop.");
			try {
				//Grab length prefix and create byte array of that length.
				Message message = designatedPeer.getNextMessageBlocking();
				
				//If the message is not a keep alive, decode it, then send a corresponding message.
				if (message != null){
					switch(message.getType()){
					case Message.TYPE_CHOKE:
						break;
					case Message.TYPE_UNCHOKE:
					{
						System.out.println(designatedPeer + " sent unchoke message.");
						Message temp = makeRequest(designatedPeer, tracker);
						//If you want something, write a Request.Message message = designatedPeer.getNextMessageBlocking();
						if (temp != null){
							designatedPeer.sendMessage(temp);
						}
						break;
					}
					case Message.TYPE_INTERESTED:{
						System.out.println(designatedPeer + " is interested in a piece.");
						designatedPeer.sendMessage(Message.UNCHOKE);
						break;
					}
					case Message.TYPE_UNINTERESTED:
						break;
					case Message.TYPE_HAVE: {
						System.out.println(designatedPeer + " sent a Have Message for Index " + ((HaveMessage)message).getIndex());
						designatedPeer.sendMessage(analyzeHave((HaveMessage)message, designatedPeer));
						break;
					}
					case Message.TYPE_BITFIELD: {
						System.out.println(designatedPeer + " sent a bitfield.");
						designatedPeer.sendMessage(analyzeBitfield(designatedPeer, (BitfieldMessage)message));
						break;
					}
					case Message.TYPE_REQUEST:
					{
						System.out.println(designatedPeer + " requested Piece: " + ((RequestMessage)message).getIndex() + " at offset " + 
								((RequestMessage)message).getOffset());
						PieceMessage pm = makePiece((RequestMessage)message);
						System.out.println("Sending " + designatedPeer + " a piece at Index " + pm.getIndex() + " at offset " + pm.getOffset());
						designatedPeer.sendMessage(pm);
						break;
					}
					case Message.TYPE_PIECE: {
						System.out.println(designatedPeer + " sent piece at Index #" + ((PieceMessage)message).getIndex() + 
								" at offset " + ((PieceMessage)message).getOffset());
						// Decode the piece. Save the file.
						Message hasmsg = savePiece((PieceMessage) message,
								tracker);
						if (hasmsg != null) {
							// designatedPeer.sendMessage(hasmsg);
							for (int i = 0; i < FileManager.approvedPeers.size(); i++) {
								//IF PEERS EXIT, REMOVE THEM FROM APPROVED PEERS!!!!!! YEAH.
								FileManager.approvedPeers.get(i).sendMessage(hasmsg);
								System.out.println("Sending a Have Message to peer " + FileManager.approvedPeers.get(i));
							}

						}

						Message reqmsg = makeRequest(designatedPeer, tracker);
						if (reqmsg != null) {
							designatedPeer.sendMessage(reqmsg);
						}
						break;
					}	
					default:
						log.severe("Unrecognized message type: " + ('0'+message.getType()));
					}
				}
				else {
					//I should have a keep alive message?
				}
			}
			catch (EOFException eof){
				//Let the tracker know the file is completed downloading, and to stop the download.
				new URL(FileManager.tracker.makeURL(info, "completed"));
				new URL(FileManager.tracker.makeURL(info, "stopped"));
				System.out.println("Peer " + designatedPeer + " has closed their stream.");
				FileManager.approvedPeers.remove(designatedPeer);
				designatedPeer.disconnect();
				break;
			}
			catch (SocketException se){
				System.out.println("Error with Peer " + designatedPeer);
				FileManager.approvedPeers.remove(designatedPeer);
				designatedPeer.disconnect();
			}
		}
		//Close input streams. Close socket.
		
		FileManager.approvedPeers.remove(designatedPeer);
		designatedPeer.disconnect();
		
	}
	
	
	private static synchronized PieceMessage makePiece(RequestMessage request) throws IOException {
		if (FileManager.bitfield[request.getIndex()] == true
				&& !(request.getPieceLength() > 131072)) {
			byte[] piece = new byte[request.getPieceLength()];
			FileManager.file.seek((request.getIndex() * (FileManager.tracker.getTorrentInfo().piece_length) + request.getOffset()));
			FileManager.file.readFully(piece);
			FileManager.addUploaded(request.getPieceLength());
			return new PieceMessage(request.getIndex(), request.getOffset(), piece);
		}
		System.out.println("NOT GOOD!!!!!!");
		return null;
	}
	
		
	private static Message analyzeHave(HaveMessage have, Peer designatedPeer){
		boolean interested = false;
		designatedPeer.changeBitfield(have.getIndex(), true);
		for (int i = 0; i < FileManager.bitfield.length; i++){
			if (FileManager.bitfield[have.getIndex()] == false){
				interested = true;
			}
		}
		
		if (interested == false){
			return Message.UNINTERESTED;
		}
		else {
			return Message.INTERESTED;
		}
	}
	
	/*
	 * Returns the peer with the peer that matches the IP address request for the project.
	 */
	private static Peer findApprovedPeers(TrackerInfo tracker) throws Exception{
		System.out.println(tracker.getPeers());
		Peer designatedPeer = null;
		//Look through all peers
		byte[] peerIdToMatch = new byte[]{'R', 'U', 'B', 'T', '1', '1'};
	
		for (int i = 0; i < tracker.getPeers().size(); i++){
			if( (tracker.getPeers().get(i).getIP().equals("128.6.5.130") || tracker.getPeers().get(i).getIP().equals("128.6.5.131") )&& 
					Arrays.equals(Arrays.copyOfRange(tracker.getPeers().get(i).getPeerId(), 0, 6), peerIdToMatch) &&
					tracker.getPeers().get(i).getIsConnected() == false) {
				//Found the peer that is wanted for this part of the project.
				designatedPeer = tracker.getPeers().get(i);
				designatedPeer.changeStatus(true);
				break;
			}
		}
		System.out.println(designatedPeer);
		
		if(designatedPeer == null){
			//The peer wanted for this part of project was not found.
			throw new Exception("Rob's tracker not found. Ya dun goofed.");
		}
		else {
			//The peer wanted for this part of the project was found.
			
		}
		return designatedPeer;
	}

	private static Message analyzeBitfield(Peer designatedPeer, BitfieldMessage bitFieldMessage){
		designatedPeer.newBitfield(bitFieldMessage.getBitfield());
		System.out.println("The bitfield of Peer " + designatedPeer + " is ");
		designatedPeer.printBitfield();
		boolean interested = false;
		for (int i = 0; i < bitFieldMessage.getBitfield().length; i++){
			if (bitFieldMessage.getBitfield()[i] == true && FileManager.bitfield[i] == false){
				interested = true;
			}
		}
		
		if (interested == false){
			log.finer("Not interested in any piece.");
			return Message.UNINTERESTED;
		}
		else {
			log.finer("Interested in a piece.");
			return Message.INTERESTED;
		}
	}
	
	private static Message makeRequest(Peer designatedPeer, TrackerInfo tracker) throws EOFException{ 
		System.out.println("I'm going to try to make a request from " + designatedPeer);
        for (int pieceIndex = 0; pieceIndex < FileManager.bitfield.length; pieceIndex++){
        		//System.out.println("Index #" + pieceIndex + " is " + FileManager.bitfield[pieceIndex] + " on the FileManager and on the Peers bitfield it is " + designatedPeer.getBitfield()[pieceIndex]);
                if (FileManager.bitfield[pieceIndex] == false && designatedPeer.getBitfield()[pieceIndex] == true && (FileManager.isRequested[pieceIndex] == null || Arrays.equals(FileManager.isRequested[pieceIndex].array(), designatedPeer.getPeerId()))){
                		FileManager.isRequested[pieceIndex] = ByteBuffer.wrap(designatedPeer.getPeerId());
                        for (int offsetIndex = 0; offsetIndex < (tracker.getTorrentInfo().piece_length)/(16384); offsetIndex ++){
                                if (FileManager.perPieceBitfield[pieceIndex*(tracker.getTorrentInfo().piece_length)/(16384) + offsetIndex] == false){
                                		
                                        System.out.println("Requesting Subpiece Index # " + offsetIndex + " of Piece # " + pieceIndex);
                                        return new RequestMessage(pieceIndex, offsetIndex*16384, 16384);
                                }
                        }
                }
        }
        throw new EOFException();
        /*
        System.out.println("Nothing to request from " + designatedPeer);
        return null;
        */
	}
	
	private static synchronized Message savePiece(PieceMessage piece, TrackerInfo tracker) throws IOException, NoSuchAlgorithmException{   
        //Mark that this piece has been obtained, and store it within the Metadata.
        FileManager.perPieceBitfield[piece.getIndex() * (tracker.getTorrentInfo().piece_length)/(16384) + (piece.getOffset()/16384)] = true;
        
        //Thanks to Rob for helping me with RAF at this point.
        System.out.println("Writing Bytes " + (piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()) + "-" + (piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()+16384));
        FileManager.file.seek((piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()));
        FileManager.file.write(piece.getPieceData());

        FileManager.addDownloaded(piece.getPieceData().length);
    
        FileManager.bitfield[piece.getIndex()] = true;
        for (int pieceCount = 0; pieceCount < (tracker.getTorrentInfo().piece_length)/(16384); pieceCount++){
                //If we're at the final step, we can't use the bottom statement which will cause OutOfBounds issues.
                try {
                        if (FileManager.perPieceBitfield[piece.getIndex() * (tracker.getTorrentInfo().piece_length)/(16384) + pieceCount] == false){
                                FileManager.bitfield[piece.getIndex()] = false;
                        }
                }
                catch (ArrayIndexOutOfBoundsException aioobe) {
                    //Do nothing because this is the final piece, where there are not 12 subpieces.
                }
        }
        
        //If you downloaded the whole piece, check the SHA-Hash. This still really needs to be updated.
        if (FileManager.bitfield[piece.getIndex()] == true){
        	System.out.println("Full piece downloaded. ");
        	
        	//Take the piece of File to be SHA-1 Hashed.
        	byte[] pieceCheck = new byte[(tracker.getTorrentInfo().piece_length)];
            FileManager.file.seek(piece.getIndex() * (tracker.getTorrentInfo().piece_length));
            FileManager.file.readFully(pieceCheck);
        	
        	FileManager.bitfield[piece.getIndex()] = shaHash(pieceCheck, tracker.getTorrentInfo().piece_hashes[piece.getIndex()].array());
        	
            if (FileManager.bitfield[piece.getIndex()] == true){
            	System.out.println("SHA-Hash successful.");
            	return new HaveMessage(piece.getIndex());
            }
            else {
            	System.out.println("SHA-Hash failed.");
            	//Undownload everything. Scary.
                for (int pieceCount = 0; pieceCount < (tracker.getTorrentInfo().piece_length)/(16384); pieceCount++){
                    //If we're at the final step, we can't use the bottom statement which will cause OutOfBounds issues.
                    try {
                    	FileManager.perPieceBitfield[piece.getIndex() * (tracker.getTorrentInfo().piece_length)/(16384) + pieceCount] = false;
                    }
                    catch (ArrayIndexOutOfBoundsException aioobe) {
                        //Do nothing because this is the final piece, where there are not 12 subpieces.
                    }
                }
            }
        }
        return null;
	}
	
	public static boolean shaHash(byte[] piece, byte[] compare) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
		digest.update(piece);
		byte[] shahash = digest.digest();

		// Verify the SHA-1 Hash of the downloaded piece.
		if (!Arrays.equals(shahash, compare)) {
			return false;
		}
		return true;
	}
	
	private static BitfieldMessage makeBitfield(){
		boolean[] bitfieldToSend = new boolean[FileManager.bitfield.length];
		for (int i = 0; i < FileManager.bitfield.length; i++){
			bitfieldToSend[i] = FileManager.bitfield[i];
		}
		BitfieldMessage bfmsg = new BitfieldMessage(bitfieldToSend);
		return bfmsg;
	}
}