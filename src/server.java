// Name: Ian Goforth
// Email: img56@msstate.edu
// Student ID: 902-268-372

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

public class server {
    public static void main(String[] args) // args include n_port
    {
        System.out.println("Hello World.");
        int n_port = Integer.parseInt(args[0]);
        int r_port = genRandomPort();
        String message = "";

        // get negotiation from client
        try {
            DatagramSocket dsneg = new DatagramSocket(n_port);
            byte[] ack = new byte[Integer.BYTES];
            System.out.println("Waiting for client handshake...");
            DatagramPacket dpnegrec = new DatagramPacket(ack, ack.length);
            dsneg.receive(dpnegrec);
            System.out.println("Received client handshake.");
            DatagramPacket dpnegsend = new DatagramPacket(intToByteArray(r_port), intToByteArray(r_port).length, dpnegrec.getAddress(), dpnegrec.getPort());
            dsneg.send(dpnegsend);
            System.out.println("Random port chosen: " + r_port);
            dsneg.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // iterate through file on socket
        byte[] end = new byte[] {0x03,0x00,0x00,0x00};
        try {
            System.out.println("Creating listening socket on random port: " + r_port);
            DatagramSocket dstrans = new DatagramSocket(r_port);
            DatagramPacket dptrans;
            do {
                dptrans = waittrans(dstrans);
                if (dptrans.getData() != end) {
                    String chars = new String(dptrans.getData());
                    System.out.println("Received chars: " + chars);
                    message = message.concat(chars);
                    sendUpperACK(dstrans, dptrans);
                }
            } while (!Arrays.equals(dptrans.getData(), end));
            System.out.println("Writing message to file:\n" + message);
            write(message);
            dstrans.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // generate random port between low (inclusive) and high (exclusive)
    private static int genRandomPort() {
        Random r = new Random();
        int low = 1024;
        int high = 65536;
        return r.nextInt(high-low) + low;
    }

    // wait for client to transfer data
    private static DatagramPacket waittrans(DatagramSocket dsocket) throws IOException {
        byte[] trans = new byte[4];
        DatagramPacket dtrans = new DatagramPacket(trans, trans.length);
        dsocket.receive(dtrans);
        return dtrans;
    }

    // send back client data to string upper
    private static void sendUpperACK(DatagramSocket dsocket, DatagramPacket trans) {
        // pack bytes into UDP packet
        String s = new String(trans.getData()).toUpperCase();
        DatagramPacket dsend = new DatagramPacket(s.getBytes(), s.getBytes().length, trans.getAddress(), trans.getPort());

        // send over port and serverip
        try {
            System.out.println("Sending ACK: " + s);
            dsocket.send(dsend);
        } catch (IOException e) {
            System.out.println("Could not send packet.");
            e.printStackTrace();
        }
    }

    // write string to file
    private static void write(String message) {
        try {
            // delete upload.txt if it exists
            Files.deleteIfExists(FileSystems.getDefault().getPath("upload.txt"));
            // write to upload.txt
            Files.write(FileSystems.getDefault().getPath("upload.txt"), message.trim().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // convert int to byte array
    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
}