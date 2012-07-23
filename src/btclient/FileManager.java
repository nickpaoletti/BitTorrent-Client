package btclient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileManager{
	//This array keeps track of how many pieces you have downloaded:
	public static boolean[] bitfield;
	
	//This 2D byte array has x rows, where x is the amount of pieces in the torrent.
	//This can be found in the TorrentInfo = key_piece_hashes.length
	//For each row, there is a byte array the size of the length of each piece, in this case 32kB.
	public static byte[][] pieces;
	public static boolean[] perPieceBitfield;
	
	//Merge together the pieces of the 2D byte array to write the File you downloaded.
	public static void makeFile(String filename) throws IOException{
		File nick = new File(filename);
		FileOutputStream file = new FileOutputStream(nick);
		for (int i = 0; i < pieces.length; i ++){
			file.write(pieces[i]);
		}
		file.close();
	}
}