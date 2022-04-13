package ru.nsu.gorin.networks.udp;

public final class Constants {
    public static final int SERVER_PORT = 1005;
    public static final int CLIENT_PORT = 1010;

    public static final int TIMEOUT = 3000;
    public static final int CLOSE_TIMEOUT = 15000;
    public static final int MAX_BUF_SIZE = 64000;
    public static final int MAX_BUF_SIZE_WITHOUT_FIELDS = MAX_BUF_SIZE - 3;

    public static final int CONDITION_OF_FAILURE = 4;

    public static final int MAX_SENDING_AMOUNT = 4;

    public static final int NO_DATA = 0;

    public static final String EMPTY_STRING = "";

    public static final int SEQ_POS = 0;
    public static final int ACK_POS = 1;
    public static final int LENG_POS = 2;
    public static final int DATA_OFFSET = 3;
}
