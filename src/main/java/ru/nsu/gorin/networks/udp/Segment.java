package ru.nsu.gorin.networks.udp;

import java.io.UnsupportedEncodingException;

import static ru.nsu.gorin.networks.udp.Constants.*;

/**
 * Вспомогательный класс.
 * Используется для удобной работы с сегментами UDP
 */
public class Segment {
    private int seq;
    private int ack;
    private int messageLength;
    private byte[] data;
    private String message;

    private int iterationCount;

    /**
     * Конструктор класса segment.
     * Используется в случае, когда размер пришедшего сообщения много больше MAX_BUFF_SIZE
     * @param message пришедшее сообщение
     * @param seq номер сообщения
     * @param ack номер ack
     */
    public Segment(String message, int seq, int ack) {
        this.seq = seq;
        this.ack = ack;

        iterationCount = message.length() / MAX_BUF_SIZE_WITHOUT_FIELDS;

        if (message.length() > MAX_BUF_SIZE_WITHOUT_FIELDS) {
            messageLength = message.length() % MAX_BUF_SIZE_WITHOUT_FIELDS;
        }
        else {
            messageLength = message.length();
        }

        data = message.getBytes();
    }

    /**
     * Конструктор класса segment.
     * Используется в случае, когда размер пришедшего сообщения не превосходит MAX_BUFF_SIZE
     * @param messageData
     * @throws UnsupportedEncodingException
     */
    public Segment(byte[] messageData) throws UnsupportedEncodingException {
        seq = messageData[SEQ_POS];
        ack = messageData[ACK_POS];
        messageLength = messageData[LENG_POS];

        data = new byte[messageLength];
        System.arraycopy(messageData, DATA_OFFSET, data, 0, messageLength);
        message = new String(data, "UTF-8");
    }

    /**
     * Метод собирает байты сообщения, seq, ack в массив байтов, представляющий собой обычный сегмент
     * @param i номер итерации в цикле. Не равен нулю в случае, когда размер пришедшего сообщения превосходит MAX_BUFF_SIZE
     * @return Возвращает собранный массив байтов
     */
    public byte[] convertDataToSegment(int i) {
        byte[] sendBuffer = new byte[MAX_BUF_SIZE];

        sendBuffer[SEQ_POS] = (byte) seq;
        sendBuffer[ACK_POS] = (byte) ack;
        sendBuffer[LENG_POS] = (byte) messageLength;
        int messageBytesOffset = MAX_BUF_SIZE*i - DATA_OFFSET*i;
        if (i < iterationCount) {
            System.arraycopy(data, messageBytesOffset, sendBuffer, DATA_OFFSET, MAX_BUF_SIZE_WITHOUT_FIELDS);
        }
        else {
            System.arraycopy(data, messageBytesOffset, sendBuffer, DATA_OFFSET, messageLength);
        }

        return sendBuffer;
    }

    /**
     * Метод собирает seq и ack в массив байтов.
     * Используется в основном при отправке ACK.
     * @return Возвращает массив байт, содержащий seq и ack.
     */
    public byte[] makeSimpleSegment() {
        byte[] sendBuffer = new byte[MAX_BUF_SIZE];

        sendBuffer[SEQ_POS] = (byte) seq;
        sendBuffer[ACK_POS] = (byte) ack;
        sendBuffer[LENG_POS] = (byte) NO_DATA;

        return sendBuffer;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public int getSeq() {
        return seq;
    }

    public int getAck() {
        return ack;
    }

    public String getMessage() {
        return message;
    }
}
