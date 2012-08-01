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
		TrackerInfo tracker = FileManager.tracker;
		TorrentInfo info = FileManager.info;
		//Find the first approved peer that I have not connected to yet.
		Peer designatedPeer;
		
		designatedPeer = null;
		try {
			designatedPeer = findApprovedPeers(tracker);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			designatedPeer.handshake(FileManager.info.info_hash.array(), tracker.getUserPeerId());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Tell the tracker I started downloading.
		try {
			new URL(FileManager.tracker.makeURL(info, "started"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//If I am resuming the download, send the peer a bitfield of what I have.
		if (FileManager.havePieces){
			try {
				designatedPeer.sendMessage(makeBitfield());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		//Keep looping until file is completed download. 
		while (RUBTClient.keepRunning){
			try {
				//Read a message from the input stream.
				Message message;

				message = designatedPeer.getNextMessageBlocking();

				//If the message is not a keep alive, decode it, then send a corresponding message.
				if (message != null){
					switch(message.getType()){
					case Message.TYPE_CHOKE:
						break;
					//If the peer unchokes me, I can now make a request.
					case Message.TYPE_UNCHOKE:
					{
						System.out.println(designatedPeer + " sent unchoke message.");
						Message temp = makeRequest(designatedPeer, tracker);
						//temp will not be null if there is a piece I want. In this event, send a request.
						if (temp != null) {

							designatedPeer.sendMessage(temp);

						}
						break;
					}
					//If a peer is interested in one of my pieces, unchoke them.
					case Message.TYPE_INTERESTED:{
						System.out.println(designatedPeer + " is interested in a piece.");
						designatedPeer.sendMessage(Message.UNCHOKE);
						break;
					}
					//If a peer is not interested in one of pieces, do nothing for now.
					case Message.TYPE_UNINTERESTED:
						break;
					//If a peer sends a have message, mark in their bitfield that they have it.
					case Message.TYPE_HAVE: {
						System.out.println(designatedPeer + " sent a Have Message for Index " + ((HaveMessage)message).getIndex());
						designatedPeer.sendMessage(analyzeHave((HaveMessage)message, designatedPeer));
						break;
					}
					//If a peer sends a bitfield, modify their bitfield to correspond to the one sent.
					case Message.TYPE_BITFIELD: {
						System.out.println(designatedPeer + " sent a bitfield.");
						designatedPeer.sendMessage(analyzeBitfield(designatedPeer, (BitfieldMessage)message));
						break;
					}
					//If the peer sends a request, give them the piece they requested.
					case Message.TYPE_REQUEST:
					{
						System.out.println(designatedPeer + " requested Piece: " + ((RequestMessage)message).getIndex() + " at offset " + 
								((RequestMessage)message).getOffset());
						PieceMessage pm = makePiece((RequestMessage)message);
						System.out.println("Sending " + designatedPeer + " a piece at Index " + pm.getIndex() + " at offset " + pm.getOffset());
						designatedPeer.sendMessage(pm);
						break;
					}
					//If the peer sends you a subpiece, save the piece and notify ALL peers you are connected to you have it if you have completed the whole piece..
					case Message.TYPE_PIECE: {
						System.out.println(designatedPeer + " sent piece at Index #" + ((PieceMessage)message).getIndex() + 
								" at offset " + ((PieceMessage)message).getOffset());
						// Decode the piece. Save the file.
						Message hasmsg = savePiece((PieceMessage) message,tracker);
						//Go through your list of peers you are connected to, and let them all know you have that piece.
						if (hasmsg != null) {
							for (int i = 0; i < FileManager.approvedPeers.size(); i++) {
								FileManager.approvedPeers.get(i).sendMessage(hasmsg);
								System.out.println("Sending a Have Message to peer " + FileManager.approvedPeers.get(i));
							}

						}
						//Upon receiving a piece, request a new one.
						Message reqmsg = makeRequest(designatedPeer, tracker);
						if (reqmsg != null) {
							designatedPeer.sendMessage(reqmsg);
						}
						
						break;
						
					}	
					default:
						//Unrecognized message type sent.
						log.severe("Unrecognized message type: " + ('0'+message.getType()));
					}
				}
				else {
					//Peer has sent a keep alive - do nothing.
				}
			}
			//In this event, the Socket has been closed, usually cleanly as you are no longer
			//downloading from the peer and they have no interest in any of your pieces.
			catch (EOFException eof){
				//Let the tracker know you are done. 
				try {
					new URL(FileManager.tracker.makeURL(info, "completed"));
					new URL(FileManager.tracker.makeURL(info, "stopped"));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println("Peer " + designatedPeer + " has closed their stream.");
				//Remove this peer from the list of peers we are downloading from, and disconnect them.
				FileManager.approvedPeers.remove(designatedPeer);
				designatedPeer.disconnect();
				break;
			}
			//This peer threw and error, so disconnect them.
			catch (SocketException se){
				System.out.println("Error with Peer " + designatedPeer);
				FileManager.approvedPeers.remove(designatedPeer);
				designatedPeer.disconnect();
			}
			//Accounting for various Exceptions.
			catch (MalformedURLException e){
				FileManager.approvedPeers.remove(designatedPeer);
				designatedPeer.disconnect();
			}
			catch (IOException ioe){
				FileManager.approvedPeers.remove(designatedPeer);
				designatedPeer.disconnect();
			}
			catch (NoSuchAlgorithmException nsae){
				FileManager.approvedPeers.remove(designatedPeer);
				designatedPeer.disconnect();
			}
			
		}
		//On the event the program is exited, close input streams and close socket.
		FileManager.approvedPeers.remove(designatedPeer);
		designatedPeer.disconnect();
	
	}
	
	
	private static synchronized PieceMessage makePiece(RequestMessage request) throws IOException {
		//Make sure the peer sent a piece under 128kB. Otherwise you have a protocol exception.
		if (FileManager.bitfield[request.getIndex()] == true
				&& !(request.getPieceLength() > 131072)) {
			//Seek to the position in your file for the piece you have, grab the bytes, and send them.
			byte[] piece = new byte[request.getPieceLength()];
			FileManager.file.seek((request.getIndex() * (FileManager.tracker.getTorrentInfo().piece_length) + request.getOffset()));
			FileManager.file.readFully(piece);
			//Note how many bytes you have uploaded.
			FileManager.addUploaded(request.getPieceLength());
			return new PieceMessage(request.getIndex(), request.getOffset(), piece);
		}
		throw new IOException("A peer requested too large a piece.");
	}
	
		
	private static Message analyzeHave(HaveMessage have, Peer designatedPeer){
		boolean interested = false;
		//Mark in the peers bitfield they have the piece.
		designatedPeer.changeBitfield(have.getIndex(), true);
		//Look through your bitfield. If you do not have this piece, let the peer know you are interested in it.
		for (int i = 0; i < FileManager.bitfield.length; i++){
			if (FileManager.bitfield[have.getIndex()] == false){
				interested = true;
			}
		}
		//If you aren't interested in the piece, say you are uninterested. If you are interested, say you are.
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
	
		//Look for all peers that have one of Rob's IPs and prefix RUBT11.
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
		//Save the bitfield into the peer.
		designatedPeer.newBitfield(bitFieldMessage.getBitfield());
		//Display the Peer's bitfield.
		System.out.println("The bitfield of Peer " + designatedPeer + " is ");
		designatedPeer.printBitfield();
		
		//Check through the peer's bitfield and see if there are any pieces they have that you do not.
		//If this is true, then you are interested in one of their pieces.
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
		System.out.println("Making a request from " + designatedPeer);
		//Look through every piece on your bitfield and see which ones you do not fully have.
        for (int pieceIndex = 0; pieceIndex < FileManager.bitfield.length; pieceIndex++){
        		//If you do not have this piece, and there is not another one of your threads currently downloading it from another peer:
                if (FileManager.bitfield[pieceIndex] == false && designatedPeer.getBitfield()[pieceIndex] == true && (FileManager.isRequested[pieceIndex] == null || Arrays.equals(FileManager.isRequested[pieceIndex].array(), designatedPeer.getPeerId()))){
                		//Mark that you are downloading this piece from the peer this thread is connected to, so that other threads will not request it.
                		FileManager.isRequested[pieceIndex] = ByteBuffer.wrap(designatedPeer.getPeerId());
                		//For every subpiece of the piece you are trying to download it, see if you have it.
                		//Request the piece you do not have.
                        for (int offsetIndex = 0; offsetIndex < (tracker.getTorrentInfo().piece_length)/(16384); offsetIndex ++){
                                if (FileManager.perPieceBitfield[pieceIndex*(tracker.getTorrentInfo().piece_length)/(16384) + offsetIndex] == false){
                                        System.out.println("Requesting Subpiece Index # " + offsetIndex + " of Piece # " + pieceIndex);
                                        return new RequestMessage(pieceIndex, offsetIndex*16384, 16384);
                                }
                        }
                }
        }
        //There is nothing you want from this peer.
        return null;
	}
	
	private static synchronized Message savePiece(PieceMessage piece, TrackerInfo tracker) throws IOException, NoSuchAlgorithmException{   
        //Mark that this subpiece has been obtained, and store it within the per piece bitfield, used only by the program for making requests.
        FileManager.perPieceBitfield[piece.getIndex() * (tracker.getTorrentInfo().piece_length)/(16384) + (piece.getOffset()/16384)] = true;
        
        //Thanks to Rob for helping me with RandomAccessFile at this point. He used his sagely ways to tell me the art of the seek.
        System.out.println("Writing Bytes " + (piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()) + "-" + (piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()+16384));
        FileManager.file.seek((piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()));
        FileManager.file.write(piece.getPieceData());

        //Mark the amount of bytes you have downloaded to your tracker total.
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
		//Apply the SHA-Hash to the unhashed piece of the array. 
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
		digest.update(piece);
		byte[] shahash = digest.digest();

		// Verify the SHA-1 Hash of the downloaded piece.
		if (!Arrays.equals(shahash, compare)) {
			//If they are not equal, return false.
			return false;
		}
		//If they are equal, return true.
		return true;
	}
	

	private static BitfieldMessage makeBitfield(){
		boolean[] bitfieldToSend = new boolean[FileManager.bitfield.length];
		for (int i = 0; i < FileManager.bitfield.length; i++){
			bitfieldToSend[i] = FileManager.bitfield[i];
		}
		BitfieldMessage bfmsg = new BitfieldMessage(FileManager.bitfield);
		return bfmsg;
	}
}