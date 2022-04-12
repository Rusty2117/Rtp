package com.russbucket.networking.rtp;

import java.io.IOException;
import java.net.SocketException;

import com.russbucket.networking.rtp.packet.RtpPacket;

/**
 * Runnable to handle the RTP continuous receiving, and passes packets out for processing.
 * 
 * Values cannot be changed after being assigned.
 * 
 * @author Rusty
 */
public abstract class RtpRunnable implements Runnable{
	
	private int bufferSize;
	private RtpSocket socket;
	
	/**
	 * Processes the RTP packets
	 * 
	 * @param packet
	 */
	public abstract void process(RtpPacket packet);
	
	/**
	 * Set the socket for the runnable to use.
	 * 
	 * Once set, this cannot be changed
	 * 
	 * @param socket
	 */
	public final void setSocket(RtpSocket socket) {
		if(this.socket != null)
			throw new RuntimeException("Cannot change socket once assigned");
		else if(socket == null)
			throw new RuntimeException("Socket cannot be null");
		
		this.socket = socket;
	}
	
	/**
	 * Set the buffer size of the RtpPackets
	 * 
	 * Once set, this cannot be changed
	 * 
	 * @param bufferSize
	 */
	public final void setBufferSize(int bufferSize) {
		if(bufferSize <= 0)
			throw new IllegalArgumentException("Buffer size cannot be 0 or negative");
		else if(this.bufferSize != 0)
			throw new RuntimeException("Cannot change buffer size once assigned");
		
		this.bufferSize = bufferSize;
	}
	
	@Override
	public void run() {
		if(this.bufferSize == 0 || this.socket == null)
			throw new IllegalStateException("Unable to receive without socket and buffer size");
		
		byte[] buffer = new byte[this.bufferSize];
		try {
			while(!this.socket.isClosed()) {
				RtpPacket packet = this.socket.receive(buffer);
				this.process(packet);
			}
		}catch(IOException e) {
			if(e instanceof SocketException && this.socket.isClosed()) {
				System.out.println("Socket has closed");
			}else {
				System.out.println("Unknown error occurred");
				e.printStackTrace();
			}
		}
	}
}
