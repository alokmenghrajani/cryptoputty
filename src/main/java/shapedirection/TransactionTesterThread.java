package shapedirection;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.RejectMessage;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.PreMessageReceivedEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.SendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class TransactionTesterThread extends Thread {
  private static final Logger log = LoggerFactory.getLogger(TransactionTesterThread.class);

  private ConcurrentMap<Peer, Boolean> testedPeers = new ConcurrentHashMap<>();

  public void run() {
    // Continuously get peers and try to connect to them.
    while (true) {
      try {
        Thread.sleep(5000);

        // iterate over our peers
        WalletAppKit kit = CryptoputtyApplication.kit;
        PeerGroup peerGroup = kit.peerGroup();
        List<Peer> connectedPeers = peerGroup.getConnectedPeers();
        for (Peer peer : connectedPeers) {
          if (testedPeers.containsKey(peer)) {
            continue;
          }
          log.info(format("Testing %s", peer.toString()));
          // Let's go slow...
          Thread.sleep(1000);

          // Create a normal transaction
          SendRequest request = SendRequest.to(kit.wallet().currentReceiveAddress(), Coin.COIN.divide(100));
          kit.wallet().completeTx(request);
          Transaction newTx = TransactionMutator.mutate(request.tx);
          peer.addPreMessageReceivedEventListener(Threading.SAME_THREAD, new PreMessageReceivedEventListener() {
            @Override
            public Message onPreMessageReceived(Peer peer, Message m) {
              if (m instanceof RejectMessage) {
                RejectMessage rejectMessage = (RejectMessage)m;
                if (newTx.getHash().equals(rejectMessage.getRejectedObjectHash())) {
                  log.info(format("peer (%s) rejected our transaction", peer));
                  testedPeers.put(peer, false);
                }
              }
              peer.removePreMessageReceivedEventListener(this);
              return m;
            }
          });
          log.info(format("sending tx to peer (%s)", peer));

          peer.sendMessage(newTx);
        }
      } catch (InterruptedException e) {
        log.warn("failed to test transactions");
        e.printStackTrace();
      } catch (InsufficientMoneyException e) {
        log.warn("send + more = money");
        e.printStackTrace();
      }
    }
  }
}
