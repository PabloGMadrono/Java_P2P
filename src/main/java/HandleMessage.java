import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;


public class HandleMessage {
    private ByteArrayOutputStream byteStream;
    public String currID;
    public String peerID;
    public int pieceSize;
    public String fileName;

    public HandleMessage() {}

    public HandleMessage(String currID, String peerID, int pieceSize, String fileName) {
        this.currID = currID;
        this.peerID = peerID;
        this.pieceSize = pieceSize;
        this.fileName = fileName;
    }

    // Generate a message string given a string (used for testing, message type is set as 8)
    public byte[] genStrMsg(String msg) throws IOException {
        byteStream = new ByteArrayOutputStream();

        // Write the header
        byte[] msgBytes = msg.getBytes();
        byte[] msgLength = ByteBuffer.allocate(4).putInt(msgBytes.length).array();
        byteStream.write(msgLength);
        byteStream.write((byte)8);
        byteStream.write(msgBytes);

        return byteStream.toByteArray();
    }

    // Generate bitField message given the peer's bitField
    public byte[] genBFMsg(boolean[] bitField) throws IOException {
        byteStream = new ByteArrayOutputStream();

        // create the byte array from the bitField
        int size = bitField.length / 8;
        if(bitField.length % 8 != 0) { size++; }

        // "increase" the size of the bitfield to fit a byte
        boolean[] bf = new boolean[bitField.length + (8 - (bitField.length % 8))];
        System.arraycopy(bitField, 0, bf, 0, bitField.length);

        byte[] msgBytes = new byte[size];
        for (int entry = 0; entry < msgBytes.length; entry++) {
            for (int bit = 0; bit < 8; bit++) {
                if (bf[entry * 8 + bit]) {
                    msgBytes[entry] |= (128 >> bit);
                }
            }
        }

        byte[] msgLength = ByteBuffer.allocate(4).putInt(msgBytes.length).array();
        byteStream.write(msgLength);
        byteStream.write((byte)5);
        byteStream.write(msgBytes);

        return byteStream.toByteArray();
    }

    // Generate interested message
    public byte[] genIntMsg() throws IOException {
        byteStream = new ByteArrayOutputStream();

        byte[] msgLength = {0, 0, 0, 0}; // length is zero
        byteStream.write(msgLength);
        byteStream.write((byte)2);

        return byteStream.toByteArray();
    }

    // Generate not interested message
    public byte[] genNotIntMsg() throws IOException {
        byteStream = new ByteArrayOutputStream();

        byte[] msgLength = {0, 0, 0, 0}; // length is zero
        byteStream.write(msgLength);
        byteStream.write((byte)3);

        return byteStream.toByteArray();
    }

    // Generate a piece message of the given piece index
    public byte[] genReqMsg(int pieceIndex) throws IOException {
        byteStream = new ByteArrayOutputStream();

        byte[] msgBytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        byte[] msgLength = ByteBuffer.allocate(4).putInt(msgBytes.length).array();

        byteStream.write(msgLength);
        byteStream.write((byte)6);
        byteStream.write(msgBytes);

        return byteStream.toByteArray();
    }

    // Generate have message
    public byte[] genHaveMsg(int pieceIndex) throws IOException {
        byteStream = new ByteArrayOutputStream();

        byte[] index = ByteBuffer.allocate(4).putInt(pieceIndex).array(); // index of piece
        byte[] msgLength = ByteBuffer.allocate(4).putInt(index.length).array();

        byteStream.write(msgLength);
        byteStream.write((byte)4);
        byteStream.write(index);

        return byteStream.toByteArray();
    }

    // Generate piece message, reads the given piece index of the file
    public byte[] genPieceMsg(int pieceIndex) throws IOException {
        byteStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[pieceSize];
        int byteIndex = pieceIndex * pieceSize;

        // read from file
        RandomAccessFile file = new RandomAccessFile("../../../peer_" + currID + "/" + fileName, "rw");
        file.seek(byteIndex);
        int read = file.read(buffer); // # of bytes actually read
        file.close();

        byte[] data = Arrays.copyOfRange(buffer, 0, read);           // only send bits actually read
        byte[] index = ByteBuffer.allocate(4).putInt(pieceIndex).array(); // index of piece
        byte[] msgLength = ByteBuffer.allocate(4).putInt(data.length + index.length).array();

        byteStream.write(msgLength);
        byteStream.write((byte)7);
        byteStream.write(index);
        byteStream.write(data);

        return byteStream.toByteArray();
    }

    // Removes the header portion of a received string message and return the string
    public String getMsgStr(byte[] msg) throws IOException{
        if(msg.length < 5) {
            throw new IOException("Missing message header information");
        }

        // Extract header length and return actual message portion
        byte[] msgLengthBytes = Arrays.copyOfRange(msg,0,4);
        int msgLength = ByteBuffer.wrap(msgLengthBytes).getInt();

        return new String(Arrays.copyOfRange(msg,5,5 + msgLength));
    }

    // Updates the bifField map given a bitField message and returns a bool if the peer has a needed piece
    public boolean handleBFMsg(byte[] msg, Map<String,boolean[]> idToBitField) throws IOException{
        if(msg.length < 5) {
            throw new IOException("Missing message header information");
        }

        // Extract header length and bitField
        byte[] msgLengthBytes = Arrays.copyOfRange(msg,0,4);
        int msgLength = ByteBuffer.wrap(msgLengthBytes).getInt();

        byte[] bitField = Arrays.copyOfRange(msg,5,5 + msgLength);

        // update the bit Field of the peer
        BitSet b = BitSet.valueOf(bitField);

        for(int i = 0; i < idToBitField.get(peerID).length / 8; i++) {
            idToBitField.get(peerID)[i] = b.get(i);
        }

        System.out.println("Received peer's bit field (as signed bytes): "); // test message
        for(int i = 0; i < bitField.length; i++) { System.out.print((bitField[0]) + " "); } // test message
        System.out.println(""); // test message

        // checks to see if peer has any needed pieces
        boolean interested = false;

        for(int i = 0; i < idToBitField.get(currID).length; i++) {
            if(!idToBitField.get(currID)[i] && idToBitField.get(peerID)[i]) {
                interested = true;
                i = idToBitField.get(currID).length;
            }
        }

        return interested;
    }

    // find the piece index of a request message
    public int handleRequest(byte[] msg) {

        // extract header information & data payload
        byte[] msgLengthBytes = Arrays.copyOfRange(msg,0,4);
        int msgLength = ByteBuffer.wrap(msgLengthBytes).getInt();

        byte[] pieceIndexBytes = Arrays.copyOfRange(msg,5,5 + msgLength);
        return ByteBuffer.wrap(pieceIndexBytes).getInt();
    }

    // write to the peer file the obtained data, also update bitField
    public void handlePiece(byte[] msg, Map<String,boolean[]> idToBitField) throws IOException {

        // extract header information & data payload
        byte[] msgLengthBytes = Arrays.copyOfRange(msg,0,4);
        int msgLength = ByteBuffer.wrap(msgLengthBytes).getInt();

        byte[] pieceIndexBytes = Arrays.copyOfRange(msg,5,9);
        int pieceIndex = ByteBuffer.wrap(pieceIndexBytes).getInt();

        int byteIndex = pieceIndex * pieceSize;

        byte[] data = Arrays.copyOfRange(msg,9, 9 + (msgLength - 4)); // msgLength - 4 to exclude the 4 byte piece index

        // write to file
        RandomAccessFile file = new RandomAccessFile("../../../peer_" + currID + "/" + fileName, "rw");
        file.seek(byteIndex);
        file.write(data);
        file.close();

        System.out.println("got piece: " + pieceIndex + " from peer " + peerID); // test message

        // update bitField
        idToBitField.get(currID)[pieceIndex] = true;
    }

    // update the peer's bitField with the given index and return if the current peer will be interested
    public boolean handleHave(byte[] msg, Map<String,boolean[]> idToBitField) {

        // extract header information & data payload
        byte[] msgLengthBytes = Arrays.copyOfRange(msg,0,4);
        int msgLength = ByteBuffer.wrap(msgLengthBytes).getInt();

        byte[] pieceIndexBytes = Arrays.copyOfRange(msg,5,5 + msgLength);
        int pieceIndex =  ByteBuffer.wrap(pieceIndexBytes).getInt();

        Logger.receiveHave(peerID, pieceIndex);

        idToBitField.get(peerID)[pieceIndex] = true;

        if(idToBitField.get(currID)[pieceIndex]) { return false; } // current peer already has piece
        return true;
    }

    // finds a needed piece the peer has, if there is not one then return -1
    public int findPieceToRequest(Map<String,boolean[]> idToBitField) {
        int pieceIndex = -1;

        // find the piece indexes of all pieces the peer has that this peer does not have
        ArrayList<Integer> neededPieces = new ArrayList<>();

        for(int i = 0; i < idToBitField.get(currID).length; i++) {
            if((!idToBitField.get(currID)[i]) && idToBitField.get(peerID)[i]) {
                neededPieces.add(i);
            }
        }

        // select a random piece
        Random rand = new Random();

        if(neededPieces.size() != 0) {
            pieceIndex = neededPieces.get(rand.nextInt(neededPieces.size()));
        }

        return pieceIndex;
    }

    // checks if all peers are done and return true if so
    public boolean checkIfDone(Map<String,boolean[]> idToBitField, Map<String, Boolean> idToDone) {

        for (Map.Entry<String,Boolean> sent : idToDone.entrySet()) {
            if(!sent.getValue()) {
                return false;
            }
        }

        for (Map.Entry<String,boolean[]> bitField : idToBitField.entrySet()) {
            for(int i = 0; i < bitField.getValue().length; i++) {
                if (!bitField.getValue()[i]) {
                    return false;
                }
            }
        }

        return true;
    }

    // checks if current peer is done and return true if so
    public boolean checkSelf(Map<String,boolean[]> idToBitField) {

        for(int i = 0; i < idToBitField.get(currID).length; i++) {
            if (!idToBitField.get(currID)[i]) {
                return false;
            }
        }

        return true;
    }
}
