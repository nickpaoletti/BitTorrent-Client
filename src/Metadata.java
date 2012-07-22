import java.io.*;
import java.nio.*;
import java.util.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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
	public static TorrentInfo torrentData;
	
	public Metadata(File torrent) throws IOException, BencodingException{
		//Learned how to do this from http://www.exampledepot.com/egs/java.io/file2bytearray.html
		InputStream fileRead = new FileInputStream(torrent);
		
		//Store length of the file in integer form.
		int fileLength = (int)torrent.length();
		//Create byte array with the same length of the .torrent file.
		byte[] torrentBytes = new byte[fileLength];
		
		//Keep track of how many bytes have been read in. 
		//Offset determines how far into the file you are, while readBytes reads parts of the file a piece at a time
		int readBytes = 0, offset = 0;
		
		//Keeps reading from the file until the whole thing has been read through.
		//the fileRead.read line in the while statement will read the torrent file byte by byte and place it into the byte array 
		while (offset < fileLength
		           && (readBytes=fileRead.read(torrentBytes, offset, torrentBytes.length-offset)) >= 0) {
				offset += readBytes;
		}

		//Change the below, I copy pasted it
		// Ensure all the bytes have been read in
	    if (offset < torrentBytes.length) {
	    	fileRead.close();
	        throw new IOException("Could not completely read file "+torrent.getName());
	    }
	
	    //Close input stream
	    fileRead.close();
	    
	    //Return byte array.
		torrentData = new TorrentInfo(torrentBytes);
	}
	
	
	//Takes in a ByteBuffer in as its argument and returns a String of the escaped info hash.
	private String returnInfoHash(ByteBuffer infohash){
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
	private String makePeerID(){
		Random rand = new Random();
		String peerid = "SWAG";
		for (int i = 0; i < 16; i++){
			peerid = peerid + Integer.toHexString(rand.nextInt(0xF));
		}

		return peerid;
	}
	
	//Creates announce URLs based on the state of the program
	public String makeURL(TrackerInfo tracker, String state){
		String url = "";
		//Used for first announce and the subsequent HTTP GET request.
		if (state.equals("starting")){
			url = url + torrentData.announce_url +  "?info_hash=" + returnInfoHash(torrentData.info_hash)
					+ "&peer_id="+ tracker.getUserPeerId() + "&port=6881" + "&uploaded=0" + "&downloaded=0" + 
					"&left=" + torrentData.file_length;
		}
		//Used when the download is started.
		else if (state.equals("started")){
			url = url + torrentData.announce_url +  "?info_hash=" + returnInfoHash(torrentData.info_hash)
					+ "&peer_id="+ tracker.getUserPeerId() + "&port=6881" + "&uploaded=0" + "&downloaded=0" + 
					"&left=" + torrentData.file_length + "&event=started";
		}
		//Use right when the download completes
		else if (state.equals("completed")){
			url = url + torrentData.announce_url +  "?info_hash=" + returnInfoHash(torrentData.info_hash)
				+ "&peer_id="+ tracker.getUserPeerId() + "&port=6881" + "&uploaded=0" + "&downloaded=0" + 
				"&left=" + torrentData.file_length + "&event=completed";
		}
		//Use right after the download completes and complete notice is sent.
		else if (state.equals("stopped")){
			url = url + torrentData.announce_url +  "?info_hash=" + returnInfoHash(torrentData.info_hash)
				+ "&peer_id="+ tracker.getUserPeerId() + "&port=6881" + "&uploaded=0" + "&downloaded=0" + 
				"&left=" + torrentData.file_length + "&event=stopped";
		}
		return url;
		
	}
	
	/* This method is responsible for establishing the HTTP GET request, but more importantly,
	 * taking the bencoded information obtained from it, decoding it, and storing it in an easier
	 * to understand TrackerInfo object. 
	 */
	public TrackerInfo httpGetRequest() throws MalformedURLException, IOException, BencodingException{
		//Create new TrackerInfo class
		TrackerInfo tracker = new TrackerInfo();
		//Pass in the torrentData to this new tracker. A bad hack...
		tracker.setTorrentInfo(torrentData);
		//Lets the Tracker know your Peer ID. Useful in the handshake.
		tracker.setUserPeerId(makePeerID());
		//Fills up the byte array which will contain the downloaded pieces to have [# of pieces]
		//pieces of size 32kB.
		tracker.setPieces(new byte[tracker.getTorrentInfo().piece_hashes.length][32768]);
		
		//Open up a new URL connection.
		URL url = new URL(makeURL(tracker, "starting"));
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
				Integer port = 0;
				String ip = "";
				String peerid = "";
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
							peerid = new String(((ByteBuffer) entry.get(b)).array());
						}
					}
					tracker.getPeers().add(new Peer(ip, peerid, port));
				}
			}
	    }
	    return tracker;
	}
}