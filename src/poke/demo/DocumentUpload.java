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
	
	//connection.poke("temp.txt","ankurthuse");
	connection.findDoc("temp", "ankurthuse");
}
	
	public static void main(String[] args) {
		DocumentUpload upload = new DocumentUpload();
		upload.run();
	}

	

}
