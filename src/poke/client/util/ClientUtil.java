package poke.client.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import com.google.protobuf.ByteString;

import poke.util.PrintNode;
import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.NameSpace;
import eye.Comm.NameValueSet;

public class ClientUtil {
	
	public static void printDocument(Document doc) {
		if (doc == null) {
			System.out.println("document is null");
			return;
		}
		
			
			com.google.protobuf.ByteString fileinfo = doc.getChunkContent();
			String s = new String(fileinfo.toByteArray());
			System.out.println(s);
		
		
	}
	
	public static void saveFile(Document doc , NameSpace space){
		String storage_path = createPath(space.getOwner());
		File file = null;
	    try {
	           file = new File(storage_path+File.separator+space.getName()+"1");
	           if (!file.exists()) {
	            	
	                     System.out.println("Creating file  "+space.getName());
	                     file.createNewFile();
	            }
	            FileWriter fw = new FileWriter(file);
	            com.google.protobuf.ByteString fileinfo = doc.getChunkContent();
                String s = new String(fileinfo.toByteArray());
	            fw.write(s);        	
	            fw.close();
	    } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	    }
	    return;
	}
    private static String createPath(String owner) {
		String tempDir = System.getProperty("java.io.tmpdir");
		if(owner==null){
			owner = "defaultDir";
		}
		File f = new File(tempDir+File.separator+owner);
		f.mkdir();
		return f.getAbsolutePath();
	}
    
	public static void printNameSpace(NameSpace space) {
		if (space == null) {
			System.out.println("namespace is null");
			return;
		}
		
		//get namespace 
		System.out.println("-------------------------------------------------------");
		System.out.println("NameSpace");
		System.out.println(" - ID   : " + space.getId());
		System.out.println(" - Name   : " + space.getName());
		System.out.println(" - Created : " + space.getCreated());
		
	}
	
	public static void printFinger(Finger f) {
		if (f == null) {
			System.out.println("finger is null");
			return;
		}

		System.out.println("Poke: " + f.getTag() + " - " + f.getNumber());
	}

	public static void printHeader(Header h) {
		System.out.println("-------------------------------------------------------");
		System.out.println("Header");
		System.out.println(" - Orig   : " + h.getOriginator());
		System.out.println(" - Req ID : " + h.getRoutingId());
		System.out.println(" - Tag    : " + h.getTag());
		System.out.println(" - Time   : " + h.getTime());
		System.out.println(" - Status : " + h.getReplyCode());
		if (h.getReplyCode().getNumber() != eye.Comm.Header.ReplyStatus.SUCCESS_VALUE)
			System.out.println(" - Re Msg : " + h.getReplyMsg());

		System.out.println("");
	}

}
