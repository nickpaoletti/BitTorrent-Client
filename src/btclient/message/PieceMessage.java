package btclient.message;
/**
 * PieceMessage.java
 * 
 * @author Nick Paoletti
 * @author Daniel Selmon
<<<<<<< HEAD
=======
 * 
 * This is a wrapper class for a piece message.
>>>>>>> 7a88bad1632fa639732138a69e36be3df8ea8b93
 *
 */
public class PieceMessage extends Message {
	private final int index;
	private final int offset;
	private final byte[] pieceData;
	/**
	 * 
	 * @param index
	 * @param offset
	 * @param pieceData
	 */
	public PieceMessage(final int index, final int offset, final byte[] pieceData){
		super(9+pieceData.length, TYPE_PIECE);
		this.index = index;
		this.offset = offset;
		this.pieceData = pieceData;
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
	public byte[] getPieceData() {
		return pieceData;
	}
}