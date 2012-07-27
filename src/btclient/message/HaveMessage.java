package btclient.message;

/**
 * This class holds the Message of type "Have", which contains the index of the piece you have.
 * @author njpaol
 *
 */
public class HaveMessage extends Message{
	private final int index;
	
	/**
	 * Creates a HaveMessage with the index specified in the parameter.
	 * @param index Index of Piece you have
	 */
	public HaveMessage(int index){
		super(5, TYPE_HAVE);
		this.index = index;
	}
	
	/**
	 * The index of the piece the peer has.
	 * @return the piece index.
	 */
	public int getIndex() {
		return index;
	}
}
