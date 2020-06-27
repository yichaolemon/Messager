import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Sender extends Thread {
	private InetAddress dstInetAddr;
	private StringBuilder msg;
	private final Lock lock = new ReentrantLock();
	private final Condition strNotEmpty = lock.newCondition();

	public void writeMsg(String line) {
		lock.lock();
		try {
			msg.append(line);
			strNotEmpty.signal();
		}	finally {
			lock.unlock();
		}
	}

	public String getMsg() throws InterruptedException {
		lock.lock();
		try {
			while (msg.length() == 0) {
				strNotEmpty.await();
			}
			String msgString = msg.toString();
			msg.setLength(0);
			return msgString;
		} finally {
			lock.unlock();
		}
	}

	public void run() {
		while (true) {
			// tries to keep a persistent connection with server
			try {
				Socket senderSkt = new Socket(dstInetAddr, Receiver.SERVER_PORT);
				PrintWriter msgStream = new PrintWriter(senderSkt.getOutputStream(), true /*auto flushing*/);
				while (true) {
					String msgString = this.getMsg();
					msgStream.write(msgString);
					msgStream.flush();
				}
			} catch (Exception e) {
				continue;
			}
		}
	}

	public Sender(InetAddress dstInetAddr) {
		this.dstInetAddr = dstInetAddr;
		msg = new StringBuilder();
	}
}