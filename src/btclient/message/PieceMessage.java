package btclient.message;

public class PieceMessage extends Message {
	private final int index;
	private final int offset;
	private final byte[] pieceData;
	
	public PieceMessage(final int index, final int offset, final byte[] pieceData){
		super(9+pieceData.length, TYPE_PIECE);
		this.index = index;
		this.offset = offset;
		this.pieceData = pieceData;
	}

	public int getIndex() {
		return index;
	}

	public int getOffset() {
		return offset;
	}

	public byte[] getPieceData() {
		return pieceData;
	}
}
