package bully;

public class IncorrectArgumentsException extends Exception {
	private static final long serialVersionUID = 3582791246080108540L;

	public IncorrectArgumentsException() {
		super("Incorrect arguments.\nUsage: java -jar Bully.jar ip configuration-file [NoInitialization]");
	}
	
	public IncorrectArgumentsException(String message) {
		super(message + "\nUsage: java -jar Bully.jar ip configuration-file [NoInitialization]");
	}
}