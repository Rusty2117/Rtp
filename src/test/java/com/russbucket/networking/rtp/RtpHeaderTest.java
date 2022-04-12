package com.russbucket.networking.rtp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.russbucket.networking.rtp.packet.RtpHeader;

class RtpHeaderTest {

	@Test
	void testHeaderEncodeDecodeJpeg() {
		RtpHeader header = new RtpHeader(RtpHeader.PayloadType.JPEG, 1530, 82, 82);
		byte[] encoded = header.encode();
		
		RtpHeader h2 = RtpHeader.decode(encoded);
		
		Assertions.assertEquals(header.getType(), h2.getType());
		Assertions.assertEquals(header.getFrameNumber(), h2.getFrameNumber());
		Assertions.assertEquals(header.getSequenceNumber(), h2.getSequenceNumber());
		Assertions.assertEquals(header.getSequenceLength(), h2.getSequenceLength());
	}
	
	@Test
	void testHeaderEncodeDecodeStat() {
		RtpHeader header = new RtpHeader(RtpHeader.PayloadType.STAT, 1, 1, 120);
		byte[] encoded = header.encode();
		
		RtpHeader h2 = RtpHeader.decode(encoded);
		
		Assertions.assertEquals(header.getType(), h2.getType());
		Assertions.assertEquals(header.getFrameNumber(), h2.getFrameNumber());
		Assertions.assertEquals(header.getSequenceNumber(), h2.getSequenceNumber());
		Assertions.assertEquals(header.getSequenceLength(), h2.getSequenceLength());
	}
	
	@Test
	void testHeaderSenderData() {
		RtpHeader header = new RtpHeader(RtpHeader.PayloadType.STAT, 1, 1, 1);
		header.setSenderData(null, 0);
		
		Assertions.assertNull(header.getSenderAddress());
		Assertions.assertEquals(0, header.getSenderPort());
	}
}
