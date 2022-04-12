package com.russbucket.networking.rtp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.imageio.ImageIO;

import com.russbucket.networking.rtp.packet.RtpHeader;
import com.russbucket.networking.rtp.packet.RtpPacket;

/**
 * Encodes images to RtpPackets and vice versa across multiple packets
 * 
 * However, previous missing data will be dropped on a complete "frame"
 * 
 * @author Rusty
 */
public class ImageProcessor implements IProcessor<BufferedImage>{
	
	private ByteArrayOutputStream baos;
	/**
	 * Keeps track of all potential image messages
	 */
	private ConcurrentSkipListMap<Integer, ImageProcessor.ImageData> potentialImages;
	
	public ImageProcessor() {
		this.baos = new ByteArrayOutputStream();
		this.potentialImages = new ConcurrentSkipListMap<Integer, ImageProcessor.ImageData>();
	}
	
	@Override
	public ArrayList<RtpPacket> encode(BufferedImage image, int frameNumber) throws IOException{
		baos.reset();
		ImageIO.write(image, "jpg", baos);
		
		ArrayList<RtpPacket> packets = new ArrayList<>();
		
		int amount = (int) Math.ceil((double)(baos.size())/(double)(RtpSocket.SIZE_MAX));
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		byte[] buffer;
		for(int sequenceNumber=0; sequenceNumber<amount; sequenceNumber++) {
			buffer = new byte[baos.size()/amount];
			bais.read(buffer);
			packets.add(new RtpPacket(buffer, RtpHeader.PayloadType.JPEG, frameNumber, sequenceNumber, amount));
		}
		
		return packets;
	}
	
	@Override
	public BufferedImage decode(RtpPacket packet) {
		int frameNumber = packet.getHeader().getFrameNumber();
		if(!this.potentialImages.containsKey(frameNumber))
			this.potentialImages.put(frameNumber, new ImageProcessor.ImageData(packet.getHeader().getSequenceLength()));
		
		ImageProcessor.ImageData imageData = this.potentialImages.get(frameNumber);
		imageData.add(packet.getHeader().getSequenceNumber(), packet.getPayload());
		
		BufferedImage image = imageData.recreate();
		
		if(image != null) {
			this.potentialImages.remove(frameNumber);
			for(Integer otherFrame : this.potentialImages.keySet()) {
				if(otherFrame < frameNumber)
					this.potentialImages.remove(otherFrame);
			}
		}
		
		return image;
	}
	
	/**
	 * Mapping to reconstruct the image from multiple packets
	 * 
	 * @author Rusty
	 */
	public class ImageData {
		private HashMap<Integer, byte[]> list;
		private int length;
		
		/**
		 * Constructor
		 * 
		 * @param length Total size of the image data (split between packets)
		 */
		public ImageData(int length) {
			this.length = length;
			list = new HashMap<Integer, byte[]>(0);
		}
		
		/**
		 * Add data to the frame
		 * 
		 * @param sequenceNumber
		 * @param array
		 */
		public void add(int sequenceNumber, byte[] array) {
			if(sequenceNumber > length)
				throw new RuntimeException("Frame size changed mid frame");
			
			this.list.put(sequenceNumber, array);
		}
		
		/**
		 * Attempts to create the current item.
		 * 
		 * @return Image if successful otherwise null
		 */
		public BufferedImage recreate() {
			if(this.list.size() < length)
				return null;
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int i=0;
			for(;i<this.list.size();i++) {
				baos.writeBytes(this.list.get(i));
			}
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
				BufferedImage img = ImageIO.read(bais);
				return img;
			}catch(IOException e) {
				return null;
			}
		}
	}

}
