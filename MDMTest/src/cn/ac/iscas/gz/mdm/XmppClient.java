package cn.ac.iscas.gz.mdm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

/**
 * Use mina client to connect xmpp server
 * 
 * @author Tim
 * 
 */
public class XmppClient {
	public static void main(String[] args) {
		String hostname = "192.168.19.150";
		int port = 5222;

		SocketConnectorConfig cfg = new SocketConnectorConfig();
		cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory()));

		for (int i = 1; i <=1000; i++) {
			SocketConnector connector = new SocketConnector();
			connector.connect(new InetSocketAddress(hostname, port), new XmppProtocolHandler("tim" + i, "tim", "mina"), cfg);
		}
	}
}

class XmppProtocolHandler extends IoHandlerAdapter {
	String username = "tim";
	String password = "tim";
	String server = "server";
	String bareJid = username + "@" + server;
	String resource = "mina";

	public XmppProtocolHandler(String username, String password, String resource) {
		this.username = username;
		this.password = password;
		this.resource = resource;
		this.bareJid = username + "@" + server;
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		System.err.println("Closed. Total " + session.getReadBytes() + " byte(s)");
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		ByteBuffer buf = (ByteBuffer) message;
		while (buf.hasRemaining()) {
			System.out.print((char) buf.get());
		}
		System.out.flush();
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		sendPacket(session, sendStream(true));
		Thread.sleep(50);
		sendPacket(session, sendAuth(username, bareJid, password));
		Thread.sleep(200);
		sendPacket(session, sendStream(false));
		Thread.sleep(50);
		sendPacket(session, sendResource(username, resource));
		Thread.sleep(50);
		sendPacket(session, sendPresence(username));
	}

	private String genPassword(String bareJid, String username, String password) {
		String str = bareJid + "\0" + username + "\0" + password;
		//BASE64Encoder base64 = new BASE64Encoder();
		return str;
	}

	private StringBuilder sendStream(boolean addHeader) throws IOException {
		StringBuilder sb = new StringBuilder();
		if (addHeader)
			sb.append("<?xml version='1.0' encoding='UTF-8'?>");
		sb.append("<stream:stream to=\"").append(server).append("\" xmlns=\"jabber:client\" xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">");
		return sb;
	}

	private StringBuilder sendAuth(String username, String bareJid, String password) throws IOException {
		String pwd = genPassword(bareJid, username, password);
		StringBuilder sb = new StringBuilder();
		sb.append("<auth mechanism=\"PLAIN\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">").append(pwd).append("</auth>");
		return sb;
	}

	private StringBuilder sendResource(String username, String resource) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<iq id=\"").append(username).append("-0\" type=\"set\"><bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\"><resource>").append(resource).append("</resource></bind></iq>");
		return sb;
	}

	private StringBuilder sendPresence(String username) throws IOException {
		StringBuilder sb = new StringBuilder("<presence id=\"").append(username).append("-2\" />");
		return sb;
	}

	private void sendPacket(IoSession session, StringBuilder sb) throws Exception {
		sb.append("\r\n");
		System.out.println(sb.toString());
		session.write(sb.toString());
	}
}