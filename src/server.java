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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

public class server {
	public static void main(String[] args) // args include n_port
	{
		// declare variable and parse args
		InetAddress emulatorip = null;
		try {
			emulatorip = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			System.out.println("Invalid Server IP.");
			e.printStackTrace();
		}
		int r_port = Integer.parseInt(args[1]);
		int s_port = Integer.parseInt(args[2]);
		String filename = args[3];

		// create sockets
		try {
			DatagramSocket s_s = new DatagramSocket();
			DatagramSocket r_s = new DatagramSocket(r_port);
	
			int seqnum = 0;
	
			String arrlog = "";
			String output = "";
	
			boolean end = false;
	
			do {
				System.out.println("Waiting for data...");
				packet p = recvfrom(r_s);
				if (p.getType() == 3) {
					arrlog = arrlog.concat(Integer.toString(p.getSeqNum()) + "\n");
					System.out.println("EOT Received. Sending EOT.");
					try {
						s_s.send(conPacket(serialize(conObject(2, seqnum, 0, null)), emulatorip, s_port));
					} catch (InvalidClassException e) {
						System.out.println("Could not serialize.");
						e.printStackTrace();
					}
					end = true;
				} else if (p.getSeqNum() == seqnum) {
					arrlog = arrlog.concat(Integer.toString(p.getSeqNum()) + "\n");
					output = output.concat(p.getData());
					System.out.println("New data recieved. Sending ACK.");
					try {
						s_s.send(conPacket(serialize(conObject(0, seqnum, 0, null)), emulatorip, s_port));
					} catch (InvalidClassException e) {
						System.out.println("Could not serialize.");
						e.printStackTrace();
					}
					seqnum ^= 1;
				} else {
					arrlog = arrlog.concat(Integer.toString(p.getSeqNum()) + "\n");
					System.out.println("Old data recieved. Resending ACK.");
					try {
						s_s.send(conPacket(serialize(conObject(0, p.getSeqNum(), 0, null)), emulatorip, s_port));
					} catch (InvalidClassException e) {
						System.out.println("Could not serialize.");
						e.printStackTrace();
					}
				}
			} while (end == false);
	
			write(arrlog, "arrival.log");
			write(output, filename);
	
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

	// write string to file
	private static void write(String message, String filename) {
		try {
			// delete upload.txt if it exists
			Files.deleteIfExists(FileSystems.getDefault().getPath(filename));
			// write to file
			Files.write(FileSystems.getDefault().getPath(filename), message.trim().getBytes());
		} catch (IOException e) {
			System.out.println("Could not write string to file.");
			e.printStackTrace();
		}
	}
}