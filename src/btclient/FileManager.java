package btclient;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.Scanner;

import btclient.message.HaveMessage;

public class FileManager{
	//This array keeps track of how many pieces you have downloaded:
	public static boolean[] bitfield;
	public static TorrentInfo info;
	public static TrackerInfo tracker;

	public static boolean[] perPieceBitfield;
	public static ByteBuffer[] isRequested;
	public static RandomAccessFile file;
	public static ArrayList<Peer> approvedPeers;
	public static boolean havePieces;
	
	//File writing code obtained from http://www.roseindia.net/java/beginners/java-write-to-file.shtml
	public static void storeFileProgress(String filename) throws IOException {
		FileWriter write = new FileWriter(filename.substring(0, filename.lastIndexOf(".mp3")) + "PROGRESS.txt");
		BufferedWriter out = new BufferedWriter(write);
		for (int i = 0; i < bitfield.length; i++){
			if (bitfield[i] == true){
				out.write('1');
			}
			else {
				out.write('0');
			}
		}
		out.close();
		write.close();
	}
	
	public static void readFileProgress(String filename) throws IOException, NoSuchAlgorithmException {
		FileManager.havePieces = true;
		FileInputStream fileRead = new FileInputStream(new File(filename.substring(0, filename.lastIndexOf(".mp3")) + "PROGRESS.txt"));
		int indexCounter = 0;
		while(fileRead.available() > 0){
			if((char)fileRead.read() == '1'){
				bitfield[indexCounter] = true;
				
				//MUST FIX HOW I HANDLE METADATA, TRACKER INFO, ETC.
				
				/*
				//Verify each piece.
				byte[] pieceCheck = new byte[data.torrentData.piece_length];
	            FileManager.file.seek(indexCounter * (data.torrentData.piece_length));
	            FileManager.file.readFully(pieceCheck);
	            
	            FileManager.bitfield[indexCounter] = Download.shaHash(pieceCheck, data.torrentData.piece_hashes[indexCounter].array());
	            */
			}
			else {
				bitfield[indexCounter] = false;
			}
			indexCounter++;
		}
		fileRead.close();

	}
	
	public static void initializeFields(){
		int numpieces = (int) Math.ceil(info.file_length/16384);
		FileManager.bitfield = new boolean[info.piece_hashes.length];
		FileManager.perPieceBitfield = new boolean[numpieces];
		FileManager.isRequested = new ByteBuffer[numpieces];
	}
			
}