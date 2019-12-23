package bully;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class Node {

	private final int uuid;
	private final int port;
	private final int timeout;
	private final String ipAddress;
	
	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;

	public Node(int uuid, int port, int timeout) {
		this.port = port;
		this.uuid = uuid;
		this.timeout = timeout;
		this.ipAddress = "localhost";
	}

	public Node(int uuid, String ipAddress, int port, int timeout) {
		this.port = port;
		this.uuid = uuid;
		this.timeout = timeout;
		this.ipAddress = ipAddress;
	}
	
	public Node(String ipAddress, int port, int timeout) {
		this.port = port;
		this.uuid = Util.hashIP(ipAddress);
		this.timeout = timeout;
		this.ipAddress = ipAddress;
	}
	
	public int getUuid() {
		return uuid;
	}

	public int getPort() {
		return port;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}


	public synchronized boolean elect() {
		boolean ok = false;
		Bully.logger.log(String.format("Sending Elect %d to %d.", Bully.self.getUuid(), getUuid()));

		if(connect()) {		
			writer.println(Bully.self.getUuid());
			writer.println(Message.ELECT);
	
			if (getMessage() == Message.OK) {
				ok = true;
				Bully.logger.logInternal(String.format("Received OKAY from %d.", getUuid()));
			}
			disconnect();
		}
		return ok;
	}
	
	public synchronized boolean heartbeat() {
		boolean alive = false;
		
		Bully.logger.log(String.format("Sending Heartbeat %d to %d.", Bully.self.getUuid(), getUuid()));

		if(connect()) {
			writer.println(Bully.self.getUuid());
			writer.println(Message.HEARTBEAT);
			
	
			if (getMessage() == Message.ALIVE) {
				alive = true;
				Bully.logger.logInternal(String.format("Received ALIVE from %d.", getUuid()));
			}
			disconnect();
		}
		return alive;
		
	}

	public synchronized void result() {
		Bully.logger.log(String.format("Sending Result %d to %d.", Bully.self.getUuid(), getUuid()));

		if(connect()){

			writer.println(Bully.self.getUuid());
			writer.println(Message.RESULT);
			disconnect();
		}
	}

	private Message getMessage() {
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				return Message.valueOf(line);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		return null;
	}

	private boolean connect() {
		try {
			socket = new Socket(this.getIpAddress(), this.getPort());
			socket.setSoTimeout(timeout);
			writer = new PrintWriter(socket.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			disconnect();
			return false;
		}
		return true;
	}

	private void disconnect() {
		try {
			socket.close();
			socket = null;
			writer = null;
			reader = null;
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (NullPointerException e) {
		}
	}
}