package bully;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import static bully.Config.*;

public class Bully {

	boolean initializing = true;
	
	BufferedReader reader;
	PrintWriter writer;
	
	public static String LOCAL_IP_ADDRESS;
	public static Node self;
	public static Node coordinator;
	public static Logger logger;

	HashMap<Integer, Node> nodes;

	public static void main(String[] args) {
		//System.out.println("Args: " + Arrays.toString(args));
		Bully bully = new Bully();
		bully.getArgs(args);
		logger = new Logger(Bully.self.getUuid());
		bully.initiateSenderTask();
		bully.listen();
	}
	
	public Bully() {
		coordinator = null;
		try {
			LOCAL_IP_ADDRESS = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			System.err.println("Unable to acquire local IP address.");
			System.exit(-1);
		}
	}

	void getArgs(String[] args) {
		try {
			Bully.self = new Node(LOCAL_IP_ADDRESS, PORT,
					CONNECTION_TIMEOUT);
			nodes = getNodesConfiguration(CONFIG_FILE_NAME);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	HashMap<Integer, Node> getNodesConfiguration(String file) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();

		String line;
		String[] data;

		Node newNode;
		while ((line = reader.readLine()) != null) {
			data = line.split(",");
			newNode = new Node(data[0], PORT, CONNECTION_TIMEOUT);
			nodes.put(newNode.getUuid(), newNode);
			System.out.println(String.format("Loading network node configuration uuid: %d, ip: %s, port: %d",
					newNode.getUuid(), newNode.getIpAddress(), newNode.getPort()));
		}

		reader.close();
		return nodes;
	}

	void startElection() {
		logger.append(String.format("%d Triggers an Election.", self.getUuid()));

		boolean ok = false;
		Collection<Node> all = nodes.values();

		for (Node n : all) {
			if (n.getUuid() > self.getUuid()) {
				ok = n.elect() || ok;
			}
		}

		// No OK responses, become the new leader.
		if (ok == false) {
			logger.append(String.format(" Wins the Election.", self.getUuid()));
			coordinator = self;
			sendResult();
		}
	}

	void sendResult() {
		logger.append(String.format("No response from other nodes, %d is the Winner of the Election.", self.getUuid()));

		Collection<Node> all = nodes.values();

		for (Node n : all) {
			// Send result to all except self
			if (n.getUuid() != self.getUuid()) {
				n.result();
			}
		}
	}

	void listen() {
		Socket client;
		try (ServerSocket serverSocket = new ServerSocket(self.getPort())) {
			while (true) {
				client = serverSocket.accept();
				client.setSoTimeout(CONNECTION_TIMEOUT);
				receive(client);
			}
		} catch (IOException e) {
			System.err.println(String.format("Could not listen on port %d",self.getPort()));
			System.exit(-1);
		}
	}

	private void receive(Socket socket) {
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);

			int senderID = Integer.parseInt(reader.readLine());
			Message message = Message.valueOf(reader.readLine());

			if (message == Message.ELECT) {
				writer.println(Message.OK);
				Bully.logger.append(String.format("Recived Election from %d, Send OK.", senderID));
				startElection();
			} else if (message == Message.RESULT) {
				coordinator = nodes.get(senderID);
				Bully.logger.append(String.format("Received Result from %d. He is now the Coordinator", senderID));
			} else if(message == Message.HEARTBEAT) {
				writer.println(Message.ALIVE);
				Bully.logger.append(String.format("Receive Hartbeat from %d, Send ALIVE.", senderID));
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		try {
			socket.close();
			reader = null;
			writer = null;

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	private void initiateSenderTask() {
		Timer timer = new Timer(true);
		TimerTask heartbeatTask = new HeartbeatTask();
		timer.scheduleAtFixedRate(heartbeatTask, 0, HEARTBEAT_INTERVAL);
	}
	
	private class HeartbeatTask extends TimerTask{

		@Override
		public void run() {
			if(coordinator == null||
					(!coordinator.getIpAddress().equals(self.getIpAddress())
					&&!coordinator.heartbeat())) {
				startElection();
			}
		}
		
	}
}