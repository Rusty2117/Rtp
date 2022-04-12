package com.russbucket.networking.rtp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.russbucket.networking.rtp.packet.RtpHeader;
import com.russbucket.networking.rtp.packet.RtpPacket;

class RtpPacketTest {

	@Test
	void test() {
		RtpPacket packetRtp = new RtpPacket(new byte[] {5}, RtpHeader.PayloadType.JPEG, 230, 283, 283);
		byte[] rawPacket = packetRtp.getPacketAsBytes();
		
		RtpPacket packetRtp2 = RtpPacket.createPacket(rawPacket, rawPacket.length, null, 0);
		Assertions.assertEquals(packetRtp.getHeader().getType(), packetRtp2.getHeader().getType());
		Assertions.assertEquals(packetRtp.getHeader().getFrameNumber(), packetRtp2.getHeader().getFrameNumber());
		Assertions.assertEquals(packetRtp.getHeader().getSequenceNumber(), packetRtp2.getHeader().getSequenceNumber());
		Assertions.assertEquals(packetRtp.getHeader().getSequenceLength(), packetRtp2.getHeader().getSequenceLength());
		Assertions.assertEquals(packetRtp.getPayload().length, packetRtp2.getPayload().length);
		Assertions.assertEquals(packetRtp.getPayload()[0], packetRtp2.getPayload()[0]);
		
		//Server side only values, should match values added when packetRtp2 created
		Assertions.assertNull(packetRtp2.getHeader().getSenderAddress());
		Assertions.assertEquals(0, packetRtp2.getHeader().getSenderPort());
	}
}
