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
		System.out.println(designatedPeer.getIP());
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
						//Send Request.
						break;
					case Message.TYPE_INTERESTED:
						break;
					case Message.TYPE_UNINTERESTED:
						break;
					case Message.TYPE_HAVE:
						break;
					case Message.TYPE_BITFIELD:
						designatedPeer.sendMessage(message);
					case Message.TYPE_REQUEST:
						break;
					case Message.TYPE_PIECE: 
						break;
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

}