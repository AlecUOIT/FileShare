package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Main extends Application {
	protected static Socket socket = null;
	protected static File localFiles = null;

	public ListView<String> listViewLeft = new ListView<String>();
	public ListView<String> listViewRight = new ListView<String>();

	// Variables to config
	public static int SERVER_PORT = 8182; // This is the port the server will be listening to
	public static String SERVER_HOST = "localhost"; // This is the server address
	public static String FILES_DIRECTORY = "files"; // location of the file directory

	@Override
	public void start(Stage primaryStage) {

		// declare buttons
		Button uploadButton = new Button("UPLOAD");
		Button downloadButton = new Button("DOWNLOAD");
		Button refreshButton = new Button("REFRESH");

		try {
			HBox buttons = new HBox(uploadButton, downloadButton, refreshButton);// create horizontal box pane for
																					// buttons
			HBox lists = new HBox(listViewLeft, listViewRight);// create horizontal box pane for lists
			VBox mainBox = new VBox(buttons, lists);// create vertical box to put both the buttons and lists into

			listViewLeft.setPrefWidth(450);// set width of the ListView
			listViewRight.setPrefWidth(450);

			Scene scene = new Scene(mainBox, 900, 430);// declare a scene with the boxes above and dimensions
			primaryStage.setScene(scene);
			primaryStage.show();// display the stage containing the scene
		} catch (Exception e) {
			e.printStackTrace();
		}

		// call method to load file names into the lists
		loadLists();

		// button click handling
		downloadButton.setOnAction(actionEvent -> {
			String selected = listViewRight.getSelectionModel().getSelectedItem();// set string to the current selected
																					// file
			if(selected != null) {//ensure a file is selected
			System.out.println("Downloading: " + selected);
			downloadFile(selected);// call downloadFile method passing name of the selected file
			}else {
				System.out.println("Please select a file to download");
			}
			selected = null;
		});

		uploadButton.setOnAction(actionEvent -> {
			String selected = listViewLeft.getSelectionModel().getSelectedItem();
			
			if(selected != null) {//ensure a file is selected
				System.out.println("Uploading: " + selected);
				uploadFile(selected);// call uploadFile method passing name of the selected file
			}else {
				System.out.println("Please select a file to upload");
			}
			selected = null;
		});

		refreshButton.setOnAction(actionEvent -> {
			// call methods to load file name into the list
			loadLocalFileList();
			loadRemoteFileList();
		});

	}

	public void uploadFile(String fileName) {
		try {
			socketConnect();// this method will open a new connection with the socket
			PrintWriter output = new PrintWriter(socket.getOutputStream());// declare PrintWriter to send data to server
			output.println("UPLOAD " + fileName);// send the upload command to the server with the filename
			output.println(Files.readString(Paths.get(localFiles.getAbsolutePath() + "/" + fileName)));// send the
																										// contents of
																										// the file
			output.flush();
			socket.close();
		} catch (IOException e) {
			System.err.println("Error uploading file to server");
		}
		loadLists();// refresh lists with updated file names
	}

	public void downloadFile(String fileName) {
		try {
			// output to server
			socketConnect();// this method will open a new connection with the socket
			PrintWriter output = new PrintWriter(socket.getOutputStream());
			output.println("DOWNLOAD " + fileName);// send the download command with the filename to write
			output.flush();

			// input from server
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));// declare a reader
																									// to receive data
			String line = null;// declare a string to hold the current line of data
			while ((line = br.readLine()) != null) {// ensure that there is data from server
				writeLocalFile(fileName, line);// call method to create or write to the downloaded local file
				break;
			}
			socket.close();// close socket as the file has been downloaded
		} catch (IOException e) {
			System.err.println("Error getting file data from server");
		}
		loadLists();// refresh lists with updated file names
	}

	// this method will write to the local file system in a file with the name in
	// the variable "fileName"
	// the file contents will be whatever is in the passed variable "contents"
	public void writeLocalFile(String fileName, String contents) {
		System.out.println("Writing to file: " + fileName + "\nContents: " + contents);
		try {
			Files.writeString(Paths.get(localFiles.getAbsolutePath() + "/" + fileName), contents);
		} catch (IOException e) {
			System.err.println("Error writing file");
		}
	}

	// this method will call methods to refresh both the local and remote lists
	public void loadLists() {
		loadLocalFileList();
		loadRemoteFileList();
	}

	// this method will update the listView with the names of files in the local
	// directory
	public void loadLocalFileList() {
		listViewLeft.getItems().clear();// clear the listView
		try {
			for (String curFile : localFiles.list()) {// for each file in the local directory
				listViewLeft.getItems().add(curFile);// add the file name to the list
			}
		} catch (NullPointerException e) {
			System.err.println("Local directory " + FILES_DIRECTORY + " does not exist");
		}

	}

	// this method will request the list of files from the server and update the
	// listView
	public void loadRemoteFileList() {
		String[] remoteFiles = null;// declare array of strings to store file names
		try {
			socketConnect();// call method to create a new socket connection
			PrintWriter output = new PrintWriter(socket.getOutputStream());// declare PrintWriter to send data to the
																			// server
			output.println("DIR");// send the DIR command
			output.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));// declare a reader
																									// to receive data
			String line = null;// declare a string to hold the line of data from the server
			while ((line = br.readLine()) != null) {
				remoteFiles = line.split(", ");// take list of file names from server and separate them into an array
				break;
			}
			socket.close();// close socket connection
		} catch (IOException e) {
			System.err.println("Error getting file list from server");
		}

		listViewRight.getItems().clear();// clear the current list items to avoid duplicates
		try {
			for (String curFile : remoteFiles) {// for each file name in the array
				listViewRight.getItems().add(curFile);// add the file name to the ListView
			}
		} catch (NullPointerException e) {
			System.err.println("Empty remote directory or issues with connection");
		}
	}

	// this method will open a socket connection to the server
	public void socketConnect() {
		if (socket.isClosed()) {// ensure that there is no open socket connection
			try {
				socket = new Socket(SERVER_HOST, SERVER_PORT);// open socket
			} catch (IOException e) {
				System.err.println("Error connecting to server socket");
			}
		} else {// if the socket is already open
			try {
				socket.close();// close the socket
				socketConnect();// call this method again to open a new connection
			} catch (IOException e) {
				System.err.println("Error closing socket connection");
			}
		}
	}

	public static void main(String[] args) {

		try {
			socket = new Socket(SERVER_HOST, SERVER_PORT);// open a socket to connect to the server
		} catch (IOException e) {
			System.err.println("Error connecting to server socket");
		}

		localFiles = new File(FILES_DIRECTORY);// set the localFiles File to the local shared directory
		launch(args);
	}
}
