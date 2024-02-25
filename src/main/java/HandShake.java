import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HandShake {
    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
    private static final int ZERO_BITS_LENGTH = 10; // 10-byte zero bits
    private static final int PEER_ID_SIZE = 4; // 4-byte peer ID
    private static final int HANDSHAKE_LENGTH = 32; // Total handshake message length
    private ByteArrayOutputStream byteStream;

    public HandShake(){};

    public byte[] generateHandShakeMsg(int peerID) throws IOException{
        // Write the handshake header
        byteStream = new ByteArrayOutputStream();
        byteStream.write(HANDSHAKE_HEADER.getBytes());
        // Write the 10 byte zero bits
        byteStream.write(new byte[ZERO_BITS_LENGTH]);
        // Write 4-byte peerID
        byte[] peerIDBytes = ByteBuffer.allocate(4).putInt(peerID).array();
        byteStream.write(peerIDBytes);
        return byteStream.toByteArray();
    }
    public int parseHandShakeMsg(byte[] receivedMsg) throws IOException{
        System.out.println("Received Handshake");
        if(receivedMsg.length!=HANDSHAKE_LENGTH){
            throw new IOException("Incorrect handshake message length");
        }
       // System.out.println("No problem so far");
        // Extract header message by its portion
        byte[] headerBytes = Arrays.copyOfRange(receivedMsg,0,18);
        byte[] zeroBytes = Arrays.copyOfRange(receivedMsg,18,28);
        byte[] peerIDBytes = Arrays.copyOfRange(receivedMsg,28,32);
        //System.out.println("seems good");
        // Reconstruct the header
        String header = new String(headerBytes);
        System.out.println("The header message is " + header);
        if(!HANDSHAKE_HEADER.equals(header))
        {
            throw new IOException("Invalid handshake header");
        }
       // System.out.println("Reach");
        // Reconstruct zero bits
        byte[] expectedZeroBits = new byte[ZERO_BITS_LENGTH];
        if(!Arrays.equals(expectedZeroBits,zeroBytes))
        {
            throw new IOException("Invalid zero bits section");
        }
      //  System.out.println("No problem in reconstructing empty byte array");

        int receivedPeerID = ByteBuffer.wrap(peerIDBytes).getInt();
        return receivedPeerID;
        // The server needs to know who's connect to it by checking id
      //  s= new ServerProcessHandler();
      //  s.setPeerID(receivedPeerID);
    }
}
