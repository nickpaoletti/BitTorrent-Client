package btclient.message;

public class RequestMessage extends Message {
	private final int index;
	private final int offset;
	private final int pieceLength;
	
	public RequestMessage(final int index, final int offset, final int pieceLength){
		super(13, TYPE_REQUEST);
		this.index = index;
		this.offset = offset;
		this.pieceLength = pieceLength;
	}

	public int getIndex() {
		return index;
	}

	public int getOffset() {
		return offset;
	}

	public int getPieceLength() {
		return pieceLength;
	}
}
