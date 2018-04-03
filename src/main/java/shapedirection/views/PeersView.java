package shapedirection.views;

import io.dropwizard.views.View;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.bitcoinj.core.Peer;
import shapedirection.CryptoputtyApplication;

public class PeersView extends View {
  List<String> peers;

  public PeersView() {
    super("peers.mustache");
    peers = new ArrayList<>();
    for (Peer peer : CryptoputtyApplication.kit.peerGroup().getConnectedPeers()) {
      InetAddress addr = peer.getAddress().getAddr();
      peers.add(String.format("%s (%s)", addr.getHostAddress(), addr.getHostName()));
    }
  }

  public List<String> getPeers() {
    return peers;
  }

  public int getNumPeers() {
    return peers.size();
  }
}
