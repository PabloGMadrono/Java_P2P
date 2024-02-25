import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public class ServerProcessHandler extends Thread {
    public String currID;
    public String peerID;
    Socket connection;
    public Map<String,boolean[]> idToBitField;
    public int pieceSize;
    public String fileName;

    public ArrayList<Integer> newPieces;
    int newIndex;
    public  Map<String, Boolean> idToDone;

    private ServerProcess serverProcess;
    public ObjectOutputStream out;
    public ObjectInputStream in;

    HandShake hs;
    HandleMessage m;

    public ServerProcessHandler() {}

    public ServerProcessHandler(Socket connection, String currID, ServerProcess serverProcess, Map<String,boolean[]> idToBitField, int pieceSize, String fileName, ArrayList<Integer> newPieces, Map<String, Boolean> idToDone) {
        this.connection = connection;
        this.currID = currID;
        this.serverProcess = serverProcess;
        this.idToBitField = idToBitField;
        this.pieceSize = pieceSize;
        this.fileName = fileName;

        this.newPieces = newPieces;
        newIndex = 0;

        this.idToDone = idToDone;
    }

    // facilitates messaging between client
    public void run() {
        try {
            out = new ObjectOutputStream(this.connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(this.connection.getInputStream());

            // wait to receive handshake from peer
            hs = new HandShake();
            byte[] receivedMessage = (byte[])in.readObject();
            int receivedPeerID = hs.parseHandShakeMsg(receivedMessage);
            peerID = String.valueOf(receivedPeerID);

            Logger.madeTCPConnection(peerID);

            System.out.println("Received peer ID: " + receivedPeerID); // test message

            if(receivedPeerID == ServerProcess.magicKiller){
                this.serverProcess.shouldBreak = true;
            }

            // send this peer's id (part of handshake)
            out.writeObject(currID);
            out.flush();

            m = new HandleMessage(currID, peerID, pieceSize, fileName);
            byte msgType;
            boolean interested;
            int pieceIndex;
            boolean done = false;

            // expecting bitField message, sends bitField in return
            receivedMessage = (byte[])in.readObject();
            interested = m.handleBFMsg(receivedMessage, idToBitField);
            out.writeObject(m.genBFMsg(idToBitField.get(currID)));

            // expecting interested message, send interested or not interested message
            receivedMessage = (byte[])in.readObject();

            msgType = receivedMessage[4];

            if(msgType == 2) { Logger.receiveInterested(peerID); }         // received interested message
            else if(msgType == 3) { Logger.receiveNotInterested(peerID); } // received not interested message

            if(interested) { out.writeObject(m.genIntMsg()); }
            else { out.writeObject(m.genNotIntMsg()); }

            // while loop to receive/send messages
            while(!done && connection.isConnected()) {
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
                // sends a bitField message if the current peer is done
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

            Logger.downloadComplete();

        }
        catch(IOException e){
            System.out.println("Failed to initialize server side stream");
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        finally {
            //Close connections
            try {
                System.out.println("Closing server");

                in.close();
                out.close();
                connection.close();
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
