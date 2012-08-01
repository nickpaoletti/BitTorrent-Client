package btclient.message;
/**
 * RequestMessage.java 
 * 
 * @author Nick Paoletti
 * @author Daniel
 *
 */
public class RequestMessage extends Message {
	private final int index;
	private final int offset;
	private final int pieceLength;
	/**
	 * 
	 * @param index
	 * @param offset
	 * @param pieceLength
	 */
	public RequestMessage(final int index, final int offset, final int pieceLength){
		super(13, TYPE_REQUEST);
		this.index = index;
		this.offset = offset;
		this.pieceLength = pieceLength;
	}
	/**
	 * 
	 * @return
	 */
	public int getIndex() {
		return index;
	}
	/**
	 * 
	 * @return
	 */
	public int getOffset() {
		return offset;
	}
	/**
	 * 
	 * @return
	 */
	public int getPieceLength() {
		return pieceLength;
	}
}