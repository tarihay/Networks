package ru.nsu.gorin.networks.udp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.util.Random;

import static ru.nsu.gorin.networks.udp.Constants.*;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    public static void main(String[] args) {
        logger.info("Server successfully started running");

        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            InetAddress address = InetAddress.getLocalHost();

            byte[] seq = new byte[SEQ_ACK_SIZE];
            DatagramPacket syn = new DatagramPacket(seq, 0, seq.length);
            serverSocket.receive(syn);
            logger.info("Syn received successfully");

            byte ackNum = (byte) (seq[0] + 1);
            byte[] synAckBytes = {ackNum, 0};
            DatagramPacket synAck = new DatagramPacket(synAckBytes, synAckBytes.length, address, CLIENT_PORT);
            serverSocket.send(synAck);
            logger.info("Syn-Ack sent");

            byte[] ackBytes = new byte[SEQ_ACK_SIZE];
            DatagramPacket ack = new DatagramPacket(ackBytes, 0, ackBytes.length);
            serverSocket.receive(ack);
            logger.info("Ack received. Starting to receive messages");

            serverSocket.setSoTimeout(CLOSE_TIMEOUT);
            while (true) {
                byte[] message = new byte[MAX_BUF_SIZE];
                DatagramPacket segment = new DatagramPacket(message, 0, message.length);
                serverSocket.receive(segment);
                boolean isReceived = true;

                Random random = new Random();
                int randomNum = random.nextInt(5);
                if (randomNum == CONDITION_OF_FAILURE) {
                    isReceived = false;
                }

                int seqMessage = (int) message[0];
                int ackMessage = (int) message[1];
                byte[] buffer = new byte[MAX_BUF_SIZE];
                System.arraycopy(message, 2, buffer, 0, message.length-2);
                logger.info("received message number " + seqMessage);

                String receivedMessage = new String(buffer, "UTF-8");
                System.out.println("seq: " + seqMessage + ", ack: " + ackMessage + ", message: " + receivedMessage + "%\n");

                ackBytes = new byte[]{(byte) seqMessage, (byte) ackMessage};
                ack = new DatagramPacket(ackBytes, ackBytes.length, address, CLIENT_PORT);
                if (isReceived) {
                    serverSocket.send(ack);
                }
                logger.info("Ack sent");
            }
        } catch (IOException ex) {
            logger.info("Server is closing connection by timeout");
        }
    }
}