package btclient.message;

public class BitfieldMessage extends Message {
	private final boolean[] bitfield;
	
	public BitfieldMessage(final boolean[] bitfield){
		super((int)Math.ceil(bitfield.length/8f) + 1, TYPE_BITFIELD);
		this.bitfield = bitfield;
	}

	public boolean[] getBitfield() {
		return bitfield;
	}
}
