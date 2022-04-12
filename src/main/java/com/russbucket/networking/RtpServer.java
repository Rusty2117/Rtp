package com.russbucket.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.russbucket.networking.rtp.RtpRunnable;
import com.russbucket.networking.rtp.RtpSocket;
import com.russbucket.networking.rtp.TextProcessor;
import com.russbucket.networking.rtp.packet.RtpHeader;
import com.russbucket.networking.rtp.packet.RtpPacket;

/**
 * Example implementation of a server side implementation of this project
 * 
 * @author Rusty
 */
public class RtpServer extends RtpRunnable{

	private RtpSocket socket;
	
	private ConcurrentHashMap<String, UserInfo> connectionMapping;
	
	private TextProcessor textProcessor;
	
	private RtpServer() {
		try {
			this.socket = new RtpSocket(5073);
		}catch(SocketException e) {
			System.out.println("Socket could not be opened/binded to the port");
			this.closeWithError(e);
		}
		
		this.connectionMapping = new ConcurrentHashMap<>(0);
		this.textProcessor = new TextProcessor();
		
		this.setSocket(socket);
		this.setBufferSize(RtpSocket.SIZE_MAX);
		
		new Thread(this).start();
	}
	
	@Override
	public void process(RtpPacket packet) {
		InetAddress senderAddress = packet.getHeader().getSenderAddress();
		int senderPort = packet.getHeader().getSenderPort();
		
		if(packet.getHeader().getType().equals(RtpHeader.PayloadType.STAT)){
			//Add or remove connections from mapping
			String text = this.textProcessor.decode(packet);
			if(text.equals("JOIN")) {
				UserInfo user = new UserInfo(senderAddress, senderPort);
				connectionMapping.put(user.toString(), user);
				System.out.println("joined");
			}else if(text.equals("LEAVE")) {
				connectionMapping.remove(new UserInfo(senderAddress, senderPort).toString());
				System.out.println("left");
			}
		}else {
			//Forward data to other clients
			for(Map.Entry<String, UserInfo> connection : this.connectionMapping.entrySet()) {
				if(connection.getKey().equals(senderAddress.getHostAddress() + ":" + senderPort))
					continue;
				try {
					this.socket.send(packet, connection.getValue().getAddress(), connection.getValue().getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void closeWithError(Exception e) {
		e.printStackTrace();
		this.socket.close();
		System.exit(1);
	}
	
	public static void main(String[] args) {
		new RtpServer();
	}
	
	/**
	 * Custom class for managing user connections (even from the same system)
	 * 
	 * @author Rusty
	 */
	public class UserInfo {
		private InetAddress address;
		private int port;
		
		public UserInfo(InetAddress address, int port) {
			this.address = address;
			this.port = port;
		}
		
		public InetAddress getAddress() {
			return address;
		}
		
		public int getPort() {
			return port;
		}
		
		public String toString() {
			return this.address.getHostAddress() + ":" + this.port;
		}
	}
}
