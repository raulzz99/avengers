/*
 * copyright 2012, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.resources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import poke.server.storage.NoOpStorage;
import eye.Comm.Document;
import eye.Comm.Header;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.NameSpace;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;

public class DocumentResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");

	
	@Override
	public Response process(Request request) {
		// TODO Auto-generated method stub
		// return null;

		// TODO add code to process the message/event received
		// logger.info("poke: " + request.getBody().getFinger().getTag());

		Response reply = null;
		
			try {
				Response.Builder rb = Response.newBuilder();
				System.out.println("INSIDE DocumentResource-process() REQUEST IS "
						+ request);

				// metadata
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, null));
				Document.Builder f = eye.Comm.Document.newBuilder();

				byte[] byte_content = request.getBody().getDoc().getFiledata().toByteArray();

				// Print and write the payload file data
				
				
				// payload
				PayloadReply.Builder pb = PayloadReply.newBuilder();
				NameSpace.Builder fb = NameSpace.newBuilder();
				fb.setId(1);
				fb.setName("Doc 1");
				fb.setCreated(10132013);
				pb.addSpaces(fb.build());

				rb.setBody(pb.build());
				if (request.getHeader().getRoutingId() == Header.Routing.DOCADD) {
					NoOpStorage nops = new NoOpStorage();
					String content = new String(byte_content, "UTF-8");
					nops.addDocument(request.getBody().getDoc().getFilename(), content);
					
				//	writeToFile(content, request.getBody().getDoc().getFilename());
				}

				reply = rb.build();
				System.out.println("INSIDE DocumentResource-process() REPLY  IS "
						+ reply);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		return reply;
	}

	public void writeToFile(String data, String fileName) throws IOException {
		File file = new File(fileName);
		
		if (!file.exists()) {
			 System.out.println("Creating file  "+fileName);
			file.createNewFile();
		}
		FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
	
		fw.write(data);
		fw.close();

	}
}
