package com.russbucket.networking.rtp;

import java.io.IOException;
import java.util.List;

import com.russbucket.networking.rtp.packet.RtpPacket;

/**
 * Template processor for encoding and decoding from a type and RtpPackets
 * 
 * @author Rusty
 * 
 * @param <T> Type to encode from or decode to based on the RtpPacket/s.
 * @see ImageProcessor
 * @see TextProcessor
 */
public interface IProcessor<T> {

	/**
	 * Encodes the data into a series of RtpPackets
	 * 
	 * @param dataIn Data to be encoded into an RtpPacket
	 * @param frameNumber Current frame number
	 * @return List of RtpPackets (may be unordered)
	 * 
	 * @throws IOException Potential exception that may occur when interacting with streams and buffers
	 */
	public List<RtpPacket> encode(T dataIn, int frameNumber) throws IOException;
	
	/**
	 * Decodes the RtpPacket into data
	 * 
	 * @param packet A single packet that may or may not contain all the necessary data
	 * @return Data or null (implementation dependent, but null may occur if data is stored in Processor class)
	 */
	public T decode(RtpPacket packet);
}
