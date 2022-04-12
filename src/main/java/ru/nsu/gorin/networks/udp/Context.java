package ru.nsu.gorin.networks.udp;

import java.net.InetAddress;

/**
 * Вспомогательный класс-контекст для удобного хранения и передачи данных между методами.
 * В особенности seq и ack
 */
public class Context {
    private int seq;
    private int ack;
    byte[] sendBuffer;

    private boolean connectionStatus;
    private boolean sendingStatus;

    private boolean receivingStatus;

    InetAddress address;

    public Context(int seq, int ack, InetAddress address) {
        this.seq = seq;
        this.ack = ack;

        this.address = address;

        connectionStatus = false;
        sendingStatus = false;

        receivingStatus = false;
    }

    public int getSeq() {
        return seq;
    }
    public void setSeq(int seq) {
        this.seq = seq;
    }


    public int getAck() {
        return ack;
    }
    public void setAck(int ack) {
        this.ack = ack;
    }


    public byte[] getSendBuffer() {
        return sendBuffer;
    }
    public void setSendBuffer(byte[] sendBuffer) {
        this.sendBuffer = sendBuffer;
    }


    public boolean hasConnected() {
        return connectionStatus;
    }
    public void setConnectionStatus(boolean connectionStatus) {
        this.connectionStatus = connectionStatus;
    }


    public boolean hasSent() {
        return sendingStatus;
    }
    public void setSendingStatus(boolean sendingStatus) {
        this.sendingStatus = sendingStatus;
    }


    public boolean hasReceived() {
        return receivingStatus;
    }
    public void setReceivingStatus(boolean receivingStatus) {
        this.receivingStatus = receivingStatus;
    }


    public InetAddress getAddress() {
        return address;
    }
}
