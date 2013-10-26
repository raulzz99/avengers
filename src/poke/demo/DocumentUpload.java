package poke.demo;

import java.io.IOException;

import poke.client.ClientConnection;
import poke.client.ClientPrintListener;

public class DocumentUpload {

public DocumentUpload(){
		
} 
	
private void run() {
	ClientConnection connection = ClientConnection.initConnection("localhost", 5570);
	ClientPrintListener listener = new ClientPrintListener("Uploading");
	connection.addListener(listener);
	
	try {
		connection.poke("temp.txt");
	} catch (IOException e) {
		e.printStackTrace();
	}
}
	
	public static void main(String[] args) {
		DocumentUpload upload = new DocumentUpload();
		upload.run();
	}

	

}
