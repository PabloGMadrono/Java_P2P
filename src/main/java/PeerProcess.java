
public class PeerProcess {
    public static void main(String[] args)
    {
        PeerManager p = new PeerManager(args[0]);
       // PeerManager p = new PeerManager();
        p.init();
    }
}
