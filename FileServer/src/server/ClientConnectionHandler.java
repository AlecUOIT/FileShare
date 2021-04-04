package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientConnectionHandler extends Thread {
	protected Socket socket = null;
	protected String filesDirectory = null;
	protected File files = null;
	protected String clientName = null;

	public ClientConnectionHandler(Socket socket, String directory) {
		// initialize variables
		this.socket = socket;
		this.filesDirectory = directory;
		this.files = new File(directory);
		System.out.println("Thread init");

	}

	public void start() {
		System.out.println("Thread start");
		boolean actionCompleted = false;
		while (!actionCompleted) {
			try {
				actionCompleted = getComand();// call getComand method to wait for and process client input
			} catch (IOException e) {
				System.err.println("Error reciving input from client");
			}
		}
		try {// after the command has been processed close the socket
			socket.close();
		} catch (IOException e) {
			System.err.println("Error closing socket");
		}

	}

	public boolean getComand() throws IOException {
		String comand = null;// declare variable to store current command

		InputStream inStream = socket.getInputStream();// declare an InputStream to receive client input
		InputStreamReader reader = new InputStreamReader(inStream);// declare an InputStreamReader to read client input
		BufferedReader in = new BufferedReader(reader);// declare BufferedReader to handle input
		String line = null;// declare a string to store the current line
		while ((line = in.readLine()) != null) {// loop while there is data to process
			comand = line.split(" ")[0];// store contents of line before the first space
			if (comand.contentEquals("DIR")) {
				System.out.println(comand + " comand: sending file list");// write to console
				PrintStream ps = new PrintStream(socket.getOutputStream());// open PrintStream to send data to client
				ps.println(dirComand());// send result of dirComand which will be a list of files
				ps.flush();
			} else if (comand.contentEquals("UPLOAD")) {
				String fileName = line.substring(line.indexOf(" ") + 1, line.length());
				uploadComand(fileName, in.readLine());// call uploadComand and pass the filename and contents of next
														// line which should contain the file
			} else if (comand.contentEquals("DOWNLOAD")) {
				String fileName = line.substring(line.indexOf(" ") + 1, line.length());
				System.out.println(comand + " filename: " + fileName);
				PrintStream ps = new PrintStream(socket.getOutputStream());// open PrintStream to send data to client
				ps.println(downloadComand(fileName));
				ps.flush();

			}
			break;
		}
		return true;
	}

	// process commands
	public String dirComand() {
		System.out.println("sending file list from: " + files.getAbsolutePath());
		return String.join(", ", files.list());// return a string which contains the list of file names, seperated by a
												// comma
	}

	public boolean uploadComand(String fileName, String contents) {
		System.out.println("Writing to file: " + fileName + " Contents: " + contents);
		try {
			Files.writeString(Paths.get(files.getAbsolutePath() + "/" + fileName), contents);// write contents to local
																								// file fileName
		} catch (IOException e) {
			System.err.println("Error writing file");
			return false;
		}
		return true;
	}

	public String downloadComand(String fileName) {
		String content = null;
		try {
			content = Files.readString(Paths.get(files.getAbsolutePath() + "/" + fileName));// set contents variable to
																							// include the contents of
																							// the file
		} catch (IOException e) {
			System.err.println("Error getting contents of file");
		}
		return content;// return the files contents to be sent to the client
	}

}
