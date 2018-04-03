package shapedirection;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bitcoinj.core.AddressMessage;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerThread extends Thread {
  private static final Logger log = LoggerFactory.getLogger(CrawlerThread.class);

  public void run() {
    // Continuously get peers and try to connect to them.
    while (true) {
      PeerGroup peerGroup = CryptoputtyApplication.kit.peerGroup();
      if (peerGroup == null) {
        continue;
      }
      List<Peer> connectedPeers = peerGroup.getConnectedPeers();
      Collections.shuffle(connectedPeers);
      for (Peer peer : connectedPeers) {
        try {
          Thread.sleep(1000);
          AddressMessage newPeers = peer.getAddr().get(5000, TimeUnit.MILLISECONDS);
          for (PeerAddress newPeer : newPeers.getAddresses()) {
            peerGroup.addAddress(newPeer);
          }
        } catch (TimeoutException e) {
          // Don't do anything...
        } catch (InterruptedException | ExecutionException e) {
          log.warn("failed to get peers");
          e.printStackTrace();
        }
      }
    }
  }
}
