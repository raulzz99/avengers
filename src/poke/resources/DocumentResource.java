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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.Server;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.resources.Resource;
import poke.server.resources.ResourceFactory;
import poke.server.resources.ResourceUtil;
import eye.Comm.Finger;
import eye.Comm.NameSpace;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.RoutingPath;
import eye.Comm.Header.ReplyStatus;

public class DocumentResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");
	private ServerConf cfg = Server.conf;

	@Override
	public Response process(Request request) {
		// TODO Auto-generated method stub
		//return null;
		
		// TODO add code to process the message/event received
		//logger.info("poke: " + request.getBody().getFinger().getTag());
//		 String nextNode = determineForwardNode(request);
//		 logger.info( " Next Node value " + nextNode);
//		if(nextNode != null ){
//			Request fwd = ResourceUtil.buildForwardMessage(request, cfg);
//			logger.info("FORWARDED STRING " + fwd.toString());
//			Resource resource = ResourceFactory.getInstance().resourceInstance(fwd.getHeader());
//			Response r = null;
//			if (resource == null) {
//				logger.error("failed to obtain resource for " + fwd);
//				r = ResourceUtil.buildError(fwd.getHeader(), ReplyStatus.FAILURE,
//						"Request not processed");
//			} else{
//				logger.info("Processing Request");
//				resource.process(fwd);
//				}
//			
//			return null;
//		} 
//		else{
		Response.Builder rb = Response.newBuilder();

		// metadata
		rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, null));
		// payload
		PayloadReply.Builder pb = PayloadReply.newBuilder();
		NameSpace.Builder fb = NameSpace.newBuilder();
		fb.setId(1);
		fb.setName("Doc 1");
		fb.setCreated(10132013);
		pb.addSpaces(fb.build());
		
		rb.setBody(pb.build());
		Response reply = rb.build();
		return reply;
//		}
	}
	
	public String determineForwardNode(Request request) {
		List<RoutingPath> paths = request.getHeader().getPathList();
		if(paths==null || paths.size() ==0 ){
			logger.info("Inside deterimine Forward Node");
//			logger.info(ResourceFactory.cfg.toString());
			NodeDesc nd = cfg.getNearest().getNearestNodes().values().iterator().next();
			int size = cfg.getNearest().getNearestNodes().size();
			logger.info("Size is " + size);
			
			return nd.getNodeId();
		}
		return null;
	}


}
