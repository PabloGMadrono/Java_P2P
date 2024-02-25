import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ClientProcess extends Thread {
    public Socket requestSocket;
    public ObjectOutputStream out;
    public ObjectInputStream in;

    HandShake hs;
    HandleMessage m;

    public String currID;
    public String peerID;
    public int port;
    String peerAddress;
    public Map<String,boolean[]> idToBitField;
    public int pieceSize;
    public String fileName;

    public ArrayList<Integer> newPieces;
    int newIndex;
    public  Map<String, Boolean> idToDone;

    public ClientProcess() {}

    public ClientProcess(String peerAddress, String port, String currID, String peerID, Map<String,boolean[]> idToBitField, int pieceSize, String fileName, ArrayList<Integer> newPieces, Map<String, Boolean> idToDone) {
        this.currID = currID;
        this.peerID = peerID;
        this.port = Integer.parseInt(port);
        this.peerAddress = peerAddress;
        this.idToBitField = idToBitField;
        this.pieceSize = pieceSize;
        this.fileName = fileName;

        this.newPieces = newPieces;
        newIndex = 0;

        this.idToDone = idToDone;

        start();
    }

    // Attempt to connect to peer with given address and port
    public void run() {
        try {
            // peerAddress = "localhost"; // added to test locally

            System.out.println("Attempting to handshake peer " + peerID); // test message

            requestSocket = new Socket(peerAddress, port);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            // Send Handshake message to the connected peer
            boolean hsMessage = sendHandShakeMsg();
            if (!hsMessage) {
                System.out.println("Connected to wrong peer");

                // should exit and close if wrong peer
            }
            else {
                Logger.makeTCPConnection(peerID);
            }

            m = new HandleMessage(currID, peerID, pieceSize, fileName);
            byte[] receivedMessage;
            boolean interested;
            int pieceIndex;
            int msgType;
            boolean done = false;

            // sending initial bitField message
            out.writeObject(m.genBFMsg(idToBitField.get(currID)));

            // receiving peer's bitField & send interested or not interested message
            receivedMessage = (byte[]) in.readObject();
            interested = m.handleBFMsg(receivedMessage, idToBitField);

            if(interested) { out.writeObject(m.genIntMsg()); }
            else { out.writeObject(m.genNotIntMsg()); }

            // while loop to receive/send messages
            while(!done && requestSocket.isConnected()) {
                receivedMessage = (byte[]) in.readObject();

                msgType = receivedMessage[4];

                if(msgType == 2) { Logger.receiveInterested(peerID); }         // interested message
                else if(msgType == 3) { Logger.receiveNotInterested(peerID); } // not interested message

                // received request message, send peer the requested piece
                if((msgType == 6)) {
                    pieceIndex = m.handleRequest(receivedMessage);

                    System.out.println("Sending piece " + pieceIndex + " to peer " + peerID); // test message

                    out.writeObject(m.genPieceMsg(pieceIndex));
                    idToBitField.get(peerID)[pieceIndex] = true;
                }
                // received have message, update peer bitField and send an interested or not interested message
                else if((msgType == 4)) {
                    interested = m.handleHave(receivedMessage, idToBitField); // logger is in handleHave()

                    if(interested) { out.writeObject(m.genIntMsg()); }
                    else { out.writeObject(m.genNotIntMsg()); }
                }
                // if the current peer has gotten new pieces send have message
                else if((newPieces.size() > newIndex)) {
                    pieceIndex = newPieces.get(newIndex);
                    newIndex++;

                    out.writeObject(m.genHaveMsg(pieceIndex));
                }
                // if interested, request pieces, find next piece to request and continuously request until there are none left
                else if(interested) {
                    pieceIndex = m.findPieceToRequest(idToBitField);

                    while(pieceIndex != -1) {
                        out.writeObject(m.genReqMsg(pieceIndex));
                        receivedMessage = (byte[]) in.readObject();
                        m.handlePiece(receivedMessage, idToBitField);

                        Logger.downloadPiece(peerID, pieceIndex);

                        newPieces.add(pieceIndex); // add new piece in list for have messages
                        newIndex++;

                        pieceIndex = m.findPieceToRequest(idToBitField); // find new piece
                    }

                    interested = false;
                    out.writeObject(m.genNotIntMsg()); // peer has no needed pieces so send not interested message
                }
                // send a junk message as default in order to avoid deadlock (each peer is waiting for the other to send a message)
                // sends a corresponding message if the current peer is done
                else {
                    if(msgType == 8 && m.getMsgStr(receivedMessage).equals("1")) { Arrays.fill(idToBitField.get(peerID), true); }

                    if(m.checkSelf(idToBitField)) {
                        out.writeObject(m.genStrMsg("1"));

                        if(!idToDone.get(currID)) { Logger.downloadComplete(); }

                        idToDone.put(currID, true);
                        idToDone.put(peerID, true);
                    }
                    else { out.writeObject(m.genStrMsg("0")); }
                }

                done = m.checkIfDone(idToBitField, idToDone); // checks if all peers are done
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        finally {
            //Close connections
            try {
                System.out.println("Closing client");

                in.close();
                out.close();
                requestSocket.close();
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // Send handshake message and return false if server rejects or connected to wrong peer
    public boolean sendHandShakeMsg() throws IOException {
        hs = new HandShake();
        byte[] messageToSend = hs.generateHandShakeMsg(Integer.parseInt(currID));

        try {
            out.writeObject(messageToSend);
            out.flush();

            // incoming message from server
            String s = (String) in.readObject();
            System.out.println("Connected peer's ID: " + s); // test message

            // if connected to wrong peer, return false
            if (!s.equals(peerID)) {
                return false;
            }
        }
        catch (IOException | ClassNotFoundException e) {
            System.out.println(e);
        }

        return true;
    }

    public static void main(String[] args) throws Exception {
        ClientProcess c = new ClientProcess("localhost", "5000", "1000", "1001", new HashMap<>(), 0, "", new ArrayList<>(), new HashMap<>());
    }
}