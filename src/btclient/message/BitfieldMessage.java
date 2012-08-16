package btclient.message;
/**
 * BitfieldMessage.java
 * 
<<<<<<< HEAD
=======
 * This is a wrapper class designed to encapsulate a bitfield.
 * 
>>>>>>> 7a88bad1632fa639732138a69e36be3df8ea8b93
 * @author Nick Paoletti
 * @author Daniel Selmon
 *
 */
public class BitfieldMessage extends Message {
	private final boolean[] bitfield;
	/**
	 * 
	 * @param bitfield
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

	 * @return
	 * @return <code>boolean[]</code> representation of the bitfield.

	 */
	public boolean[] getBitfield() {
		return bitfield;
	}
}