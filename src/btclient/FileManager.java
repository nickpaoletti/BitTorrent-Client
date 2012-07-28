package btclient;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class FileManager{
	//This array keeps track of how many pieces you have downloaded:
	public static boolean[] bitfield;
	static Metadata data;
	static TrackerInfo tracker;

	public static boolean[] perPieceBitfield;
	public static ByteBuffer[] isRequested;
	public static RandomAccessFile file;
	public static ArrayList<Peer> approvedPeers;
	
	
	//File writing code obtained from http://www.roseindia.net/java/beginners/java-write-to-file.shtml
	public static void storeFileProgress(String filename) throws IOException {
		FileWriter write = new FileWriter(filename + "PROGRESS");
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
	}
	
}