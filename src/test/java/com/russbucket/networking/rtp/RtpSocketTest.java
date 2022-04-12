package com.russbucket.networking.rtp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.russbucket.networking.rtp.packet.RtpHeader;
import com.russbucket.networking.rtp.packet.RtpPacket;

class RtpSocketTest {

	@Test
	void test() {
		try {
			RtpSocket server = new RtpSocket(5556);
			RtpSocket client = new RtpSocket();
			client.connect("127.0.0.1", 5556);

			//Send packet from client to server
			RtpPacket packetRtp = new RtpPacket(new byte[] {5}, RtpHeader.PayloadType.JPEG, 0, 0, 1);
			client.send(packetRtp);

			//Receive packet on server from client
			byte[] buffer = new byte[20];
			RtpPacket packetReceived = server.receive(buffer);

			//Success Criteria
			Assertions.assertEquals(packetRtp.getHeader().getType(), packetReceived.getHeader().getType());
			Assertions.assertEquals(packetRtp.getHeader().getFrameNumber(), packetReceived.getHeader().getFrameNumber());
			Assertions.assertEquals(packetRtp.getHeader().getSequenceNumber(), packetReceived.getHeader().getSequenceNumber());
			Assertions.assertEquals(packetRtp.getHeader().getSequenceLength(), packetReceived.getHeader().getSequenceLength());
			Assertions.assertEquals(packetRtp.getPayload().length, packetReceived.getPayload().length);
			Assertions.assertEquals(packetRtp.getPayload()[0], packetReceived.getPayload()[0]);

			client.close();
			server.close();
		}catch(Exception e) {
			Assertions.fail(e);
		}
	}
}
