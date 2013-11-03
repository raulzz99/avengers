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
package poke.server.queue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.Thread.State;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.resources.DocumentResource;
import poke.server.Server;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.resources.Resource;
import poke.server.resources.ResourceFactory;
import poke.server.resources.ResourceUtil;
import poke.server.routing.ForwardResource;
import poke.server.storage.InMemoryStorage;
import poke.server.storage.jdbc.DatabaseStorage;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.Header.ReplyStatus;
import eye.Comm.Header;
import eye.Comm.NameSpace;
import eye.Comm.Payload;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.RoutingPath;

/**
 * A server queue exists for each connection (channel). A per-channel queue
 * isolates clients. However, with a per-client model. The server is required to
 * use a master scheduler/coordinator to span all queues to enact a QoS policy.
 * 
 * How well does the per-channel work when we think about a case where 1000+
 * connections?
 * 
 * @author gash
 * 
 */
public class PerChannelQueue implements ChannelQueue {
	protected static Logger logger = LoggerFactory.getLogger("server");
	private  InMemoryStorage docStorer = new InMemoryStorage();
	private Channel channel;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> inbound;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorker oworker;
	private InboundWorker iworker;
	private ServerConf cfg = Server.conf;
	private static int replicaCount = 1;
	DatabaseStorage ds =null;
	private Properties prop=null;
	// not the best method to ensure uniqueness
	private ThreadGroup tgroup = new ThreadGroup("ServerQueue-" + System.nanoTime());

	protected PerChannelQueue(Channel channel) {
		this.channel = channel;
		init();
	}

	protected void init() {
		prop = new Properties();
		try {
			prop.load(new FileInputStream("/home/ankurthuse/Desktop/CMPE275/avengers-develop/Server_DB_properties.properties"));
			ds= new DatabaseStorage(prop);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		inbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

		iworker = new InboundWorker(tgroup, 1, this);
		iworker.start();

		oworker = new OutboundWorker(tgroup, 1, this);
		oworker.start();

		// let the handler manage the queue's shutdown
		// register listener to receive closing of channel
		// channel.getCloseFuture().addListener(new CloseListener(this));
	}

	protected Channel getChannel() {
		return channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#shutdown(boolean)
	 */
	@Override
	public void shutdown(boolean hard) {
		logger.info("server is shutting down");

		channel = null;

		if (hard) {
			// drain queues, don't allow graceful completion
			inbound.clear();
			outbound.clear();
		}

		if (iworker != null) {
			iworker.forever = false;
			if (iworker.getState() == State.BLOCKED || iworker.getState() == State.WAITING)
				iworker.interrupt();
			iworker = null;
		}

		if (oworker != null) {
			oworker.forever = false;
			if (oworker.getState() == State.BLOCKED || oworker.getState() == State.WAITING)
				oworker.interrupt();
			oworker = null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueRequest(eye.Comm.Finger)
	 */
	@Override
	public void enqueueRequest(Request req) {
		try {
			inbound.put(req);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for processing", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueResponse(eye.Comm.Response)
	 */
	@Override
	public void enqueueResponse(Response reply) {
		if (reply == null){
			//logger.info("reply is null");
			return;
		}
		try {
			outbound.put(reply);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for reply", e);
		}
	}

	protected class OutboundWorker extends Thread {
		int workerId;
		PerChannelQueue sq;
		boolean forever = true;

		public OutboundWorker(ThreadGroup tgrp, int workerId, PerChannelQueue sq) {
			super(tgrp, "outbound-" + workerId);
			this.workerId = workerId;
			this.sq = sq;

			if (outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
		public void run() {
			
			Channel conn = sq.channel;
			if (conn == null || !conn.isOpen()) {
				PerChannelQueue.logger.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				if (!forever && sq.outbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = sq.outbound.take();
					if (conn.isWritable()) {
						boolean rtn = false;
						if (channel != null && channel.isOpen() && channel.isWritable()) {
							ChannelFuture cf = channel.write(msg);

							// blocks on write - use listener to be async
							cf.awaitUninterruptibly();
							rtn = cf.isSuccess();
							if (!rtn)
								sq.outbound.putFirst(msg);
						}

					} else
						sq.outbound.putFirst(msg);
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					PerChannelQueue.logger.error("Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				PerChannelQueue.logger.info("connection queue closing");
			}
		}
	}

	protected class InboundWorker extends Thread {
		int workerId;
		PerChannelQueue sq;
		boolean forever = true;

		public InboundWorker(ThreadGroup tgrp, int workerId, PerChannelQueue sq) {
			super(tgrp, "inbound-" + workerId);
			this.workerId = workerId;
			this.sq = sq;

			if (outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel conn = sq.channel;
			logger.info("Server side---- Inbound worker run method " );
			if (conn == null || !conn.isOpen()) {
				PerChannelQueue.logger.error("connection missing, no inbound communication");
				return;
			}

			while (true) {
				if (!forever && sq.inbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = sq.inbound.take();
					// process request and enqueue response
					if (msg instanceof Request) {
						Request req = ((Request) msg);
						Resource rsc = ResourceFactory.getInstance().resourceInstance(req.getHeader());
						// do we need to route the request?
						Response reply = null;
						if (rsc == null) {
							logger.error("failed to obtain resource for " + req);
							reply = ResourceUtil.buildError(req.getHeader(), ReplyStatus.FAILURE,
									"Request not processed");
						}
						if (! req.getHeader().getTag().equals("response") ) 
						{
							String path = null;
							if (req.getHeader().getRoutingId() == Header.Routing.DOCADD || req.getHeader().getRoutingId() == Header.Routing.DOCREPLICATE) {
								docStorer.addDocument(req.getBody().getSpace().getName(),req.getBody().getDoc());
								
								if((req.getBody().getDoc().getChunkId()) == (req.getBody().getDoc().getTotalChunk())){
									logger.info("CBefore Save file is called " +req.getBody().getDoc().getDocName());
									logger.info("CHUNK ID IS " + req.getBody().getDoc().getChunkId() );
									path = docStorer.saveFile(req.getBody().getDoc(), req.getBody().getSpace(), req.getBody().getDoc().getDocName());
									reply = rsc.process(req);
									sq.enqueueResponse(reply);
								}
							}
							String nodeid = cfg.getServer().getGeneral().get("node.id");
							Iterator confList = cfg.getNearest().getNearestNodes().values().iterator();
							while(confList.hasNext()){
//								if (! req.getHeader().getTag().equals("response")) {
									
									NodeDesc node = (NodeDesc)confList.next();
									
									if (req.getHeader().getRoutingId() == Header.Routing.DOCADD || req.getHeader().getRoutingId() == Header.Routing.DOCREPLICATE) {
										
										//docStorer.addDocument(req.getBody().getSpace().getName(),req.getBody().getDoc());
										if(node.getNodeType().equals("replication")){
											if(req.getHeader().getRoutingId() == Header.Routing.DOCADD){
											//replicaCount = replicaCount -1 ;
												logger.info("current node doing doc replication: "+nodeid);
												ForwardResource fr = new ForwardResource(Header.Routing.DOCREPLICATE, node);
												fr.process(req);
											}
										}
										
										if((req.getBody().getDoc().getChunkId()) == (req.getBody().getDoc().getTotalChunk())){
											if (node.getNodeType().equals("leader")) {
												NameSpace ns = req.getBody().getSpace();
												NameSpace.Builder nsBuilder = eye.Comm.NameSpace.newBuilder();
												nsBuilder.setName(ns.getName());
												nsBuilder.setOwner(ns.getOwner());
												nsBuilder.setIpAddress(node.getHost());
												nsBuilder.setStoragePath(path);
												Request.Builder nsreqBuilder = eye.Comm.Request.newBuilder();
												Payload.Builder nsplBuilder = eye.Comm.Payload.newBuilder();
												nsplBuilder.setSpace(nsBuilder.build());
												nsreqBuilder.setHeader(req.getHeader());
												nsreqBuilder.setBody(nsplBuilder.build());
												ForwardResource fr = new ForwardResource(Header.Routing.METADD, node);
												fr.process(nsreqBuilder.build());
											}
										}
									}
									if (req.getHeader().getRoutingId() == Header.Routing.METADD || req.getHeader().getRoutingId() == Header.Routing.METAREPLICATE) 
									{
										
										NameSpace tempns = req.getBody().getSpace();
										
										boolean addresult = ds.addNameSpace(tempns.getName(),tempns.getOwner(),tempns.getIpAddress(),tempns.getStoragePath()); // STORE TO DB
										
										Response reply2 = null;
										Response.Builder rb = Response.newBuilder();
										PayloadReply.Builder pb = PayloadReply.newBuilder();
										
										if(!addresult){ //SUCCESSFUL ADD TO DB
											
											rb.setHeader(ResourceUtil.buildHeaderFrom(req.getHeader(), ReplyStatus.SUCCESS, null));
											rb.setBody(pb.build());
											reply2 = rb.build();
											sq.enqueueResponse(reply2);
											
											if( req.getHeader().getRoutingId() == Header.Routing.METADD ){ // ONLY ACCESSED BY LEADER
												logger.info("current node doing metadata replication: "+nodeid);
												if(node.getNodeType().equals("replication")){
													ForwardResource fr = new ForwardResource(Header.Routing.METAREPLICATE, node);
													fr.process(req);
												}
											}
										}
										else{ //FAILURE ADD TO DB
											rb.setHeader(ResourceUtil.buildHeaderFrom(req.getHeader(), ReplyStatus.FAILURE, null));
											rb.setBody(pb.build());
											reply2 = rb.build();
											sq.enqueueResponse(reply2);
										}
									}
								
							}
//							logger.info("header tag: "+req.getHeader().getTag());
//							if (! req.getHeader().getTag().equals("response") ) {
//								if((req.getBody().getDoc().getChunkId()) == (req.getBody().getDoc().getTotalChunk())){
//									logger.info("CBefore Save file is called " +req.getBody().getDoc().getDocName());
//									logger.info("CHUNK ID IS " + req.getBody().getDoc().getChunkId() );
//									docStorer.saveFile(req.getBody().getDoc(), req.getBody().getSpace(), req.getBody().getDoc().getDocName());
//									reply = rsc.process(req);
//									//logger.info("Reply value is " + reply.toString());
//									sq.enqueueResponse(reply);
//								}
//							} else {
//								//todo handle response
//							}
//							
//						}
					}
						}
						
					} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					PerChannelQueue.logger.error("Unexpected processing failure", e);
					break;
				}
				
			}

			if (!forever) {
				PerChannelQueue.logger.info("connection queue closing");
			}
		}
	}
	
	protected Channel createConnection(){
		
		return null;
	}

	public class CloseListener implements ChannelFutureListener {
		private ChannelQueue sq;

		public CloseListener(ChannelQueue sq) {
			this.sq = sq;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			sq.shutdown(true);
		}
	}
}
