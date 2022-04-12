package com.russbucket.networking;

import java.awt.AWTException;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.russbucket.networking.rtp.ImageProcessor;
import com.russbucket.networking.rtp.RtpRunnable;
import com.russbucket.networking.rtp.RtpSocket;
import com.russbucket.networking.rtp.TextProcessor;
import com.russbucket.networking.rtp.packet.RtpPacket;

/**
 * Example client implementation for this project.
 * 
 * This example takes screenshots of a 1080p screen and sends them over the network to other connected clients
 * 
 * @author Rusty
 */
public class RtpClient {
	
	private JFrame window;
	private RtpSocket socket;
	private TextProcessor textProcessor;
	private ConnectionReceiver connectionReceiver;
	private ScreenCapture screenCapture;
	
	private BufferedImage displayedImage;
	
	private RtpClient() {
		this.textProcessor = new TextProcessor();
		
		this.window = new JFrame("RTP Client");
		this.window.setSize(600, 500);
		this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				if(displayedImage != null)
					g.drawImage(displayedImage, 0, 30, this);
			}
		};
		panel.setLayout(null);
		panel.setSize(window.getSize());
		panel.setPreferredSize(window.getSize());
		
		JButton btnStart = new JButton("Start");
		btnStart.setBounds(0, 0, 100, 30);
		btnStart.setEnabled(false);
		panel.add(btnStart);
		
		JButton btnStop = new JButton("Stop");
		btnStop.setBounds(100, 0, 100, 30);
		btnStop.setEnabled(false);
		panel.add(btnStop);
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.setBounds(200, 0, 100, 30);
		panel.add(btnConnect);
		
		JButton btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setBounds(300, 0, 100, 30);
		btnDisconnect.setEnabled(false);
		panel.add(btnDisconnect);
		
		//Button handlers
		btnConnect.addActionListener((event)->{
			//Connect to the RTP server (may receive data if any is sending)
			try {
				this.socket = new RtpSocket();
				this.socket.connect("127.0.0.1", 5073);
			}catch(SocketException e){
				System.out.println("Socket could not be opened/binded to a local port");
				e.printStackTrace();
				return;
			}catch(UnknownHostException e) {
				System.out.println("Client unable to determine host");
				e.printStackTrace();
				return;
			}
			connectionReceiver = new ConnectionReceiver();
			connectionReceiver.setSocket(this.socket);
			connectionReceiver.setBufferSize(RtpSocket.SIZE_MAX);
			new Thread(connectionReceiver).start();

			try {
				List<RtpPacket> packets = this.textProcessor.encode("JOIN", 1);
				for(RtpPacket packet : packets) {
					this.socket.send(packet);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			btnConnect.setEnabled(false);
			btnStart.setEnabled(true);
			btnDisconnect.setEnabled(true);
		});

		btnStart.addActionListener((event)->{
			//Send screenshot images to others connected over RTP
			screenCapture = new ScreenCapture();
			new Thread(screenCapture).start();
			
			btnStart.setEnabled(false);
			btnStop.setEnabled(true);
			btnDisconnect.setEnabled(true);
		});

		btnStop.addActionListener((event)->{
			//Stop sending screenshot images
			this.screenCapture.stop();

			btnStop.setEnabled(false);
			btnStart.setEnabled(true);
		});

		btnDisconnect.addActionListener((event)->{
			//Disconnect from the RTP server
			try {
				List<RtpPacket> packets = this.textProcessor.encode("LEAVE", 1);
				for(RtpPacket packet : packets) {
					this.socket.send(packet);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			this.socket.close();
			this.socket = null;

			btnDisconnect.setEnabled(false);
			btnStart.setEnabled(false);
			btnStop.setEnabled(false);
			btnConnect.setEnabled(true);
		});
		
		window.add(panel);
		
		this.window.setVisible(true);
	}
	
	public static void main(String[] args) {
		new RtpClient();
	}
	
	/**
	 * Controls while screenshots should be captured on a dedicated thread
	 * 
	 * @author Rusty
	 */
	class ScreenCapture implements Runnable{
		
		private volatile boolean shouldCapture;
		
		public void stop() {
			this.shouldCapture = false;
		}
		
		@Override
		public void run() {
			Robot r = null;
			try {
				r = new Robot();
			}catch(AWTException e) {
				e.printStackTrace();
				return;
			}
			
			Rectangle area = new Rectangle(1920, 1080);
			BufferedImage img;
			int frameNumber = 0;
			long fps = 1000/30;
			shouldCapture = true;
			ImageProcessor imageProcessor = new ImageProcessor();
			
			while(this.shouldCapture) {
				long start = System.currentTimeMillis();
				
				img = r.createScreenCapture(area);
				
				try {
					List<RtpPacket> packets = imageProcessor.encode(img, frameNumber);
					for(RtpPacket packet : packets) {
						socket.send(packet);
					}
				}catch(IOException e) {
					e.printStackTrace();
					return;
				}
				frameNumber++;
				
				long delay = fps - (System.currentTimeMillis() - start);
				if(delay<0)
					delay = 0;
				try {
					Thread.sleep(delay);
				}catch(InterruptedException e) {}
			}
		}
	}
	
	/**
	 * Dedicated class for processing the received data, assumed to be images
	 * 
	 * @author Rusty
	 */
	class ConnectionReceiver extends RtpRunnable{
		private ImageProcessor imageProcessor;
		
		public ConnectionReceiver() {
			this.imageProcessor = new ImageProcessor();
		}
		
		@Override
		public void process(RtpPacket packet) {
			BufferedImage image = this.imageProcessor.decode(packet);
			if(image != null) {
				displayedImage = image;
				window.repaint();
			}
		}
	}
}
