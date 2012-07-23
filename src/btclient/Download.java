package btclient;
import java.io.*;
import java.net.*;

/* Download.java
 * 
 * by Nick Paoletti and Daniel Selmon

 * This is the Download class, which given the TrackerInfo (and also the Metadata, so that announce URLs can be
 * constructed), will find the correct peer (Rob), initiate a handshake, and start downloading. It continues to
 * download until all the required pieces have been obtained. 
 */
public class Download{
	/*
	 * This is the method that will Download the file, given the info held in the tracker.
	 * It first starts by establishing the handshake, then reading and sending messages until
	 * the entire piece has been downloaded. 
	 */
	public static void downloadFile(TrackerInfo tracker, Metadata data) throws Exception{
		//Make the socket needed and create DataInput and Output streams
		Peer designatedPeer = findPeer(tracker);
		Socket socket = new Socket(designatedPeer.getIP(), designatedPeer.getPort());
		DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
		DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
		
		/*MAKE HANDSHAKE*/
		//Some code here taken from Ernest-Friedman Hill @ http://www.coderanch.com/t/397984/java/java/we-store-integer-byte-array
		//From here is where I got the idea of a ByteArrayOutputStream (quite convenient!), which I use throughout my code. 
		byte[] clientHandshake;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		//Write in String length and String Idenitifer
		dos.write((byte)19);
		dos.flush();
		dos.writeBytes("BitTorrent protocol");
		dos.flush();
		//Write 8 blank bytes.
		dos.write(new byte[8]);
		dos.flush();
		//Writes in the info hash & peer id.
		dos.write(tracker.getTorrentInfo().info_hash.array());
		dos.flush();
		dos.writeBytes(tracker.getUserPeerId());
		dos.flush();
		
		//Write in these 68 bytes into our array. Awesome.
		clientHandshake = baos.toByteArray();
		baos.close();
		dos.close();
		
		//Send the handshake.
		dataOutputStream.write(clientHandshake);
		//"Always flush yo shit" - Mahatma Gandhi, 2012
		dataOutputStream.flush();
		//Read in the hosts handshake.
		byte[] hostHandshake = new byte[68];
		dataInputStream.readFully(hostHandshake);

		//Check the info hashes
		for (int i = 28; i < 48; i++){
			if (hostHandshake[i] != clientHandshake[i]){
				throw new Exception("Info hashes don't match when receiving handshake.");
			}
		} 
		//Should check Peer ID
		
		//Tell the tracker I started downloading.
		new URL(data.makeURL(tracker, "started"));
		
		//Keep looping until file is completed download. 
		while (true){
			try {
				//Grab length prefix and create byte array of that length.
				int messageLength = dataInputStream.readInt();

				//Read in the rest of the message, of length given by the message.
				byte[] message = new byte[messageLength];
				dataInputStream.readFully(message);

				//If the message is not a keep alive, decode it, then send a corresponding message.
				if (message.length != 0){
					dataOutputStream.write(Message.decode(message, designatedPeer, tracker));
					dataOutputStream.flush();
					/*If the message was a piece you received, the above dataOutputStream.write will
					 * write a "Has" message to let the peer know you have the file. You then will request
					 * the next file.
					 */
					if((int)message[0] == 7){
						dataOutputStream.write(Message.makeRequest(designatedPeer, tracker));
						dataOutputStream.flush();
						
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
		dataInputStream.close();
		dataOutputStream.close();
		socket.close();
		
	}
		
	
	/*
	 * Returns the peer with the peer that matches the IP address request for the project.
	 */
	private static Peer findPeer(TrackerInfo tracker) throws Exception{
		Peer designatedPeer = null;
		//Look through all peers
		for (int i = 0; i < tracker.getPeers().size(); i++){
			if(tracker.getPeers().get(i).getIP().equals("128.6.5.130") && tracker.getPeers().get(i).getPeerId().substring(0, 6).equals("RUBT11")){
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