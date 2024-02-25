/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE 
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */

import java.io.*;
import java.util.*;

/*
 * The StartRemotePeers class begins remote peer processes. 
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 * You must modify this program a little bit if your peer processes are written in C or C++.
 * Please look at the lines below the comment saying IMPORTANT.
 */
public class StartRemotePeers {

	public Vector<RemotePeerInfo> peerInfoVector;
	
	public void getConfiguration()
	{
		String st;
		int i1;
		peerInfoVector = new Vector<RemotePeerInfo>();
		try {
			BufferedReader in = new BufferedReader(new FileReader("resources/PeerInfo.cfg"));
			while((st = in.readLine()) != null) {
				
				 String[] tokens = st.split("\\s+");
		    	 //System.out.println("tokens begin ----");
			     //for (int x=0; x<tokens.length; x++) {
			     //    System.out.println(tokens[x]);
			     //}
		         //System.out.println("tokens end ----");
			    
			     peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
			
			}
			
			in.close();
		}
		catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}

	public static class ProcessTuple{
		Process p;
		String n;
		public ProcessTuple(Process p, String n){
			this.p = p;
			this.n = n;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		List<ProcessTuple> processList = new ArrayList<>();
		try {
			StartRemotePeers myStart = new StartRemotePeers();
			myStart.getConfiguration();
					
			// get current path
			// String path = System.getProperty("user.dir");
			String path = "P2P_Project/src/main/java";
			FileOutputStream bf = new FileOutputStream(new File("test.txt"));
			// start clients at remote hosts
			for (int i = 0; i < myStart.peerInfoVector.size(); i++) {

				RemotePeerInfo pInfo = (RemotePeerInfo) myStart.peerInfoVector.elementAt(i);
				
				System.out.println("Start remote peer " + pInfo.peerId +  " at " + pInfo.peerAddress );
				
				// *********************** IMPORTANT *************************** //
				// If your program is JAVA, use this line.
				// Runtime.getRuntime().exec("ssh " + "yiheng.qiu@"+ pInfo.peerAddress + " cd " + path + "; java PeerProcess.java " + pInfo.peerId);

				 // This is for testing purpose, these lines will generate out.txt

				/*
				String user = "yiheng.qiu";
				String host = pInfo.peerAddress;
				String command = " cd " + path + "; javac PeerProcess.java; java PeerProcess " + pInfo.peerId;

				List<String> commandsList = new ArrayList<>();
				commandsList.add("ssh ");
				commandsList.add(user + "@" + host);
				commandsList.add(command);

				ProcessBuilder builder = new ProcessBuilder(commandsList);
				builder.redirectOutput(new File("out.txt"));
				builder.redirectErrorStream(true);

				try {
					Process p = builder.start();
				} catch (IOException e) {
					e.printStackTrace();
				}

				 */

				String line = String.format("ssh yiheng.qiu@%s cd %s ; javac PeerProcess.java ; java PeerProcess %s", pInfo.peerAddress, path, pInfo.peerId);
//				String line = String.format("ssh yiheng.qiu@%s pkill -u yiheng.qiu", pInfo.peerAddress);
				System.out.println("Executing "+ line);
//				String line = "ssh \" + \"yiheng.qiu@\"+ pInfo.peerAddress + \" cd \" + path + \"; java PeerProcess.java \" + pInfo.peerId";
				Process p = Runtime.getRuntime().exec( line  );
				processList.add(new ProcessTuple(p, line));

			}
			for(int i = processList.size() - 1; i >= 0; i--){
				ProcessTuple p = processList.get(i);
				System.out.println("Waiting for " + p.n);
				//p.p.getInputStream().transferTo(bf);
//				p.getErrorStream().transferTo(bf);
				int exitVal = p.p.waitFor();
//				System.out.println(line + "Completed");
//					Process p = Runtime.getRuntime().exec( "echo 1123" );
//				ProcessBuilder builder = new ProcessBuilder("echo 123");
//				builder.redirectOutput(f);
//				builder.redirectError(f);
//
//				Process process = builder.start();
//				process.waitFor();
//
//				System.out.println(process.exitValue());



				// If your program is C/C++, use this line instead of the above line.
				//Runtime.getRuntime().exec("ssh " + pInfo.peerAddress + " cd " + path + "; ./peerProcess " + pInfo.peerId);
			}
//			System.out.println("Starting all remote peers has done." );
			bf.close();
		}
		catch (Exception ex) {
			System.out.println(ex);
		}
		finally {

		}

	}

}
