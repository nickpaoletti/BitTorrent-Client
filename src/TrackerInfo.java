import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/* TrackerInfo.java
 * 
 * by Nick Paoletti and Daniel Selmon
 * 
 * This is the TrackerInfo object. This object stores the Metadata from the .torrent file in a very
 * easy to understand fashion. It also stores the currently downloaded pieces of the file specified
 * in the .torrent file, and has a method to merge them into a File object.
 */

public class TrackerInfo{
	//Fields stored within the tracker info, corresponding to the values found in a HTTP GET request.
	private TorrentInfo mTorrentInfo;
	private String mUserPeerId;
	private int mIncomplete, mDownloaded, mComplete, mMinInterval, mInterval;
	private ArrayList<Peer> mPeers;
	
	//This 2D byte array has x rows, where x is the amount of pieces in the torrent.
	//This can be found in the TorrentInfo = key_piece_hashes.length
	//For each row, there is a byte array the size of the length of each piece, in this case 32kB.
	private byte[][] mPieces;
	
	//Create the TrackerInfo object by initializing the list of peers.
	public TrackerInfo(){
		mPeers = new ArrayList<Peer>();
	}
	//Merge together the pieces of the 2D byte array to write the File you downloaded.
	public void makeImage(String filename) throws IOException{
		File nick = new File(filename);
		FileOutputStream file = new FileOutputStream(nick);
		for (int i = 0; i < mPieces.length; i ++){
			file.write(mPieces[i]);
		}
		file.close();
	}
	//These methods are used in Metadata to read in information from the tracker and store it
	//within a TrackerInfo object.
	public void setTorrentInfo(TorrentInfo info) {
		mTorrentInfo = info;
	}
	public void setUserPeerId(String userpeerid) {
		mUserPeerId = userpeerid;
	}
	public void setIncomplete(int incomplete) {
		mIncomplete = incomplete;
	}
	public void setDownloaded(int downloaded) {
		mDownloaded = downloaded;
	}
	public void setComplete(int complete) {
		mComplete = complete;
	}
	public void setMinInterval(int mininterval) {
		mMinInterval = mininterval;
	}
	public void setInterval(int interval) {
		mInterval = interval;
	}
	public void setPieces(byte[][] pieces) {
		mPieces = pieces;
	}
	public TorrentInfo getTorrentInfo() {
		return mTorrentInfo;
	}
	public String getUserPeerId() {
		return mUserPeerId;
	}
	public int getIncomplete() {
		return mIncomplete;
	}
	public int getDownloaded() {
		return mDownloaded;
	}
	public int getComplete() {
		return mComplete;
	}
	public int getMinInterval() {
		return mMinInterval;
	}
	public int getInterval() {
		return mInterval;
	}
	public ArrayList<Peer> getPeers() {
		return mPeers;
	}
	public byte[][] getPieces() {
		return mPieces;
	}
}