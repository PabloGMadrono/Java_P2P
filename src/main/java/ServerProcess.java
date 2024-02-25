import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Map;

public class ServerProcess extends Thread {
    public ServerSocket listener;
    public int port;
    public String currID;
    public Map<String,boolean[]> idToBitField;
    public int pieceSize;
    public String fileName;
    public int numOfServers;

    public ArrayList<Integer> newPieces;
    public  Map<String, Boolean> idToDone;

    public volatile boolean shouldBreak = false;
    public static int magicKiller = 999999842;


    public ServerProcess(String port, String currID, ArrayList<String> peerIDs, Map<String,boolean[]> idToBitField, int pieceSize, String fileName, ArrayList<Integer> newPieces, Map<String, Boolean> idToDone) {
        this.port = Integer.parseInt(port);
        this.currID = currID;
        this.idToBitField = idToBitField;
        this.pieceSize = pieceSize;
        this.fileName = fileName;

        this.idToDone = idToDone;

        numOfServers = peerIDs.size();

        // find number of servers the peer will need to create
        for(int i = 0; i < peerIDs.size(); i++) {
            numOfServers--;
            if(currID.equals(peerIDs.get(i))) {
                i = peerIDs.size();
            }
        }

        this.newPieces = newPieces;

        start();
    }

    // Initialize the serverSocket with port number, then wait for peer connections
    public void run() {
        try {
            System.out.println("Trying to bind " + port); // test message
            listener = new ServerSocket(this.port);
            System.out.println("Server created, waiting for peers"); // test message

            // wait for connections
            int numAccept = 0; // used to terminate the number of connections later on
            while(numAccept < numOfServers) // exit when connected to all peers excluding self
            {
                numAccept += 1;
                new ServerProcessHandler(listener.accept(), currID, this, idToBitField, pieceSize, fileName, newPieces, idToDone).start();
                System.out.println("Handling " + numAccept); // test message

                if(shouldBreak){
                    System.out.println("Magic number detected Exiting.");
                    System.exit(-1);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("I/O Exception happen when initialize server socket");
            System.out.println(e);
        }
    }

    public static  void main(String[] args) throws  Exception {
        // Send HandShake message to other peers
//        RemotePeerInfo currPeer = peerInfoMap.get(currProcessID);
//        String peerAddress = currPeer.peerAddress;
//        String port = currPeer.peerPort;
        // The server needs to be on before other peers connect to it

//        s = new ServerProcess(port);
//        c = new ClientProcess(peerAddress,port,currProcessID);
        //new ServerProcess("5000");
    }

}
