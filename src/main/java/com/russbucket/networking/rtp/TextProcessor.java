package com.russbucket.networking.rtp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import com.russbucket.networking.rtp.packet.RtpHeader;
import com.russbucket.networking.rtp.packet.RtpPacket;

/**
 * Simple text processor, data may be split across multiple packets.
 * 
 * However, previous missing data will be dropped on a complete "frame"
 * 
 * @author Rusty
 */
public class TextProcessor implements IProcessor<String>{
	
	/**
	 * Keeps track of all potential text messages
	 */
	private ConcurrentSkipListMap<Integer, TextProcessor.TextData> potentialText;
	
	public TextProcessor() {
		this.potentialText = new ConcurrentSkipListMap<Integer, TextProcessor.TextData>();
	}
	
	@Override
	public List<RtpPacket> encode(String text, int frameNumber) throws IOException{
		ArrayList<RtpPacket> packets = new ArrayList<>();
		
		CharsetEncoder encoder = StandardCharsets.US_ASCII.newEncoder();
		encoder.onMalformedInput(CodingErrorAction.IGNORE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE)
				.replaceWith(new byte[] {0});
		byte[] data = encoder.encode(CharBuffer.wrap(text)).array();
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		
		int amount = (int) Math.ceil((double)(data.length)/(double)(RtpSocket.SIZE_MAX));
		byte[] buffer;
		for(int sequenceNumber=0; sequenceNumber<amount; sequenceNumber++) {
			buffer = new byte[data.length/amount];
			bais.read(buffer);
			packets.add(new RtpPacket(buffer, RtpHeader.PayloadType.STAT, frameNumber, sequenceNumber, amount));
		}
		
		return packets;
	}
	
	@Override
	public String decode(RtpPacket packet) {
		int frameNumber = packet.getHeader().getFrameNumber();
		if(!this.potentialText.containsKey(frameNumber))
			this.potentialText.put(frameNumber, new TextProcessor.TextData(packet.getHeader().getSequenceLength()));
		
		TextProcessor.TextData textData = this.potentialText.get(frameNumber);
		textData.add(packet.getHeader().getSequenceNumber(), packet.getPayload());
		
		String str = textData.recreate();
		
		if(str != null) {
			this.potentialText.remove(frameNumber);
			for(Integer otherFrame : this.potentialText.keySet()) {
				if(otherFrame < frameNumber)
					this.potentialText.remove(otherFrame);
			}
		}
		
		return str;
	}
	
	/**
	 * Mapping to reconstruct the text from multiple packets
	 * 
	 * @author Rusty
	 */
	public class TextData {
		private HashMap<Integer, byte[]> list;
		
		/**
		 * Constructor
		 * 
		 * @param length Total size of the text data (split between packets)
		 */
		public TextData(int length) {
			list = new HashMap<Integer, byte[]>(length);
		}
		
		/**
		 * Add data to the frame
		 * 
		 * @param sequenceNumber
		 * @param array
		 */
		public void add(int sequenceNumber, byte[] array) {
			if(sequenceNumber > list.size())
				throw new RuntimeException("Frame size changed mid frame");
			
			this.list.put(sequenceNumber, array);
		}
		
		/**
		 * Attempts to create the current item.
		 * 
		 * @return String containing the text if successful otherwise an empty string
		 */
		public String recreate() {
			if(this.list.size() < list.size())
				return null;
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for(int sequenceNumber=0; sequenceNumber<this.list.size();sequenceNumber++) {
				baos.writeBytes(this.list.get(sequenceNumber));
			}
			
			CharsetDecoder decoder = StandardCharsets.US_ASCII.newDecoder();
			decoder.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE)
					.replaceWith("?");
			try {
				return decoder.decode(ByteBuffer.wrap(baos.toByteArray())).toString();
			}catch(CharacterCodingException e) {
				e.printStackTrace();
				return "";
			}
		}
	}
}
