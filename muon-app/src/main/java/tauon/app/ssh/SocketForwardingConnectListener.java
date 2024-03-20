package tauon.app.ssh;

import net.schmizz.concurrent.Event;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.Channel;
import net.schmizz.sshj.connection.channel.SocketStreamCopyMonitor;
import net.schmizz.sshj.connection.channel.forwarded.ConnectListener;
import org.apache.log4j.Logger;
import org.newsclub.net.unix.AFSocketAddress;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class SocketForwardingConnectListener implements ConnectListener {
    private static final Logger LOG = Logger.getLogger(SocketForwardingConnectListener.class);
    
    protected final SocketAddress addr;
    
    public SocketForwardingConnectListener(SocketAddress addr) {
        this.addr = addr;
    }
    
    public void gotConnect(Channel.Forwarded chan) throws IOException {
        LOG.debug("New connection from " + chan.getOriginatorIP() + ":" + chan.getOriginatorPort());
        
        Socket sock;
        if (addr instanceof AFSocketAddress) {
            sock = ((AFSocketAddress) addr).getAddressFamily().newSocket();
        } else {
            sock = new Socket();
        }
        
        sock.setSendBufferSize(chan.getLocalMaxPacketSize());
        sock.setReceiveBufferSize(chan.getRemoteMaxPacketSize());
        sock.connect(this.addr);
        chan.confirm();
        
        Event<IOException> soc2chan = (new StreamCopier(sock.getInputStream(), chan.getOutputStream(), chan.getLoggerFactory())).bufSize(chan.getRemoteMaxPacketSize()).spawnDaemon("soc2chan");
        Event<IOException> chan2soc = (new StreamCopier(chan.getInputStream(), sock.getOutputStream(), chan.getLoggerFactory())).bufSize(chan.getLocalMaxPacketSize()).spawnDaemon("chan2soc");
        SocketStreamCopyMonitor.monitor(5, TimeUnit.SECONDS, chan2soc, soc2chan, chan, sock);
    }
}
