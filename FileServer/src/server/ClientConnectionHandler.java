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

public class ClientConnectionHandler extends Thread{
	protected Socket socket = null;
	protected String filesDirectory = null;
	protected File files = null;
	protected String clientName = null;
	
	
	public ClientConnectionHandler(Socket socket,String directory) {
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
				actionCompleted = getComand();
			} catch (IOException e) {
				System.err.println("Error reciving input from client");
			}
		}
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Error closing socket");
		}
		
	}
	
	public boolean getComand() throws IOException {
		String comand = null;
		
		InputStream inStream= socket.getInputStream();
		InputStreamReader reader = new InputStreamReader(inStream);
		BufferedReader in = new BufferedReader(reader);
		String line = null;
		while ((line = in.readLine()) != null) {
			comand = line.split(" ")[0];
			if (comand.contentEquals("DIR")) {
				System.out.println(comand + " comand: sending file list");//write to console
				PrintStream ps = new PrintStream(socket.getOutputStream());//open PrintStream to send data to client
				ps.println(dirComand());//send result of dirComand which will be a list of files
				ps.flush();
			}else if (comand.contentEquals("UPLOAD")) {
				String fileName = line.substring(line.indexOf(" ")+1, line.length());
				uploadComand(fileName, in.readLine());//call uploadComand and pass the filename and contents of next line which should contain the file
			}else if (comand.contentEquals("DOWNLOAD")) {
				String fileName = line.substring(line.indexOf(" ")+1, line.length());
				System.out.println(comand + " filename: " + fileName);
				PrintStream ps = new PrintStream(socket.getOutputStream());//open PrintStream to send data to client
				ps.println(downloadComand(fileName));
				ps.flush();
				
			}
			break;
		}
		return true;
	}
	
	public String dirComand() {
		System.out.println("sending file list from: " + files.getAbsolutePath());
		return String.join(", ", files.list());
	}
	
	public boolean uploadComand(String fileName, String contents) {
		System.out.println("Writing to file: " + fileName +  " Contents: " + contents);
		try {
			Files.writeString(Paths.get(files.getAbsolutePath() + "/" + fileName), contents);
		} catch (IOException e) {
			System.err.println("Error writing file");
			return false;
		}
		return true;
	}
	
	public String downloadComand(String fileName) {
		String content = null;
		try {
			content = Files.readString(Paths.get(files.getAbsolutePath() + "/" + fileName));
		} catch (IOException e) {
			System.err.println("Error getting contents of file");
		}
		return content;
	}
	
}

