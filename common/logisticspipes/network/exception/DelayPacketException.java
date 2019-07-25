package logisticspipes.network.exception;

public class DelayPacketException extends RuntimeException {
	public DelayPacketException(String message) {
		super(message);
	}

	public DelayPacketException(Throwable cause) {
		super(cause);
	}

	public DelayPacketException() {
		super();
	}
}
