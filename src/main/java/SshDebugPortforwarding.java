import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Trimmed down version of the PortForwardingL example from jsch,
 * <p>
 * Instead of using the UI that sample provides here we just provide all the
 * required bits of info by pasting them into the code directly as constants.
 * <p>
 * THe GUI code clutters things up and makes it rather hard to see
 * what really that example is doing.
 *
 * @author Kris De Volder
 */
public class SshDebugPortforwarding {

	private String appGuid = "c8111c9e-55b4-4afd-a85b-a0b02e358590";
	private int instanceIndex = 0;
	
	//Output from: cf app demo --guid
	private String userName = "cf:"+appGuid+"/"+instanceIndex;
	//private String userName = "pi";
	
	//The port where ssh daemon is running (this info is available from CF cloudcontroller)
	private int sshPort = 2222; //default would be 22
	//private int sshPort = 22; //default would be 22
	
	//The password (token you get from 'cf get-ssh-code')
	//!!!This is a one time code!!!
	private String password = "tLsa0O";
	
	//The host name where ssh daemon is running: ssh hostname (this info is available from CF cloudcontroller)
	private String hostName = "ssh.run.pivotal.io";
	//private String hostName = "fileserver";
	
	//The port we want to bind locally 
	private int localDebugPort = 8998;
	//private int localPort = 9091;
	
	//The host on the remote end that we want to forward traffic to
	private String remoteHost = "localhost"; 
	   //                       ^^^^^^^^^^ localhost, that's odd? 
	   // It means that we want bind on the same host our ssh connects opens 
	   //to (i.e. the 'localhost' is resolved in the remote network, not on the client side)

	//The remote port that we want to forward traffic to 
	//  (i.e. this is prt the remote process is binding its debugger to via JVM arguments)
	private int remoteDebugPort = 8998;
	//private int remotePort = 9091;
	
	private String hostPrint = "e7:13:4e:32:ee:39:62:df:54:41:d7:f7:8b:b2:a7:6b";
	
	private UserInfo userinfo = new UserInfo() {
		
		private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		public void showMessage(String message) {
			System.out.println("message: "+message);
		}
		
		public boolean promptYesNo(String message) {
			System.out.println("Yes/No? : "+message);
			//TODO: This gets called to confirm host fingerprint. We auto confirm it.
			// The right way to do this is to somehow register the host print in the ssh client ahead of time.
			// Haven't figured out how to do that yet.
			return true;
//			return "y".equals(readline());
		}
		
		private String readline() {
			try {
				return reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "";
		}

		public boolean promptPassword(String message) {
			return true;
		}
		
		public boolean promptPassphrase(String message) {
			return false;
		}
		
		public String getPassword() {
			return password;
		}
		
		public String getPassphrase() {
			return null;
		}
	};

	public void run() throws Exception {
		JSch jsch = new JSch();
		Session session = jsch.getSession(userName, hostName, sshPort);
// TODO: this attempt to register host fingerprint doesn't work. Probably something to do with
//   how to encode the fingerprint properly
//		HostKeyRepository hostKeyRepo = session.getHostKeyRepository();
//		HostKey hostkey = new HostKey(hostName, HostKey.SSHRSA, keyAsBytes(hostPrint));
//		hostKeyRepo.add(hostkey, userinfo);
		
		session.setPassword(password);
		session.setUserInfo(userinfo);
		
		session.connect();

		int assignedPort = session.setPortForwardingL(localDebugPort, remoteHost, remoteDebugPort);
		System.out.println("localhost:"+assignedPort+" -> "+remoteHost+":"+remoteDebugPort);
	}
	
	private byte[] keyAsBytes(String print) throws Exception {
		String[] pieces = print.split(":");
		byte[] bytes = new byte[pieces.length];
		for (int i = 0; i < pieces.length; i++) {
			bytes[i] = (byte) (Integer.parseInt(pieces[i],16) & 0xff);
		}
		return print.getBytes("utf8");
	}

	public static void main(String[] args) throws Exception {
		new SshDebugPortforwarding().run();
	}
}
