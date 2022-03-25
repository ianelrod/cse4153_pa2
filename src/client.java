// Name: Ian Goforth
// Email: img56@msstate.edu
// Student ID: 902-268-372

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class client {
	public static void main(String[] args) // args include emulatorip, send port, receive port, file
	{
		// declare variable and parse args
		InetAddress emulatorip = null;
		try {
			emulatorip = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			System.out.println("Invalid Server IP.");
			e.printStackTrace();
		}
		int s_port = Integer.parseInt(args[1]);
		int r_port = Integer.parseInt(args[2]);
		Path path = FileSystems.getDefault().getPath(args[3]);

		// declare and get list
		List <String> list = convert(path);

		// create sockets
		try {
			DatagramSocket s_s = new DatagramSocket();
			DatagramSocket r_s = new DatagramSocket(r_port);
	
			int seqnum = 0;
	
			String seqlog = "";
			String acklog = "";
	
			for (String s : list) {
				boolean received = true;
				System.out.println("Sending packet: " + s);
				do {
					received = true;
					try {
						r_s.setSoTimeout(2000);
						try {
							packet p = conObject(1, seqnum, s.length(), s);
							s_s.send(conPacket(serialize(p), emulatorip, s_port));
							seqlog = seqlog.concat(Integer.toString(p.getSeqNum()) + "\n");
						} catch (InvalidClassException e) {
							System.out.println("Could not serialize.");
							e.printStackTrace();
						}
						packet pr = recvfrom(r_s);
						if (pr.getSeqNum() != seqnum) {
							throw new BadSeqNumException();
						}
						System.out.println("Received ACK.");
						acklog = acklog.concat(Integer.toString(pr.getSeqNum()) + "\n");
					} catch (SocketTimeoutException e) {
						System.out.println("Did not receive ACK. Resending packet.");
						received = false;
					} catch (BadSeqNumException e) {
						System.out.println("Received wrong sequence number. Resending packet.");
						received = false;
					}
				} while(received == false);
				
				seqnum ^= 1;
			}
	
			boolean end = false;
	
			while (end == false) {
				try {
					r_s.setSoTimeout(2000);
					packet ps = conObject(3, seqnum, 0, null);
					s_s.send(conPacket(serialize(ps), emulatorip, s_port));
					seqlog = seqlog.concat(Integer.toString(ps.getSeqNum()) + "\n");
					packet pr = recvfrom(r_s);
					if (pr.getType() == 2) {
						end = true;
						acklog = acklog.concat(Integer.toString(pr.getSeqNum()) + "\n");
						System.out.println("Received EOT.");
					}
				} catch (SocketTimeoutException e) {
					System.out.println("Did not receive EOT. Resending packet.");
				}
			}
	
			write(acklog, "clientack.log");
			write(seqlog, "clientseqnum.log");
	
			s_s.close();
			r_s.close();
		} catch (SocketException e) {
			System.out.println("Could not create socket.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO Exception.");
			e.printStackTrace();
		}
	}

	// construct packet object given parameters
	private static packet conObject(int type, int seqnum, int length, String data) {
		return new packet(type, seqnum, length, data);
	}

	private static DatagramPacket conPacket(byte[] b, InetAddress i, int port) {
		return new DatagramPacket(b, b.length, i, port);
	}

	// generate deserialized packet from serialized byte array
	private static packet deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			return (packet) in.readObject();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				System.out.println("IO Exception.");
				e.printStackTrace();
			}
		}
	}

	// generate serialized byte array from serializable object
	private static byte[] serialize(packet p) throws IOException, InvalidClassException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(p);
			out.flush();
			return bos.toByteArray();
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				System.out.println("IO Exception.");
				e.printStackTrace();
			}
		}
	}

	private static packet recvfrom(DatagramSocket r_s) throws IOException {
		byte[] r_b = new byte[1024];
		DatagramPacket r_p = new DatagramPacket(r_b, r_b.length);
		r_s.receive(r_p);
		byte[] data = new byte[r_p.getLength()];
		System.arraycopy(r_p.getData(), r_p.getOffset(), data, 0, r_p.getLength());
		packet rp = null;
		try {
			rp = deserialize(data);
		} catch (ClassNotFoundException e) {
			System.out.println("Could not deserialize.");
			e.printStackTrace();
		}
		return rp;
	}

	private static List<String> convert(Path path) { // Convert file to List of 4 char 8-bit ASCII
		List<String> list = new ArrayList<>();
		String message = null;

		// read message from file
		try {
			System.out.println("Reading message from file...");
			message = Files.readString(path.toAbsolutePath());
		} catch (IOException e) {
			System.out.println("Could not read message from file.");
			e.printStackTrace();
		}

		// add message to list of strings 4 chars long
		for (int i = 0; i < Objects.requireNonNull(message).length(); i += 30) {
			list.add(message.substring(i, Math.min(i + 30, message.length())));
		}

		//return list
		return list;
	}
	// write string to file
	private static void write(String message, String filename) {
		try {
			// delete upload.txt if it exists
			Files.deleteIfExists(FileSystems.getDefault().getPath(filename));
			// write to upload.txt
			Files.write(FileSystems.getDefault().getPath(filename), message.trim().getBytes());
		} catch (IOException e) {
			System.out.println("Could not write string to file");
			e.printStackTrace();
		}
	}
}

class BadSeqNumException extends Exception
{
      // Parameterless Constructor
      public BadSeqNumException() {}

      // Constructor that accepts a message
      public BadSeqNumException(String message)
      {
         super(message);
      }
 }