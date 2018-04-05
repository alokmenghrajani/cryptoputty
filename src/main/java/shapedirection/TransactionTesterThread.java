package shapedirection;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.RejectMessage;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener;
import org.bitcoinj.core.listeners.PreMessageReceivedEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.SendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static shapedirection.TransactionTesterThread.PeerState.REJECTS;
import static shapedirection.TransactionTesterThread.PeerState.STRONG;
import static shapedirection.TransactionTesterThread.PeerState.UNKNOWN;
import static shapedirection.TransactionTesterThread.PeerState.WEAK;

public class TransactionTesterThread extends Thread {
  private static final Logger log = LoggerFactory.getLogger(TransactionTesterThread.class);

  enum PeerState {
    REJECTS,
    STRONG,
    WEAK,
    UNKNOWN
  }

  private ConcurrentMap<Peer, PeerState> testedPeers = new ConcurrentHashMap<>();

  public void run() {
    // Continuously get peers and test whether they accept malformed transactions.
    //
    // We create a malformed transaction which also has an invalid signature. We then check which
    // peers report a malformed signature vs which ones report an invalid signature.

    while (true) {
      try {
        Thread.sleep(5000);

        // log some stats
        Set<String> strong = testedPeers.entrySet().stream().filter(e -> e.getValue() == STRONG).map(e -> e.getKey().getPeerVersionMessage().subVer).collect(toSet());
        Set<String> weak = testedPeers.entrySet().stream().filter(e -> e.getValue() == WEAK).map(e -> e.getKey().getPeerVersionMessage().subVer).collect(toSet());
        Set<String> unknown = testedPeers.entrySet().stream().filter(e -> e.getValue() == UNKNOWN).map(e -> e.getKey().getPeerVersionMessage().subVer).collect(toSet());

        log.info(format("stats. Strong: %d (%f%%), weak: %d (%f%%), unknown: %d",
            strong.size(),
            Math.floor((double) strong.size() / testedPeers.size() * 10000)/100,
            weak.size(),
            Math.floor((double) weak.size() / testedPeers.size() * 10000)/100,
            unknown.size()));

        // iterate over our peers
        WalletAppKit kit = CryptoputtyApplication.kit;
        PeerGroup peerGroup = kit.peerGroup();
        List<Peer> connectedPeers = peerGroup.getConnectedPeers();
        for (Peer peer : connectedPeers) {
          if (testedPeers.containsKey(peer)) {
            continue;
          }
          log.info(format("Testing %s", peer.toString()));

          // Create a normal transaction
          SendRequest request = SendRequest.to(kit.wallet().currentReceiveAddress(), Coin.COIN.div(5));
          kit.wallet().completeTx(request);
          Transaction newTx = TransactionMutator.mutate(request.tx, true);
          peer.addPreMessageReceivedEventListener(Threading.SAME_THREAD, new PreMessageReceivedEventListener() {
            @Override
            public Message onPreMessageReceived(Peer peer, Message m) {
              if (m instanceof RejectMessage) {
                RejectMessage rejectMessage = (RejectMessage)m;
                if (newTx.getHash().equals(rejectMessage.getRejectedObjectHash())) {
                  log.info(format("%s rejected our transaction (%s)", peer, peer.getPeerVersionMessage().subVer));
                  testedPeers.put(peer, REJECTS);
                }
              }
              peer.removePreMessageReceivedEventListener(this);
              return m;
            }
          });
          int currentHeight = kit.wallet().getLastBlockSeenHeight();
          kit.peerGroup().addBlocksDownloadedEventListener(new BlocksDownloadedEventListener() {
            @Override
            public void onBlocksDownloaded(Peer peer, Block block,
                @Nullable FilteredBlock filteredBlock,
                int blocksLeft) {
              if (newTx.getConfidence().getConfidenceType() == TransactionConfidence.ConfidenceType.BUILDING) {
                testedPeers.put(peer, STRONG);
                kit.peerGroup().removeBlocksDownloadedEventListener(this);
              } else if (kit.wallet().getLastBlockSeenHeight() > currentHeight + 2) {
                // unlock the funds
                log.info(format("unlocking %s with %s", newTx.getHashAsString(), request.tx.getHashAsString()));
                peerGroup.broadcastTransaction(request.tx);
                testedPeers.put(peer, WEAK);
                kit.peerGroup().removeBlocksDownloadedEventListener(this);
              }
            }
          });

          log.info(format("sending peer (%s) tx %s", peer, newTx.getHashAsString()));
          testedPeers.put(peer, UNKNOWN);
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
