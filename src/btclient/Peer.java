package btclient;
/* Peer.java
 * 
 * by Nick Paoletti and Daniel Selmon
 * 
 * This is the Peer object, used primarily by the TrackerInfo object, but also used in
 * Download to update the files downloaded from the peer. It stores each Peers IP, Peer ID,
 * Port, and which files you have downloaded from them in a boolean array called the 'bitfield.'
 */

public class Peer{
	private String mIp; 
	private String mPeerId;
	private int mPort;
	private boolean[] mBitfield;
	//private short[] mDownloadedPieces; (not used atm)
	public Peer(String ip, String peerid, int port){
		mIp = ip;
		mPeerId = peerid;
		mPort = port;
		mBitfield = null;
	}
	public String getIP() {
		return mIp;
	}
	public String getPeerId() {
		return mPeerId;
	}
	public int getPort() {
		return mPort;
	}
	public boolean[] getBitfield() {
		return mBitfield;
	}
	public void setBitfield(boolean[] bitfield) {
		mBitfield = bitfield;
	}
	public String toString(){
		return "IP: " + mIp + " Peer ID: " + mPeerId + " Port: " + mPort;
	}
}