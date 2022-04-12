package com.russbucket.networking.rtp.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

public class RtpPacket {

	private RtpHeader header;
	private byte[] payload;
	
	public RtpPacket(byte[] payload, RtpHeader header) {
		this.header = header;
		this.payload = payload;
	}
	
	public RtpPacket(byte[] payload, RtpHeader.PayloadType type, int frameNumber, int sequenceNumber, int sequenceLength) {
		this(payload, new RtpHeader(type, frameNumber, sequenceNumber, sequenceLength));
	}
	
	public RtpHeader getHeader() {
		return this.header;
	}
	
	public byte[] getPayload() {
		return this.payload;
	}
	
	/**
	 * Creates a RtpPacket from the received data
	 * 
	 * @param packet Data received
	 * @param length Length of the data that is readable (not empty)
	 * @param senderAddress
	 * @param senderPort
	 * @return Complete RtpPacket
	 */
	public static RtpPacket createPacket(byte[] packet, int length, InetAddress senderAddress, int senderPort) {
		ByteArrayInputStream bais = new ByteArrayInputStream(packet);
		//Header
		byte[] headerData = new byte[RtpHeader.SIZE];
		bais.read(headerData, 0, headerData.length);
		RtpHeader header = RtpHeader.decode(headerData);
		header.setSenderData(senderAddress, senderPort);
		
		//Payload (offset is 0, as we don't need to adjust the marker)
		byte[] payload = new byte[length - headerData.length];
		bais.read(payload, 0, payload.length);
		
		return new RtpPacket(payload, header);
	}
	
	/**
	 * Converts the packet into bytes (for sending)
	 * 
	 * @return Byte array containing the packet
	 */
	public byte[] getPacketAsBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.writeBytes(header.encode());
		baos.writeBytes(payload);
		return baos.toByteArray();
	}
	
}
