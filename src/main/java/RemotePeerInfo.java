public class RemotePeerInfo {
    public String peerId;
    public String peerAddress;
    public String peerPort;
    public boolean hasFile;

    public RemotePeerInfo(String pId, String pAddress, String pPort) {
        peerId = pId;
        peerAddress = pAddress;
        peerPort = pPort;
    }
    public RemotePeerInfo(String pId, String pAddress, String pPort, boolean hasFile)
    {
        peerId = pId;
        peerAddress = pAddress;
        peerPort = pPort;
        this.hasFile = hasFile;
    }

}
