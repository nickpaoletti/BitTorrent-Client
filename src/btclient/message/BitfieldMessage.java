package btclient.message;
/**
 * BitfieldMessage.java
 * 
 * @author Nick Paoletti
 * @author Daniel Selmon
 *
 */
public class BitfieldMessage extends Message {
	private final boolean[] bitfield;
	/**
	 * 
	 * @param bitfield
	 */
	public BitfieldMessage(final boolean[] bitfield){
		super((int)Math.ceil(bitfield.length/8f) + 1, TYPE_BITFIELD);
		this.bitfield = bitfield;
	}
	/**
	 * 
	 * @return
	 */
	public boolean[] getBitfield() {
		return bitfield;
	}
}