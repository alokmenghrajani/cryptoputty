package shapedirection.health;

import com.codahale.metrics.health.HealthCheck;
import shapedirection.CryptoputtyApplication;

import static java.lang.String.format;

public class PeerHealth extends HealthCheck {
  @Override
  protected Result check() {
    if (CryptoputtyApplication.kit.peerGroup() == null) {
      return Result.unhealthy("no peerGroup");
    }
    int peers = CryptoputtyApplication.kit.peerGroup().numConnectedPeers();
    if (peers < 10) {
      return Result.unhealthy(format("low number of peers: %d", peers));
    }
    return Result.healthy();
  }
}
