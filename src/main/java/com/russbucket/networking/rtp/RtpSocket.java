package com.russbucket.networking.rtp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.russbucket.networking.rtp.packet.RtpHeader;
import com.russbucket.networking.rtp.packet.RtpPacket;

/**
 * Socket implementation for RTP connections. This can be used to send and receive data.
 * 
 * This implementation is "best effort" as it overlays UDP.
 * 
 * @author Rusty
 */
public class RtpSocket {
	
	private DatagramSocket socket;
	
	/**
	 * This size is not imposed anywhere in the code,
	 * but should be used in implementation for best results
	 */
	public static final int SIZE_MAX = 65000 + RtpHeader.SIZE;
	
	/**
	 * Create a socket
	 * 
	 * @throws SocketException If the socket could not be opened or binded to a local port
	 */
	public RtpSocket() throws SocketException {
		this(0);
	}
	
	/**
	 * Create a socket to accept data on
	 * 
	 * @param port Port to bind the socket to (0 for any available port)
	 * 
	 * @throws SocketException If the socket could not be opened or binded to the specific port
	 */
	public RtpSocket(int port) throws SocketException {
		this.socket = new DatagramSocket(port);
		this.socket.setTrafficClass(0x02 | 0x04 | 0x08 | 0x10);
	}
	
	/**
	 * Receives a packet
	 * 
	 * @param receiveBuffer
	 * @return
	 * @throws IOException
	 */
	public RtpPacket receive(byte[] receiveBuffer) throws IOException {
		DatagramPacket udp = new DatagramPacket(receiveBuffer, receiveBuffer.length);
		this.socket.receive(udp);
		return RtpPacket.createPacket(udp.getData(), udp.getLength(), udp.getAddress(), udp.getPort());
	}
	
	/**
	 * Returns whether the socket is clsoed
	 * 
	 * @return True if the socket is closed
	 */
	public boolean isClosed() {
		return this.socket.isClosed();
	}
	
	/**
	 * Closes the socket
	 */
	public void close() {
//		this.socket.disconnect();
		this.socket.close();
	}
	
	/**
	 * Connects to the remote address and port
	 * 
	 * @param address
	 * @param port
	 * 
	 * @throws UnknownHostException If no IP address could be found for the address
	 */
	public void connect(String address, int port) throws UnknownHostException {
		this.socket.connect(InetAddress.getByName(address), port);
	}
	
	/**
	 * Send a packet to another endpoint
	 * 
	 * @param packet
	 * @param address Address of the endpoint
	 * @param port Port of the endpoint
	 * 
	 * @throws IOException If an IO error occurs
	 */
	public void send(RtpPacket packet, String address, int port) throws IOException {
		this.send(packet, InetAddress.getByName(address), port);
	}

	/**
	 * Send a packet to the remote endpoint (if connected)
	 * 
	 * @param packet
	 * 
	 * @throws IOException If an IO error occurs
	 */
	public void send(RtpPacket packet) throws IOException {
		this.send(packet, this.socket.getInetAddress(), this.socket.getPort());
	}
	
	/**
	 * Send a packet to another endpoint
	 * 
	 * @param packet
	 * @param address Address of the endpoint
	 * @param port Port of the endpoint
	 * 
	 * @throws IOException If an IO error occurs
	 */
	public void send(RtpPacket packet, InetAddress address, int port) throws IOException {
		DatagramPacket udp = new DatagramPacket(packet.getPacketAsBytes(), packet.getPacketAsBytes().length, address, port);
		this.socket.send(udp);
	}
}
