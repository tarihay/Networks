package ru.nsu.gorin.networks.udp;

import java.io.UnsupportedEncodingException;

import static ru.nsu.gorin.networks.udp.Constants.*;

public class Segment {
    private int seq;
    private int ack;
    private int messageLength;
    private byte[] data;
    private String message;

    private int iterationCount;

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

    public Segment(byte[] messageData) throws UnsupportedEncodingException {
        seq = messageData[SEQ_POS];
        ack = messageData[ACK_POS];
        messageLength = messageData[LENG_POS];

        data = new byte[messageLength];
        System.arraycopy(messageData, DATA_OFFSET, data, 0, messageLength);
        message = new String(data, "UTF-8");
    }

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

    public int getMessageLength() {
        return messageLength;
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
