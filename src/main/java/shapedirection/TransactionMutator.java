package shapedirection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import static org.bitcoinj.script.ScriptOpCodes.OP_0;
import static org.bitcoinj.script.ScriptOpCodes.OP_DROP;

public class TransactionMutator implements OnTransactionBroadcastListener {
  private static final Logger log = LoggerFactory.getLogger(TransactionMutator.class);

  public static Transaction mutate(Transaction tx) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      // version
      uint32ToByteStreamLE(tx.getVersion(), out);

      // inputs
      out.write(tx.getInputs().size());
      for (TransactionInput input : tx.getInputs()) {
        out.write(input.getOutpoint().unsafeBitcoinSerialize());
        byte[] script = input.getScriptBytes();
        if (script.length == 0) {
          log.info("got tx with empty script.");
          return null;
        }
        if (script[0] == OP_0) {
          log.info("tx already mutated, returning.");
          return null;
        }
        byte[] newScript = new byte[script.length + 2];
        newScript[0] = OP_0;
        newScript[1] = OP_DROP;
        for (int i = 0; i < script.length; i++) {
          newScript[i + 2] = script[i];
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
      return newTx;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override public void onTransaction(Peer peer, Transaction tx) {
    log.info(format("Recv: %s (%s)", Hex.toHexString(tx.unsafeBitcoinSerialize()), tx.getHashAsString()));

    try {
      Transaction newTx = mutate(tx);
      if (newTx == null) {
        return;
      }
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
