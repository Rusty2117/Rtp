package com.russbucket.networking.rtp.packet;

import java.net.InetAddress;

/**
 * Custom Header containing the following:
 * Payload type (8 bits)
 * Frame Number (16 bits)
 * Sequence Number (16 bits)
 * 
 * See link for more information on RTP:
 * https://grouper.ieee.org/groups/1722/contributions/2010/1722-gross-rtp-v1.pdf
 * https://www.ietf.org/rfc/rfc3550.txt
 * 
 * @author Rusty
 */
public class RtpHeader {
	
	public enum PayloadType {
		JPEG, INVALID, STAT;
		
		byte toByte() {
			byte b;
			switch(this) {
			case STAT:
				b = 2;
				break;
			case JPEG:
				b = 1;
				break;
			default:
				b = -1;
				break;
			}

			return b;
		}
		
		static PayloadType toType(byte b) {
			if(b==1)
				return JPEG;
			else if(b==2)
				return STAT;
			
			return INVALID;
		}
	}
	
	private PayloadType type;
	private int frameNumber, sequenceNumber;
	private int sequenceLength;
	
	private InetAddress senderAddress;
	private int senderPort;
	
	public static final int SIZE = 7;
	
	public RtpHeader(PayloadType type, int frameNumber, int sequenceNumber, int sequenceLength) {
		this.type = type;
		this.frameNumber = frameNumber;
		this.sequenceNumber = sequenceNumber;
		this.sequenceLength = sequenceLength;
	}
	
	/**
	 * Set the sender's data. This is not sent, and is only used for processing purposes
	 * 
	 * @param address
	 * @param port
	 */
	public void setSenderData(InetAddress address, int port) {
		this.senderAddress = address;
		this.senderPort = port;
	}
	
	/**
	 * Gets the sender's address, if set on receive
	 * 
	 * @return
	 */
	public InetAddress getSenderAddress() {
		return this.senderAddress;
	}
	
	/**
	 * Gets the sender's port, if set on receive
	 * 
	 * @return
	 */
	public int getSenderPort() {
		return this.senderPort;
	}
	
	public int getSequenceLength() {
		return this.sequenceLength;
	}
	
	public int getSequenceNumber() {
		return this.sequenceNumber;
	}
	
	public int getFrameNumber() {
		return this.frameNumber;
	}
	
	public PayloadType getType() {
		return this.type;
	}
	
	public static RtpHeader decode(byte[] headerData) {
		return new RtpHeader(
				PayloadType.toType(headerData[0]),
				(headerData[2] & 0xFF) + ((headerData[1] & 0xFF) << 8),
				(headerData[4] & 0xFF) + ((headerData[3] & 0xFF) << 8),
				(headerData[6] & 0xFF) + ((headerData[5] & 0xFF) << 8));
	}
	
	public byte[] encode() {
		byte[] b = new byte[SIZE];
		b[0] = this.type.toByte();
		b[1] = (byte)(this.frameNumber >> 8);
		b[2] = (byte)(this.frameNumber & 0xFF);
		b[3] = (byte)(this.sequenceNumber >> 8);
		b[4] = (byte)(this.sequenceNumber & 0xFF);
		b[5] = (byte)(this.sequenceLength >> 8);
		b[6] = (byte)(this.sequenceLength & 0xFF);
		return b;
	}
}
