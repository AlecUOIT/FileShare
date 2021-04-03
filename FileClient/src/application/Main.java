package application;
	
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
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
	
	public static int SERVER_PORT = 8182; //This is the port the server will be listening to
	public static String SERVER_HOST = "localhost"; //This is the server address
	public static String FILES_DIRECTORY = "files"; //location of the file directory
	
	@Override
	public void start(Stage primaryStage) {
		
		Button uploadButton = new Button("UPLOAD");
		Button downloadButton = new Button("DOWNLOAD");
		Button refreshButton = new Button("REFRESH");
		
		try {
			HBox buttons = new HBox(uploadButton, downloadButton, refreshButton);
	        HBox lists = new HBox(listViewLeft, listViewRight);
	        VBox mainBox = new VBox(buttons, lists);
	        
	        listViewLeft.setPrefWidth(450);
	        listViewRight.setPrefWidth(450);
	        
	        
	        Scene scene = new Scene(mainBox, 900, 800);
	        primaryStage.setScene(scene);
	        primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		loadLocalFileList();
		loadRemoteFileList();
		
		downloadButton.setOnAction(actionEvent ->  {
			String selected = listViewRight.getSelectionModel().getSelectedItem();
			System.out.println("Downloading " + selected);
			downloadFile(selected);
		});
		
		uploadButton.setOnAction(actionEvent ->  { 
			String selected = listViewLeft.getSelectionModel().getSelectedItem();
			System.out.println("Uploading " + selected);
			uploadFile(selected);
		});
		
		refreshButton.setOnAction(actionEvent ->  {		
			loadLocalFileList();
			loadRemoteFileList();
		});
		
		
	}
	
	public void uploadFile(String fileName) {
		try {
			socketConnect();
			PrintWriter output = new PrintWriter(socket.getOutputStream());
			output.println("UPLOAD " + fileName);
			output.println(Files.readString(Paths.get(localFiles.getAbsolutePath() + "/" + fileName)));
			output.flush();
			socket.close();
		} catch (IOException e) {
			System.err.println("Error uploading file to server");
		}
		loadRemoteFileList();
	}
	
	public void downloadFile(String fileName) {
		System.out.println(socket.isConnected());
		try {
			socketConnect();
			PrintWriter output = new PrintWriter(socket.getOutputStream());
			output.println("DOWNLOAD " + fileName);
			output.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				writeLocalFile(fileName, line);
				break;
			}
			socket.close();
		} catch (IOException e) {
			System.err.println("Error getting file data from server");
		}
		loadLocalFileList();
	}
	
	public void writeLocalFile(String fileName, String contents) {
		System.out.println("Writing to file: " + fileName +  " Contents: " + contents);
		try {
			Files.writeString(Paths.get(localFiles.getAbsolutePath() + "/" + fileName), contents);
		} catch (IOException e) {
			System.err.println("Error writing file");
		}
	}
	
	public void loadLocalFileList() {
		listViewLeft.getItems().clear();
		try {
			for (String curFile : localFiles.list()) {
				listViewLeft.getItems().add(curFile);
			}
		} catch (NullPointerException e) {
			System.out.println("Empty local directory");
		}
		
	}
	
	public void loadRemoteFileList() {
		String[] remoteFiles = null;
		try {
			socketConnect();
			PrintWriter output = new PrintWriter(socket.getOutputStream());
			output.println("DIR");
			output.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				remoteFiles = line.split(", ");
				break;
			}
			socket.close();
		} catch (IOException e) {
			System.err.println("Error getting file list from server");
		}
		
		listViewRight.getItems().clear();
		try {
			for (String curFile : remoteFiles) {
				listViewRight.getItems().add(curFile);
			}
		} catch (NullPointerException e) {
			System.out.println("Empty remote directory or issues with connection");
		}
	}
	
	
	public void socketConnect() {
		if (socket.isClosed()) {
			try {
				socket = new Socket(SERVER_HOST, SERVER_PORT);
			} catch (IOException e) {
				System.err.println("Error connecting to server socket");
			}
		}else {
			try {
				socket.close();
				socketConnect();
			} catch (IOException e) {
				System.err.println("Error closing socket connection");
			}
		}
	}
	
	public static void main(String[] args) {

		try {
			socket = new Socket(SERVER_HOST, SERVER_PORT);
		} catch (IOException e) {
			System.err.println("Error connecting to server socket");
		}
		
		localFiles = new File(FILES_DIRECTORY);
		launch(args);
	}
}
