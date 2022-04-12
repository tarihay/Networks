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
        Scanner scanner = new Scanner(System.in);

        try (DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT)) {
            logger.info("Client successfully started running");
            int seq = 0;
            int ack = 0;
            InetAddress address = InetAddress.getLocalHost();
            Context context = new Context(seq, ack, address);

            //Making triple Handshake and checking correctness of numbers (seq, ack) of SYN-ACK and SYN
            context = checkConnectionByTripleHandshake(clientSocket, context);
            seq = context.getSeq();
            ack = context.getAck();

            if (context.hasConnected()) {
                String message;

                int sendCount = 0;

                byte[] sendBuffer = new byte[MAX_BUF_SIZE];
                context.setSendBuffer(sendBuffer);
                context.setSeq(seq);
                context.setAck(ack);
                while (true) {
                    if (!context.hasSent()) {
                        message = scanner.nextLine();

                        //Setting increased values of seq and ack to context
                        // to send the message with right fields further
                        context.setSeq(++seq);
                        context.setAck(++ack);

                        sendMessageToServer(clientSocket, message, context);
                        //Updating sendBuffer after executing sending the message
                        sendBuffer = context.getSendBuffer();

                        sendCount = 1;

                        context.setSendingStatus(true);
                    }
                    else {
                        if (sendCount >= MAX_SENDING_AMOUNT) {
                            logger.info("Too many tries to send the message. Client is closing connection");

                            //exiting from loop to stop sending messages to server
                            break;
                        }
                        else {
                            sendCount++;
                        }

                        //resending the message
                        DatagramPacket segment = new DatagramPacket(sendBuffer, sendBuffer.length, address, SERVER_PORT);
                        clientSocket.send(segment);

                        logger.info("Client is resending the message");
                    }


                    context = receiveAckAndCheckCorrectness(clientSocket, context);
                }

                clientSocket.close();
            }
            else {
                logger.info("Connection has not been established. Server sent wrong ack. Closing the app");
            }
        }
        catch (Exception ex) {
            logger.info("Client closed connection");
        }
    }


    /**
     * Метод делает тройное рукопожатие и проверяет пришедший номер SYN-ACK'а и отправленного SYN
     * @param clientSocket сокет, с помощью которого клиент отправляет  и получает данные от сервера
     * @param context используется для удобства передачи и изменения seq и ack
     * @return Возвращает context с новыми seq и ack
     * @throws IOException в случае неудачного получения или отправки через сокет
     *
     * @see Context
     */
    private static Context checkConnectionByTripleHandshake(DatagramSocket clientSocket, Context context) throws IOException {
        int seq = context.getSeq();
        int ack = context.getAck();

        //Making SYN and sending it to Server
        Segment synSegment = new Segment(EMPTY_STRING, seq, ack);
        byte[] mes = synSegment.makeSimpleSegment();
        DatagramPacket syn = new DatagramPacket(mes, mes.length, context.getAddress(), SERVER_PORT);
        clientSocket.send(syn);
        logger.info("Client sent SYN");

        //Making byte array to receive SYN-ACK from Server
        byte[] synAckBytes = new byte[MAX_BUF_SIZE];
        DatagramPacket synAck = new DatagramPacket(synAckBytes, 0, synAckBytes.length);
        clientSocket.receive(synAck);
        logger.info("Client successfully received SYN-ACK");

        //Making ACK and sending it to Server
        if (synAckBytes[ACK_POS] == mes[SEQ_POS] + 1) {
            Segment ackSegment = new Segment(EMPTY_STRING, ++seq, ++ack);
            byte[] ackBytes = ackSegment.makeSimpleSegment();
            DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, context.getAddress(), SERVER_PORT);
            clientSocket.send(ackPacket);
            logger.info("Client sent ACK");

            context.setConnectionStatus(true);
        }
        else {
            context.setConnectionStatus(false);
        }

        context.setSeq(seq);
        context.setAck(ack);
        return context;
    }

    /**
     * Метод отправляет полученное в консоли сообщение на сервер
     * @param clientSocket используется для отправки сообщения
     * @param message полученное сообщение
     * @param context используется для удобного хранения необходимых данных (полей сегмента)
     * @return возвращает context с измененным полем sendBuffer
     * @throws IOException в случае неудачной отправки
     *
     * @see Context
     */
    private static Context sendMessageToServer(DatagramSocket clientSocket, String message, Context context) throws IOException {
        int seq = context.getSeq();
        int ack = context.getAck();
        byte[] sendBuffer = context.getSendBuffer();

        //building the segment
        Segment commonSegment = new Segment(message, seq, ack);

        for (int i = 0; i <= commonSegment.getIterationCount(); i++) {

            //putting all fields together
            sendBuffer = commonSegment.convertDataToSegment(i);

            DatagramPacket segment = new DatagramPacket(sendBuffer, sendBuffer.length, context.getAddress(), SERVER_PORT);
            clientSocket.send(segment);
            logger.info("Client sent the message number " + seq);
        }

        context.setSendBuffer(sendBuffer);
        return context;
    }

    /**
     * Метод получает ACK с сервера и проверяет корректность полей полученного сегмента
     * @param clientSocket используется для получения сообщения
     * @param context используется для удобного хранения seq и изменения статуса получения
     * @return Возвращает изменный context
     *
     * @see Context
     */
    private static Context receiveAckAndCheckCorrectness(DatagramSocket clientSocket, Context context) {
        int seq = context.getSeq();

        Random random = new Random();
        int randomNum = random.nextInt(5);

        byte[] ackBytes = new byte[MAX_BUF_SIZE];
        DatagramPacket ackPacket = new DatagramPacket(ackBytes, 0, ackBytes.length);

        try {
            clientSocket.setSoTimeout(TIMEOUT);
            clientSocket.receive(ackPacket);

            //Don't receive ACK by random
            if (randomNum != CONDITION_OF_FAILURE) {
                logger.info("Client received the ack to message number " + seq);
                if (ackBytes[ACK_POS] - 1 == seq) {
                    context.setSendingStatus(false);
                }
            }
            else {
                logger.info("Client didn't receive the ack to the message number " + seq);
            }
        }
        catch (IOException ex) {
            logger.info("Client didn't receive the message number " + seq + ". Starting to resend it");
        }

        return context;
    }
}
