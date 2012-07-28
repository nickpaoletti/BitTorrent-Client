package btclient;
/* Code taken from Rob's BencodingException class.
 * 
 */

public final class DisconnectException extends Exception {
	/**
	 * So I'm supposed to have one of these... thanks Eclipse for making this one for me.
	 */
	private static final long serialVersionUID = 108573016425631736L;
	private final String errorMessage;
	
	public DisconnectException(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public DisconnectException() {
		this.errorMessage = null;
	}
	
	@Override
	public final String toString() {	
		return "Peer Disconnected for reason:\n"
				+ (this.errorMessage == null ? "" : this.errorMessage);
	}

}
