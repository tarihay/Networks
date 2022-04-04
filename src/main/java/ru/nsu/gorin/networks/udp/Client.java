package ru.nsu.gorin.networks.udp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.Scanner;

import static ru.nsu.gorin.networks.udp.Constants.*;

public class Client {
    private static final Logger logger = LogManager.getLogger(Client.class);

    public static void main(String[] args) throws Exception {
        int seq = 0;
        int ack = 0;

        Scanner scanner = new Scanner(System.in);

        try (DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT)) {
            logger.info("Client successfully started running");
            InetAddress address = InetAddress.getLocalHost();

            //Making SYN and sending it to Server
            Segment synSegment = new Segment(EMPTY_STRING, seq, ack);
            byte[] mes = synSegment.makeSimpleSegment();
            DatagramPacket syn = new DatagramPacket(mes, mes.length, address, SERVER_PORT);
            clientSocket.send(syn);
            logger.info("Client sent SYN");

            //Making byte array to receive SYN-ACK from Server
            byte[] synAckBytes = new byte[MAX_BUF_SIZE];
            DatagramPacket synAck = new DatagramPacket(synAckBytes, 0, synAckBytes.length);
            clientSocket.receive(synAck);
            logger.info("Client successfully received SYN-ACK");

            if (synAckBytes[ACK_POS] == mes[SEQ_POS] + 1) {
                //Making ACK and sending it to Server
                Segment ackSegment = new Segment(EMPTY_STRING, ++seq, ++ack);
                byte[] ackBytes = ackSegment.makeSimpleSegment();
                DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, address, SERVER_PORT);
                clientSocket.send(ackPacket);
                logger.info("Client sent ACK");

                String message;
                boolean isSent = false;

                int sendCount = 0;

                byte[] sendBuffer = new byte[MAX_BUF_SIZE];
                while (true) {
                    if (!isSent) {
                        message = scanner.nextLine();

                        //building the segment
                        Segment commonSegment = new Segment(message, ++seq, ++ack);

                        for (int i = 0; i <= commonSegment.getIterationCount(); i++) {

                            //putting all fields together
                            sendBuffer = commonSegment.convertDataToSegment(i);

                            DatagramPacket segment = new DatagramPacket(sendBuffer, sendBuffer.length, address, SERVER_PORT);
                            clientSocket.send(segment);
                            logger.info("Client sent the message number " + seq);
                        }
                        sendCount = 1;

                        isSent = true;
                    }
                    else {
                        if (sendCount >= MAX_SENDING_AMOUNT) {
                            logger.info("Too many tries to send the message. Client is closing connection");
                            clientSocket.close();
                        }
                        else {
                            sendCount++;
                        }

                        //resending the message
                        DatagramPacket segment = new DatagramPacket(sendBuffer, sendBuffer.length, address, SERVER_PORT);
                        clientSocket.send(segment);

                        logger.info("Client is resending the message");
                    }

                    Random random = new Random();
                    int randomNum = random.nextInt(5);

                    ackPacket = new DatagramPacket(ackBytes, 0, ackBytes.length);

                    try {
                        clientSocket.setSoTimeout(TIMEOUT);
                        clientSocket.receive(ackPacket);

                        //Don't receive ACK by random
                        if (randomNum != CONDITION_OF_FAILURE) {
                            logger.info("Client received the ack to message number " + seq);
                            if (ackBytes[ACK_POS] - 1 == seq) {
                                isSent = false;
                            }
                        }
                        else {
                            logger.info("Client didn't receive the ack to the message number " + seq);
                        }
                    }
                    catch (IOException ex) {
                        logger.info("Client didn't receive the message number " + seq + ". Starting to resend it");
                    }
                }
            }
            else {
                logger.info("Connection has not been established. Server sent wrong ack. Closing the app");
            }
        }
        catch (Exception ex) {
            logger.info("Client closed connection");
        }
    }
}
