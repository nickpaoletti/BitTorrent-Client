package btclient;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Logger;

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
public class Download{
	
	private static final Logger log = Logger.getLogger(Download.class.getName());
	
	/*
	 * This is the method that will Download the file, given the info held in the tracker.
	 * It first starts by establishing the handshake, then reading and sending messages until
	 * the entire piece has been downloaded. 
	 */
	static Peer designatedPeer;
	
	public static void downloadFile(TrackerInfo tracker, Metadata data) throws Exception{
		designatedPeer = findRobsGoodPeer(tracker);
		designatedPeer.handshake(data.torrentData.info_hash.array(), tracker.getUserPeerId());
		
		//Tell the tracker I started downloading.
		new URL(data.makeURL(tracker, "started"));
		
		//Keep looping until file is completed download. 
		while (true){
			try {
				log.fine("Going through message read loop");
				//Grab length prefix and create byte array of that length.
				Message message = designatedPeer.getNextMessageBlocking();
				
				//If the message is not a keep alive, decode it, then send a corresponding message.
				if (message != null){
					switch(message.getType()){
					case Message.TYPE_CHOKE:
						break;
					case Message.TYPE_UNCHOKE:
					{
						System.out.println("They unchoked me those fools");
						Message temp = makeRequest(designatedPeer, tracker);
						//If you want something, write a Request.
						if (temp != null){
							System.out.println("I'm going to make a request! THIS IS FUN");
							designatedPeer.sendMessage(temp);
						}
						break;
					}
					case Message.TYPE_INTERESTED:
						break;
					case Message.TYPE_UNINTERESTED:
						break;
					case Message.TYPE_HAVE:
						break;
					case Message.TYPE_BITFIELD:
						System.out.println("Got a bitfield from em");
						designatedPeer.sendMessage(analyzeBitfield(designatedPeer, (BitfieldMessage)message));
					case Message.TYPE_REQUEST:
						break;
					case Message.TYPE_PIECE:
					{
						System.out.println("Got a piece from em");
						//Decode the piece. Save the file.
						Message hasmsg = savePiece((PieceMessage)message, tracker);
						if (hasmsg != null){
							designatedPeer.sendMessage(hasmsg);
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
					//Keep alive. Do nothing.
				}
			}
			
			catch (EOFException eof){
				//Let the tracker know the file is completed downloading, and to stop the download.
				new URL(data.makeURL(tracker, "completed"));
				new URL(data.makeURL(tracker, "stopped"));
				System.out.println("File Complete");
				break;
			}	
		}
		//Close input streams. Close socket.
		
		designatedPeer.disconnect();
		
	}
		
	
	/*
	 * Returns the peer with the peer that matches the IP address request for the project.
	 */
	private static Peer findRobsGoodPeer(TrackerInfo tracker) throws Exception{
		Peer designatedPeer = null;
		//Look through all peers
		byte[] peerIdToMatch = new byte[]{'R', 'U', 'B', 'T', '1', '1'};

		for (int i = 0; i < tracker.getPeers().size(); i++){
			if( tracker.getPeers().get(i).getIP().equals("128.6.5.130") && 
					Arrays.equals(Arrays.copyOfRange(tracker.getPeers().get(i).getPeerId(), 0, 6), peerIdToMatch)) {
				//Found the peer that is wanted for this part of the project.
				designatedPeer = tracker.getPeers().get(i);
			}
		}
		
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
	
	private static Message makeRequest(Peer designatedPeer, TrackerInfo tracker){ 
		System.out.println("I'm going to try to make a request.");
        for (int pieceIndex = 0; pieceIndex < FileManager.bitfield.length; pieceIndex++){
        		System.out.println("Index #" + pieceIndex + " is " + FileManager.bitfield[pieceIndex] + " on the FileManager and on the Peers bitfield it is " + designatedPeer.getBitfield()[pieceIndex]);
                if (FileManager.bitfield[pieceIndex] == false && designatedPeer.getBitfield()[pieceIndex] == true){
                        for (int offsetIndex = 0; offsetIndex < (tracker.getTorrentInfo().piece_length)/(16384); offsetIndex ++){
                                if (FileManager.perPieceBitfield[pieceIndex*(tracker.getTorrentInfo().piece_length)/(16384) + offsetIndex] == false){
                                        System.out.println("Requesting Subpiece Index # " + offsetIndex + " of Piece # " + pieceIndex);
                                        return new RequestMessage(pieceIndex, offsetIndex*16384, 16384);
                                }
                        }        
                }
        }
        return null;
	}
	
	private static Message savePiece(PieceMessage piece, TrackerInfo tracker) throws IOException{   
        /* SHA CODE - VERY IMPORTANT!!!! WOOO!!!!!!
         * //Check the SHA-1 hash of the piece that is downloaded.
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                digest.update(filepiece);
                byte[] shahash = digest.digest();
        
                System.out.println("Sha hash of downloaded piece:");
                ToolKit.printString(shahash, false, 0);
                System.out.println("Sha hash of existing piece:");
                ToolKit.printString(tracker.getTorrentInfo().piece_hashes[index], false, 0);
                
                //Verify the SHA-1 Hash of the downloaded piece.
                if(!Arrays.equals(shahash,tracker.getTorrentInfo().piece_hashes[index].array())){
                        throw new IOException("Hash pieces don't match.");
                }
         */
        
        
        //Mark that this piece has been obtained, and store it within the Metadata.
        FileManager.perPieceBitfield[piece.getIndex() * (tracker.getTorrentInfo().piece_length)/(16384) + (piece.getOffset()/16384)] = true;
        
        //Thanks to Rob for helping me with RAF at this point.
        System.out.println("Writing Bytes " + (piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()) + "-" + (piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()+16384));
        FileManager.file.seek((piece.getIndex()*(tracker.getTorrentInfo().piece_length)+piece.getOffset()));
        FileManager.file.write(piece.getPieceData());

        //[(index * (tracker.getTorrentInfo().piece_length)/(16384))] = filepiece;
        
        FileManager.bitfield[piece.getIndex()] = true;
        for (int pieceCount = 0; pieceCount < (tracker.getTorrentInfo().piece_length)/(16384); pieceCount++){
                //If we're at the final step, we can't use the bottom statement which will cause OutOfBounds issues.
                try {
                        if (FileManager.perPieceBitfield[piece.getIndex() * (tracker.getTorrentInfo().piece_length)/(16384) + pieceCount] == false){
                                FileManager.bitfield[piece.getIndex()] = false;
                        }
                }
                catch (ArrayIndexOutOfBoundsException aioobe) {
                    //Do nothing. These pieces are pointless.
                }
        
        }
        
        if (FileManager.bitfield[piece.getIndex()] == true){
        	return new HaveMessage(piece.getIndex());
        }
        
        return null;
	}
}