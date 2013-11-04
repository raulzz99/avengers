package poke.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import poke.server.queue.PerChannelQueue;
import poke.server.resources.ResourceUtil;

import com.google.protobuf.ByteString;

import eye.Comm.Document;
import eye.Comm.Header;
import eye.Comm.NameSpace;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.Header.ReplyStatus;

public class ServerUtil {
	private static final int CHUNK_SIZE = 100; 
	private static long FILE_SIZE = 0; 
	private static long TOTAL_CHUNK = 0; 
	
	public static byte[] read(InputStream ios) throws IOException{
		
	    ByteArrayOutputStream ous = null;
	    try {
	        byte[] buffer = new byte[CHUNK_SIZE];
	        ous = new ByteArrayOutputStream();
	        
	        int read = CHUNK_SIZE;
	        if ( (read = ios.read(buffer)) != -1 ) {
	        	ous.write(buffer, 0, read);
	        }
	        else{
	        	return null;
	        }
	    } 
	    catch(Exception e){
	    	System.out.print(e);
	    }
	    return ous.toByteArray();
	}
	public static void chunk(File file, Request orgreq, PerChannelQueue sq) throws IOException
	{
		InputStream ios = new FileInputStream(file);
		
		//File size in bytes
		FILE_SIZE = file.length();
		//Set totalchuck by dividing FILE_SIZE/65536 (divide by 12 for now)
		TOTAL_CHUNK = FILE_SIZE/CHUNK_SIZE;
		Response reply = null;
		
		for(long i=0; i<=TOTAL_CHUNK; i++)
		{
			
			NameSpace.Builder ns = null;
			Document.Builder f = null;
		
			byte[] filedata = read(ios);
			
			if (filedata == null) {
				return;
			}
			
			if(FILE_SIZE < CHUNK_SIZE){
				System.out.println("File read less than chunk size");
				com.google.protobuf.ByteString fileinfo = ByteString.copyFrom(filedata);
				
				//Namespace builder
				ns = eye.Comm.NameSpace.newBuilder();
				
				ns.setName(orgreq.getBody().getSpace().getName());
				ns.setOwner(orgreq.getBody().getSpace().getOwner());
				
				// data to send
				f = eye.Comm.Document.newBuilder();
				f.setDocName("temp");
				f.setChunkContent(fileinfo);
				f.setChunkId(i);
				f.setTotalChunk(TOTAL_CHUNK);
				
			}else{
				com.google.protobuf.ByteString fileinfo = ByteString.copyFrom(filedata);
				
				//Namespace builder
				ns = eye.Comm.NameSpace.newBuilder();
				
				ns.setName(orgreq.getBody().getSpace().getName());
				ns.setOwner(orgreq.getBody().getSpace().getOwner());
				
				// data to send
				f = eye.Comm.Document.newBuilder();
				f.setDocName("temp");
				f.setChunkContent(fileinfo);
				f.setChunkId(i);
				f.setTotalChunk(TOTAL_CHUNK);
				// payload containing data
				FILE_SIZE = FILE_SIZE - CHUNK_SIZE;
			}

			Response.Builder rb = Response.newBuilder();
			// metadata
			rb.setHeader(ResourceUtil.buildHeaderFrom(orgreq.getHeader(), ReplyStatus.SUCCESS, null));
			// payload
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			
			pb.addSpaces(orgreq.getBody().getSpace());
			pb.addDocs(f.build()); //send chunk
			
			rb.setBody(pb.build());
			reply = rb.build();
			sq.enqueueResponse(reply);
		}
		return;
	}
	
}
