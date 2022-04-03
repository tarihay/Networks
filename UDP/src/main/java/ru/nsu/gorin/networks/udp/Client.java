package ru.nsu.gorin.networks.udp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import static ru.nsu.gorin.networks.udp.Constants.*;

public class Client {
    private static final Logger logger = LogManager.getLogger(Client.class);

    private static final int TIMEOUT_EXIT = 1;
    private static final int ANOTHER_EXIT = 2;

    private static final int SYN_ACK_SEQ = 1;
    private static final int SYN_ACK_ACK = 0;
    private static final int SYN_SEQ = 0;

    public static void main(String[] args) {
        logger.info("Client successfully started running");

        Scanner scanner = new Scanner(System.in);

        try (DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT)) {
            InetAddress address = InetAddress.getLocalHost();

            int seq = 0;
            int ack = 0;
            byte[] mes = {(byte) seq, (byte) ack};
            DatagramPacket syn = new DatagramPacket(mes, mes.length, address, SERVER_PORT);
            clientSocket.send(syn);

            byte[] synAckBytes = new byte[SEQ_ACK_SIZE];
            DatagramPacket synAck = new DatagramPacket(synAckBytes, 0, synAckBytes.length);
            clientSocket.receive(synAck);

            if (synAckBytes[SYN_ACK_ACK] == mes[SYN_SEQ] + 1) {
                seq++;
                ack++;
                byte[] ackBytes = {(byte) seq, (byte) ack};
                DatagramPacket ackSegment = new DatagramPacket(ackBytes, ackBytes.length, address, SERVER_PORT);
                clientSocket.send(ackSegment);

                String message = null;
                boolean isSent = false;

                clientSocket.setSoTimeout(CLOSE_TIMEOUT);
                while (true) {
                    if (!isSent) {
                        message = scanner.nextLine();
                        byte[] messageBytes = message.getBytes();


                        if (messageBytes.length > MAX_BUF_SIZE) {
                            int additionalSegmentsAmount;
                            if (messageBytes.length % MAX_BUF_SIZE == 0) {
                                additionalSegmentsAmount = messageBytes.length / MAX_BUF_SIZE;
                            }
                            else {
                                additionalSegmentsAmount = messageBytes.length / MAX_BUF_SIZE + 1;
                            }
                            for (int i = 0; i < additionalSegmentsAmount; i++) {
                                byte[] messagePart = new byte[MAX_BUF_SIZE];
                                messagePart[0] = (byte) (++seq);
                                messagePart[1] = (byte) (++ack);
                                int messageBytesOffset = MAX_BUF_SIZE*i - 2*i;
                                if (messageBytesOffset + MAX_BUF_SIZE - 2 < messageBytes.length) {
                                    System.arraycopy(messageBytes, messageBytesOffset, messagePart, 2, MAX_BUF_SIZE - 2);
                                }
                                else {
                                    System.arraycopy(messageBytes, messageBytesOffset, messagePart, 2, messageBytes.length - messageBytesOffset);
                                }
                                DatagramPacket segment = new DatagramPacket(messagePart, messagePart.length, address, SERVER_PORT);
                                clientSocket.send(segment);
                            }
                        }
                        else {
                            byte[] sendBuffer = new byte[messageBytes.length + 2];
                            sendBuffer[0] = (byte) (++seq);
                            sendBuffer[1] = (byte) (++ack);
                            System.arraycopy(messageBytes, 0, sendBuffer, 2, messageBytes.length);
                            DatagramPacket segment = new DatagramPacket(sendBuffer, sendBuffer.length, address, SERVER_PORT);
                            clientSocket.send(segment);
                        }
                        isSent = true;

                        logger.info("Client sent the message number " + seq);
                    }
                    else {
                        byte[] messageBytes = message.getBytes();
                        byte[] sendBuffer = new byte[messageBytes.length + 2];
                        sendBuffer[0] = (byte) seq;
                        sendBuffer[1] = (byte) ack;
                        System.arraycopy(messageBytes, 0, sendBuffer, 2, messageBytes.length);
                        DatagramPacket segment = new DatagramPacket(sendBuffer, sendBuffer.length, address, SERVER_PORT);
                        clientSocket.send(segment);

                        logger.info("Client is resending the message");
                    }

                    Random random = new Random();
                    int randomNum = random.nextInt(5);

                    ackSegment = new DatagramPacket(ackBytes, 0, ackBytes.length);

                    try {
                        clientSocket.setSoTimeout(TIMEOUT);
                        clientSocket.receive(ackSegment);

                        if (randomNum != CONDITION_OF_FAILURE) {
                            logger.info("Client received the ack to message number " + seq);
                            if (ackBytes[0] == seq) {
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
        catch (IOException ex1) {
            logger.info("Client is closing by timeout");
        }
        catch (Exception ex2) {
            logger.error(ex2);
            System.exit(ANOTHER_EXIT);
        }
    }
}
