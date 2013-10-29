/*
 * copyright 2013, gash
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
package poke.server.routing;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.Server;
import poke.server.conf.JsonUtil;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.resources.Resource;
import poke.server.resources.ResourceFactory;
import poke.server.resources.ResourceUtil;
import poke.servertoserver.ServerConnector;
import eye.Comm.Finger;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.RoutingPath;

/**
 * The forward resource is used by the ResourceFactory to send requests to a
 * destination that is not this server.
 * 
 * Strategies used by the Forward can include TTL (max hops), durable tracking,
 * endpoint hiding.
 * 
 * @author gash
 * 
 */
public class ForwardResource implements Resource  {
	protected static Logger logger = LoggerFactory.getLogger("server");

	private ServerConf cfg = Server.conf;
//	private ServerConf cfg;
	private long HOPCOUNT;
	
	public ForwardResource(long hopCount) {
		HOPCOUNT = hopCount;
	}
	
	public ServerConf getCfg() {
		return cfg;
	}

	/**
	 * Set the server configuration information used to initialized the server.
	 * 
	 * @param cfg
	 */
	public void setCfg(ServerConf cfg) {
		
		this.cfg = cfg;
		logger.info("Server Conf Value is  " + this.cfg);
	}

	@Override
	public Response process(Request request) {
		
			String nextNode = determineForwardNode(request);
			Request fwd;
			logger.info("Value returned by forward Node" + nextNode);
			if (nextNode != null) {
			Iterator configIt = cfg.getNearest().getNearestNodes().values().iterator();
			while(configIt.hasNext()){
				NodeDesc node = (NodeDesc) configIt.next();
				fwd = ResourceUtil.buildForwardMessage(request, node,HOPCOUNT );
				if(fwd == null){
					continue;
				}
				logger.info(" FORWARDED STRING " + fwd.toString());
				NodeDesc nodeDesc = cfg.getNearest().getNearestNodes().get(nextNode);
				logger.info("HOST "+ nodeDesc.getHost() + " PORT " + nodeDesc.getPort());
				ServerConnector serve = new ServerConnector(nodeDesc.getHost(),nodeDesc.getPort());
				logger.info("SERVER CONNECTOR VALUE is " + serve);
				try {
					serve.getOutboundServer().put(fwd);
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}
				
			}
			return null;
		} else {
			logger.info("Next node is null");
			Response reply = null;
			// cannot forward the message - no edge or already traveled known
			// edges

			// TODO should we just fail silently?

			Response.Builder rb = Response.newBuilder();
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			Finger.Builder fb = Finger.newBuilder();
			fb.setTag(request.getBody().getFinger().getTag());
			fb.setNumber(request.getBody().getFinger().getNumber());
			pb.setFinger(fb.build());
			rb.setBody(pb.build());
			
			
			
			reply = rb.build();

			return reply;
		}
	}

	/**
	 * Find the nearest node that has not received the request.
	 * 
	 * TODO this should use the heartbeat to determine which node is active in
	 * its list.
	 * 
	 * @param request
	 * @return
	 */
	private String determineForwardNode(Request request) {
		List<RoutingPath> paths = request.getHeader().getPathList();
		
		if (paths == null || paths.size() == 0) {
			logger.info("Server conf " + cfg);
			NodeDesc nd = cfg.getNearest().getNearestNodes().values().iterator().next();
			
			logger.info("Returned Value " + nd.getNodeId());
			return nd.getNodeId();
		} else {
			logger.info("Inside else block");
			logger.info("PATHS " + paths.iterator().next().getNode());
			// if this server has already seen this message return null
			for (RoutingPath rp : paths) {
				for (NodeDesc nd : cfg.getNearest().getNearestNodes().values()) {
					if (!nd.getNodeId().equalsIgnoreCase(rp.getNode()))
						return nd.getNodeId();
				}
			}
		}

		return null;
	}
}
