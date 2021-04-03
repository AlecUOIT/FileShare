package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
	protected Socket clientSocket = null;
	protected ServerSocket serverSocket = null;
	protected ClientConnectionHandler[] threads = null;
	protected int numThreads = 0;

	//Variables to config
	public static int SERVER_PORT = 8182; //This is the port the server will be listening to
	public static int MAX_THREADS = 20005; //max number of threads supported
	public static String FILES_DIRECTORY = "files"; //location of the file directory
	
	public Main() {

		try {
			serverSocket = new ServerSocket(SERVER_PORT);
			System.out.println("Init server");
			
			threads = new ClientConnectionHandler[MAX_THREADS];
			while(true) {
				clientSocket = serverSocket.accept();
				System.out.println("Client #"+(numThreads+1)+" connected.");
				threads[numThreads] = new ClientConnectionHandler(clientSocket, FILES_DIRECTORY);
				threads[numThreads].start();
				numThreads++;
			}
			
		} catch (IOException e) {
			System.err.println("Error while creating server connection");
		}
		
	}
	public static void main(String[] args) {
		new Main();

	}

}
