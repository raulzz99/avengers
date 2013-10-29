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
package poke.server.resources;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import eye.Comm.Header;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.Header.Routing;
import eye.Comm.NameSpace;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.RoutingPath;

public class ResourceUtil {

	protected static Logger logger = LoggerFactory.getLogger("server");
	/**
	 * Build a forwarding request message. Note this will return null if the
	 * server has already seen the request.
	 * 
	 * @param req
	 *            The request to forward
	 * @param cfg
	 *            The server's configuration
	 * @return The request with this server added to the routing path or null
	 */
	public static Request buildForwardMessage(Request req, NodeDesc node, long count) {
		
//		NodeDesc node = cfg.getNearest().getNearestNodes().values().iterator().next();
//        Iterator it = cfg.getNearest().getNearestNodes().values().iterator();
//        while(it.hasNext()){
//        	NodeDesc node = (NodeDesc) it.next();
//        	String iam = node.getNodeId();
//        	logger.info("vaue of iam is " +  iam);
//        	List<RoutingPath> paths = req.getHeader().getPathList();
//        	logger.info("Size of paths " +  paths.size());
//        	if (paths != null) {
//        	
//        }
		
		//String iam = cfg.getServer().getProperty("node.id");
        
        
//        List<RoutingPath> paths = req.getHeader().getPathList();
        
        
            // if this server has already seen this message return null
            
//        }
		
       
		
		
//		//NodeDesc node = cfg.getNearest().getNearestNodes().values().iterator().next();
//		Collection<NodeDesc> node =  cfg.getNearest().getNearestNodes().values();
//		Iterator it = node.iterator();
//		String iam=null;
//		node.size();
//		//NodeDesc[] desc = (NodeDesc[]) node.toArray(); 
//		int i = 0;
//		while(it.hasNext()){
//			//iam = desc.
//			logger.info("Value of iam " + iam);
//			List<RoutingPath> paths = req.getHeader().getPathList();
//			logger.info("Size of paths " + paths.size());
//			if(paths!=null){
//				for(RoutingPath rp : paths){
//					if(iam.equalsIgnoreCase(rp.getNode())){
//						//i++;
//						continue;
//						}
//					else{
//						break;
//					}	
//						// Continue statement goes here
//				}
//				
//					
//				}
//				else{
//				break;
//			}
//		}
			
		
//		for(NodeDesc nd : node){
//			iam = nd.getNodeId();
//			logger.info("vaue of iam is " +  iam);
//			List<RoutingPath> paths = req.getHeader().getPathList();
//			logger.info("Size of paths " +  paths.size());
//			if (paths != null) {
//				// if this server has already seen this message return null
//				for (RoutingPath rp : paths) {
//					logger.info("NODE IS " + rp.getNode());
//					if (iam.equalsIgnoreCase(rp.getNode()))
//						return null;
//				}
//			}
//			else{
//				break;
//			}
//		
//		}
		
		//String iam = cfg.getServer().getProperty("node.id");
		//logger.info("vaue of iam is " +  iam);
		
//	NodeDesc node = cfg.getNearest().getNearestNodes().values().iterator().next();
    String iam = node.getNodeId();
    //String iam = cfg.getServer().getProperty("node.id");
    logger.info("vaue of iam is " +  iam);
    List<RoutingPath> paths = req.getHeader().getPathList();
    logger.info("Size of paths " +  paths.size());
    if (paths != null) {
        // if this server has already seen this message return null
        for (RoutingPath rp : paths) {
            logger.info("NODE IS " + rp.getNode());
            if (iam.equalsIgnoreCase(rp.getNode()))
                return null;
        }
    }
		
		
		Request.Builder bldr = Request.newBuilder(req);
		Header.Builder hbldr = bldr.getHeaderBuilder();
		hbldr.setRemainingHopCount(count);
		RoutingPath.Builder rpb = RoutingPath.newBuilder();
		rpb.setNode(iam);
		rpb.setTime(System.currentTimeMillis());
		hbldr.addPath(rpb.build());

		return bldr.build();
	}

		
	/**
	 * build the response header from a request
	 * 
	 * @param reqHeader
	 * @param status
	 * @param statusMsg
	 * @return
	 */
	public static Header buildHeaderFrom(Header reqHeader, ReplyStatus status, String statusMsg) {
		return buildHeader(reqHeader.getRoutingId(), status, statusMsg, reqHeader.getOriginator(), reqHeader.getTag());
	}

	public static Header buildHeader(Routing path, ReplyStatus status, String msg, String from, String tag) {
		Header.Builder bldr = Header.newBuilder();
		bldr.setOriginator(from);
		bldr.setRoutingId(path);
		bldr.setTag(tag);
		bldr.setReplyCode(status);

		if (msg != null)
			bldr.setReplyMsg(msg);

		bldr.setTime(System.currentTimeMillis());

		return bldr.build();
	}

	public static Response buildError(Header reqHeader, ReplyStatus status, String statusMsg) {
		Response.Builder bldr = Response.newBuilder();
		Header hdr = buildHeaderFrom(reqHeader, status, statusMsg);
		bldr.setHeader(hdr);
		//bldr.setBody(value)
		// TODO add logging
		PayloadReply.Builder bodyBuilder = PayloadReply.newBuilder();
		NameSpace.Builder nsBuilder = NameSpace.newBuilder();
		nsBuilder.setDesc("THis is error!");
		nsBuilder.setName("ERROR");
		NameSpace ns = nsBuilder.build();
		bodyBuilder.addSpaces(ns);
		PayloadReply body = bodyBuilder.build();
		bldr.setBody(body);
		return bldr.build();
	}
}
