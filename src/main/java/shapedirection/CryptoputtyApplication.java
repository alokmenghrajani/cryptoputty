package shapedirection;

import com.google.common.io.Files;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.File;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shapedirection.health.PeerHealth;

import static java.lang.String.format;

public class CryptoputtyApplication extends Application<CryptoputtyConfiguration> {
  private static final Logger log = LoggerFactory.getLogger(CryptoputtyApplication.class);
  public static WalletAppKit kit;

  public static void main(final String[] args) throws Exception {
    new CryptoputtyApplication().run(args);
  }

  @Override
  public String getName() {
    return "cryptoputty";
  }

  @Override
  public void initialize(final Bootstrap<CryptoputtyConfiguration> bootstrap) {
    NetworkParameters params = TestNet3Params.get();
    File tempDir = Files.createTempDir();
    tempDir.deleteOnExit();
    kit = new WalletAppKit(params, tempDir, "cryptoputty");
    try {
      kit.restoreWalletFromSeed(new DeterministicSeed(
          "office suit release flame robust know depth truly swim bird quality reopen", null, "",
          1522261414L));
      kit.setBlockingStartup(false);
      kit.setAutoSave(true);

      kit.startAsync();
      kit.awaitRunning();

      log.info(format("send money to: %s", kit.wallet().freshReceiveAddress().toString()));
      log.info("done initializing");
    } catch (UnreadableWalletException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run(final CryptoputtyConfiguration configuration, final Environment environment) {
    environment.healthChecks().register("peer", new PeerHealth());
  }
}
