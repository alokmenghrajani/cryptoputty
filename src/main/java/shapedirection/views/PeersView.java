package shapedirection.views;

import io.dropwizard.views.View;
import java.util.ArrayList;
import java.util.List;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import shapedirection.CryptoputtyApplication;

public class PeersView extends View {
  List<String> peers;

  public PeersView() {
    super("peers.mustache");
    peers = new ArrayList<>();
    for (Peer peer : CryptoputtyApplication.kit.peerGroup().getConnectedPeers()) {
      PeerAddress address = peer.getAddress();
      peers.add(String.format("%s:%d", address.getAddr().getHostAddress(), address.getPort()));
    }
  }

  public List<String> getPeers() {
    return peers;
  }

  public int getNumPeers() {
    return peers.size();
  }
}
