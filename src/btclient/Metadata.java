package btclient;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.logging.Logger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import btclient.bencoding.Bencoder2;
import btclient.bencoding.BencodingException;
import btclient.message.HaveMessage;

/* Metadata.java
 * 
 * by Nick Paoletti and Daniel Selmon
 * Big thanks to Dan on helping me clean this up a lot. - Nick
 * 
 * This is the Metadata class, which creates a Metadata object, which stores one field only - the torrent data.
 * Using this data, the HTTP GET request is made. The information from the tracker is then decoded using
 * Bencoder2.java, and stored in a convenient TrackerInfo object, which will be used throughout the rest of the
 * program to have the information necessary to download the file.
 */

public class Metadata {
	private static final Logger log = Logger.getLogger(Metadata.class.getName());
	
	//public static TorrentInfo torrentData;
	
	public static TorrentInfo makeTorrentInfo (File torrent) throws IOException, BencodingException{
		//Learned how to do this from http://www.exampledepot.com/egs/java.io/file2bytearray.html
		InputStream fileRead = new FileInputStream(torrent);
		
		//Store length of the file in integer form.
		int fileLength = (int)torrent.length();
		
		log.finest("Metadata file length: " + fileLength + " bytes.");
		
		
		//Create byte array with the same length of the .torrent file.
		byte[] torrentBytes = new byte[fileLength];
		DataInputStream din = new DataInputStream(fileRead);
		din.readFully(torrentBytes);
		
		//Close input stream
	    din.close();
	    
	    //Return byte array.
	    return new TorrentInfo(torrentBytes);
	}

	//Takes in a ByteBuffer in as its argument and returns a String of the escaped info hash.
	public static String returnInfoHash(ByteBuffer infohash){
		//Turn the ByteBuffer into a Byte Array
		byte[] temp = infohash.array();
		
		//Create a String made of each character in the byte array formatted as hexadecimal
		String s = "";
		for (int i = 0; i < 20; i++){
			s = s + "%" + String.format("%02X", temp[i]);
		}
		return s;
	}
	
	//Create a randomly generated 20 digit Peer ID with prefix "SWAG" to denote the client.
	private static byte[] makePeerID(){
		byte[] peerid = new byte[20];
		peerid[0] = 'S';
		peerid[1] = 'W';
		peerid[2] = 'A';
		peerid[3] = 'G';
		
		Random rand = new Random(System.currentTimeMillis());
		
		for (int i = 4; i < peerid.length; i++){
			peerid[i] = (byte)(rand.nextInt(26) + 'A');
		}

		return peerid;
	}
	
	//Creates announce URLs based on the state of the program
	
	
	/* This method is responsible for establishing the HTTP GET request, but more importantly,
	 * taking the bencoded information obtained from it, decoding it, and storing it in an easier
	 * to understand TrackerInfo object. 
	 */
	public static TrackerInfo httpGetRequest(TorrentInfo torrentData) throws MalformedURLException, IOException, BencodingException{
		//Create new TrackerInfo class
		TrackerInfo tracker = new TrackerInfo();
		//Pass in the torrentData to this new tracker. A bad hack...
		tracker.setTorrentInfo(torrentData);
		log.finest("Size of each file piece: " +  tracker.getTorrentInfo().piece_length);
		//Lets the Tracker know your Peer ID. Useful in the handshake.
		tracker.setUserPeerId(makePeerID());
		
		//Open up a new URL connection.
		URL url = new URL(torrentData.announce_url +  "?info_hash=" + returnInfoHash(torrentData.info_hash)
				+ "&peer_id="+ tracker.getUserPeerId() + "&port=6881" + "&uploaded=0" + "&downloaded=0" + 
				"&left=" + torrentData.file_length);
		URLConnection urlc = url.openConnection();
		
		//Obtain the bencoded dictionary obtained from making the HTTP Request.
		//Thank you Rob for going over this method of obtaining the data.
		int contentLength = urlc.getContentLength();
		byte[] urlcontent = new byte[contentLength];
		DataInputStream dis = new DataInputStream(urlc.getInputStream());
		dis.readFully(urlcontent);
		dis.close();
		
		//Decode the bencoded information held within the GET request and store the 'dictionary' into a Map.
		HashMap<ByteBuffer, Object> dictionary = (HashMap<ByteBuffer, Object>) Bencoder2.decode(urlcontent);
		//Look through each key in the dictionary and save its value to the TrackerInfo object.
	    for(ByteBuffer key : dictionary.keySet()) {
			String keyAsString = new String(key.array());	
	    	if(keyAsString.equals("incomplete")){
				tracker.setIncomplete((Integer)dictionary.get(key));
			}
			if(keyAsString.equals("downloaded")){
				tracker.setDownloaded((Integer)dictionary.get(key));
			}
			if(keyAsString.equals("complete")){
				tracker.setComplete((Integer)dictionary.get(key));
			}
			if(keyAsString.equals("mininterval")){
				tracker.setMinInterval((Integer)dictionary.get(key));
			}
			if(keyAsString.equals("interval")){
				tracker.setInterval((Integer)dictionary.get(key));
			}
			/*The special case:
			 * The list of peers is stored as an ArrayList of Maps within the dictionary which stores the information
			 * about the tracker. Go through this array list, and for each peer in the list, go through their 
			 * dictionary, and take that info and store it into a Peer object. Place that Peer object into the 
			 * TrackerInfo objects ArrayList of Peers.
			 */
			if(keyAsString.equals("peers")){
				FileManager.approvedPeers = new ArrayList<Peer>();
				int port = 0;
				String ip = "";
				byte[] peerid = new byte[20];
				ArrayList<HashMap<ByteBuffer, Object>> peerList = (ArrayList<HashMap<ByteBuffer, Object>>)dictionary.get(key);
				for(HashMap<ByteBuffer, Object> entry : peerList){
					for(ByteBuffer b : entry.keySet()){
						String temp = new String(b.array());
						if(temp.equals("port")){
							port = (Integer) entry.get(b);
						}
						if(temp.equals("ip")){
							ip = new String(((ByteBuffer) entry.get(b)).array());
						}
						if(temp.equals("peer id")){
							peerid = ((ByteBuffer) entry.get(b)).array();
						}
					}
					Peer peerToAdd = new Peer(ip, port, peerid, torrentData.piece_hashes.length);
					tracker.getPeers().add(peerToAdd);
					//GET APPROVED PEERS
					if (peerToAdd.getIP().equals("128.6.5.131") || peerToAdd.getIP().equals("128.6.5.130")){
						FileManager.approvedPeers.add(peerToAdd);
					}
					
				}
			}
	    }
	    return tracker;
	}
}