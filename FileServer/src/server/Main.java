package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
	protected Socket clientSocket = null;
	protected ServerSocket serverSocket = null;
	protected ClientConnectionHandler[] threads = null;
	protected int numThreads = 0;

	// Variables to config
	public static int SERVER_PORT = 8182; // This is the port the server will be listening to
	public static int MAX_THREADS = 1000; // max number of threads supported
	public static String FILES_DIRECTORY = "files"; // location of the file directory

	public Main() {

		try {
			serverSocket = new ServerSocket(SERVER_PORT);// open socket with the server
			System.out.println("Starting server on port " + SERVER_PORT);

			while (true) {// this loop will allow the server to continue running after the number of
							// threads exceeds the max
				numThreads = 0;
				threads = new ClientConnectionHandler[MAX_THREADS];// init array of threads
				while (numThreads < MAX_THREADS) {// loop until the number of threads exceeds the array
					clientSocket = serverSocket.accept();
					System.out.println("Thread " + (numThreads + 1) + "/" + MAX_THREADS + " created");
					threads[numThreads] = new ClientConnectionHandler(clientSocket, FILES_DIRECTORY);// create new
																										// thread
					threads[numThreads].start();// start the ClientConnectionHandler thread
					numThreads++;// increment the number of threads opened
				}
			}

		} catch (IOException e) {
			System.err.println(
					"Error while creating server connection, please make sure port " + SERVER_PORT + " is free");
		}

	}

	public static void main(String[] args) {
		new Main();
	}

}
