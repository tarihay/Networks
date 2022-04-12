package ru.nsu.gorin.networks.udp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Random;

import static ru.nsu.gorin.networks.udp.Constants.*;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            logger.info("Server successfully started running");

            int seq = 0;
            int ack = 0;
            InetAddress address = InetAddress.getLocalHost();
            Context context = new Context(seq, ack, address);

            context = checkConnectionByTripleHandshake(serverSocket, context);
            seq = context.getSeq();
            ack = context.getAck();

            serverSocket.setSoTimeout(CLOSE_TIMEOUT);
            while (true) {
                byte[] message = new byte[MAX_BUF_SIZE];
                DatagramPacket receivedPacket = new DatagramPacket(message, 0, message.length);
                serverSocket.receive(receivedPacket);
                boolean isReceived = true;

                //Don't receive message by random
                Random random = new Random();
                int randomNum = random.nextInt(5);
                if (randomNum == CONDITION_OF_FAILURE) {
                    isReceived = false;
                }

                //Unzipping received segment
                Segment receivedSegment = new Segment(message);
                logger.info("received message number " + receivedSegment.getSeq());
                System.out.println("seq: " + receivedSegment.getSeq() + ", ack: " + receivedSegment.getAck()
                        + ", message: " + receivedSegment.getMessage() + "%\n");

                ack = receivedSegment.getSeq() + 1;

                //making ack segment
                Segment ackSegment = new Segment("", seq, ack);
                byte[] ackBytes = ackSegment.makeSimpleSegment();
                DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, address, CLIENT_PORT);
                if (isReceived) {
                    serverSocket.send(ackPacket);
                }
                logger.info("Ack sent");
            }
        } catch (IOException ex) {
            logger.info("Server is closing connection by timeout");
        }
    }

    /**
     * Метод делает тройное рукопожатие
     * @param serverSocket сокет, с помощью которого сервер отправляет и получает данные от клиента
     * @param context используется для удобства передачи и изменения seq и ack
     * @return Возвращает context с новым ack
     * @throws IOException в случае ошибки получения или отправки через сокет
     *
     * @see Context
     */
    private static Context checkConnectionByTripleHandshake(DatagramSocket serverSocket, Context context) throws IOException {
        int seq = context.getSeq();
        int ack = context.getAck();

        //Receiving Syn
        byte[] synBytes = new byte[MAX_BUF_SIZE];
        DatagramPacket syn = new DatagramPacket(synBytes, 0, synBytes.length);
        serverSocket.receive(syn);
        logger.info("SYN received successfully");

        //Sending SYN-ACK
        ack = synBytes[SEQ_POS] + 1;
        Segment synAckSegment = new Segment("", seq, ack);
        byte[] synAckBytes = synAckSegment.makeSimpleSegment();
        DatagramPacket synAckPacket = new DatagramPacket(synAckBytes, synAckBytes.length, context.getAddress(), CLIENT_PORT);
        serverSocket.send(synAckPacket);
        logger.info("SYN-ACK sent");

        //Receiving ACK
        byte[] ackBytes = new byte[MAX_BUF_SIZE];
        DatagramPacket ackPacket = new DatagramPacket(ackBytes, 0, ackBytes.length);
        serverSocket.receive(ackPacket);
        logger.info("Ack received. Starting to receive messages");

        context.setSeq(seq);
        context.setAck(ack);
        return context;
    }
}