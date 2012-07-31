package btclient;
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
	private byte[] userPeerId;
	private int mIncomplete, mDownloaded, mComplete, mMinInterval, mInterval;
	private ArrayList<Peer> mPeers;
	
	//Create the TrackerInfo object by initializing the list of peers.
	public TrackerInfo(){
		mPeers = new ArrayList<Peer>();
	}

	//These methods are used in Metadata to read in information from the tracker and store it
	//within a TrackerInfo object.
	public void setTorrentInfo(TorrentInfo info) {
		mTorrentInfo = info;
	}
	public void setUserPeerId(byte[] userpeerid) {
		userPeerId = userpeerid;
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
	public TorrentInfo getTorrentInfo() {
		return mTorrentInfo;
	}
	public byte[] getUserPeerId() {
		return userPeerId;
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
	
	public String makeURL(TorrentInfo torrentData, String state){
		String url = "";
		//Used for first announce and the subsequent HTTP GET request.
		if (state.equals("started")){
			url = url + torrentData.announce_url +  "?info_hash=" + Metadata.returnInfoHash(torrentData.info_hash)
					+ "&peer_id="+ userPeerId + "&port=6881" + "&uploaded=" + FileManager.uploaded + "&downloaded=" + FileManager.downloaded + 
					"&left=" + (torrentData.file_length - FileManager.downloaded) + "&event=started";
		}
		//Use right when the download completes
		else if (state.equals("completed")){
			url = url + torrentData.announce_url +  "?info_hash=" + Metadata.returnInfoHash(torrentData.info_hash)
			+ "&peer_id="+ userPeerId + "&port=6881" + "&uploaded=" + FileManager.uploaded + "&downloaded=" + FileManager.downloaded + 
			"&left=" + (torrentData.file_length - FileManager.downloaded) + "&event=completed";
		}
		//Use right after the download completes and complete notice is sent.
		else if (state.equals("stopped")){
			url = url + torrentData.announce_url +  "?info_hash=" + Metadata.returnInfoHash(torrentData.info_hash)
			+ "&peer_id="+ userPeerId + "&port=6881" + "&uploaded=" + FileManager.uploaded + "&downloaded=" + FileManager.downloaded + 
			"&left=" + (torrentData.file_length - FileManager.downloaded) + "&event=stopped";
		}
		return url;
		
	}
	
	

}