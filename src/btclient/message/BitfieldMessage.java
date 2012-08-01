package btclient.message;
/**
 * BitfieldMessage.java
 * 
 * This is a wrapper class designed to encapsulate a bitfield.
 * 
 * @author Nick Paoletti
 * @author Daniel Selmon
 *
 */
public class BitfieldMessage extends Message {
	private final boolean[] bitfield;
	/**
	 * Constructor
	 * 
	 * @param bitfield a boolean[] representation of the bitfield.
	 */
	public BitfieldMessage(final boolean[] bitfield){
		super((int)Math.ceil(bitfield.length/8f) + 1, TYPE_BITFIELD);
		this.bitfield = bitfield;
	}
	/**
	 * 
	 * @return <code>boolean[]</code> representation of the bitfield.
	 */
	public boolean[] getBitfield() {
		return bitfield;
	}
}