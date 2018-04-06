package shapedirection;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
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
import static shapedirection.TransactionTesterThread.PeerState.ACCEPTED_MINED;
import static shapedirection.TransactionTesterThread.PeerState.ACCEPTED_STUCK;
import static shapedirection.TransactionTesterThread.PeerState.REJECTED_MANDATORY;
import static shapedirection.TransactionTesterThread.PeerState.REJECTED_NON_MANDATORY;
import static shapedirection.TransactionTesterThread.PeerState.UNKNOWN;

public class TransactionTesterThread extends Thread {
  private static final Logger log = LoggerFactory.getLogger(TransactionTesterThread.class);

  enum PeerState {
    ACCEPTED_MINED,
    ACCEPTED_STUCK,
    REJECTED_NON_MANDATORY,
    REJECTED_MANDATORY,
    UNKNOWN
  }

  private ConcurrentMap<PeerAddress, PeerState> testedPeers = new ConcurrentHashMap<>();

  public void run() {
    // Continuously get peers and test whether they accept malformed transactions.
    //
    // We create a malformed transaction which also has an invalid signature. We then check which
    // peers report a malformed signature vs which ones report an invalid signature.

    while (true) {
      try {
        Thread.sleep(5000);

        // log some stats
        long accepted_mined = testedPeers.entrySet().stream().filter(e -> e.getValue() == ACCEPTED_MINED).count();
        long accepted_stuck = testedPeers.entrySet().stream().filter(e -> e.getValue() == ACCEPTED_STUCK).count();
        long rejected_non_mandatory = testedPeers.entrySet().stream().filter(e -> e.getValue() == REJECTED_NON_MANDATORY).count();
        long rejected_mandatory = testedPeers.entrySet().stream().filter(e -> e.getValue() == REJECTED_MANDATORY).count();
        long unknown = testedPeers.entrySet().stream().filter(e -> e.getValue() == UNKNOWN).count();

        log.info(format("stats: accepted_mined: %d (%f%%), accepted_stuck: %d (%f%%), rejected_non_mandatory: %d (%f%%), rejected_mandatory: %d (%f%%), unknown: %d",
            accepted_mined,
            Math.floor((double) accepted_mined / testedPeers.size() * 10000)/100,
            accepted_stuck,
            Math.floor((double) accepted_stuck / testedPeers.size() * 10000)/100,
            rejected_non_mandatory,
            Math.floor((double) rejected_non_mandatory / testedPeers.size() * 10000)/100,
            rejected_mandatory,
            Math.floor((double) rejected_mandatory / testedPeers.size() * 10000)/100,
            unknown));

        // iterate over our peers
        WalletAppKit kit = CryptoputtyApplication.kit;
        PeerGroup peerGroup = kit.peerGroup();
        List<Peer> connectedPeers = peerGroup.getConnectedPeers();
        for (Peer peer : connectedPeers) {
          if (testedPeers.containsKey(peer.getAddress())) {
            continue;
          }
          log.info(format("Testing %s", peer.toString()));

          // Create a normal transaction
          SendRequest request = SendRequest.to(kit.wallet().currentReceiveAddress(), Coin.MILLICOIN);
          kit.wallet().completeTx(request);
          Transaction newTx = TransactionMutator.mutate(request.tx);
          peer.addPreMessageReceivedEventListener(Threading.SAME_THREAD, new PreMessageReceivedEventListener() {
            @Override
            public Message onPreMessageReceived(Peer peer, Message m) {
              if (m instanceof RejectMessage) {
                RejectMessage rejectMessage = (RejectMessage)m;
                if (newTx.getHash().equals(rejectMessage.getRejectedObjectHash())) {
                  log.info(format("%s rejected our transaction (%s) with message %s - %s",
                      peer,
                      peer.getPeerVersionMessage().subVer,
                      rejectMessage.getReasonString(),
                      rejectMessage.getRejectedMessage()));
                  if (rejectMessage.getReasonString().startsWith("non-mandatory")) {
                    testedPeers.put(peer.getAddress(), REJECTED_NON_MANDATORY);
                  } else {
                    testedPeers.put(peer.getAddress(), REJECTED_MANDATORY);
                  }
                }
                peer.removePreMessageReceivedEventListener(this);
                // todo: removeBlocksDownloadedEventListener;
              }
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
                testedPeers.put(peer.getAddress(), ACCEPTED_MINED);
                kit.peerGroup().removeBlocksDownloadedEventListener(this);
                // todo: removePreMessageReceivedEventListener
              } else if (kit.wallet().getLastBlockSeenHeight() > currentHeight + 2) {
                // unlock the funds
                log.info(format("unlocking %s with %s", newTx.getHashAsString(), request.tx.getHashAsString()));
                peerGroup.broadcastTransaction(request.tx);
                testedPeers.put(peer.getAddress(), ACCEPTED_STUCK);
                kit.peerGroup().removeBlocksDownloadedEventListener(this);
                // todo: removePreMessageReceivedEventListener
              }
            }
          });

          log.info(format("sending peer (%s) tx %s", peer, newTx.getHashAsString()));
          testedPeers.put(peer.getAddress(), UNKNOWN);
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
