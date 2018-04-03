package shapedirection;

import com.google.common.io.Files;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import java.io.File;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shapedirection.health.PeerHealth;
import shapedirection.resources.IndexResource;
import shapedirection.resources.PeersResource;

import static java.lang.String.format;

public class CryptoputtyApplication extends Application<CryptoputtyConfiguration> {
  private static final Logger log = LoggerFactory.getLogger(CryptoputtyApplication.class);
  public static CryptoputtyConfiguration config;
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
    bootstrap.addBundle(new ViewBundle<>());
  }

  @Override
  public void run(final CryptoputtyConfiguration configuration, final Environment environment) {
    config = configuration;

    environment.healthChecks().register("peer", new PeerHealth());
    environment.jersey().register(new IndexResource());
    environment.jersey().register(new PeersResource());

    // Setup WalletAppKit
    File tempDir = Files.createTempDir();
    tempDir.deleteOnExit();
    kit = new WalletAppKit(configuration.getNetwork(), tempDir, "cryptoputty");
    try {
      kit.restoreWalletFromSeed(new DeterministicSeed(configuration.getTestWallet(), null, "", 1522774271L));

      kit.setBlockingStartup(false);
      kit.setAutoSave(true);
      kit.startAsync();
      kit.awaitRunning();

      // Perhaps having bloom filters doesn't hurt since we don't plan to do anything
      // kit.peerGroup().setBloomFilteringEnabled(false);

      // I have no idea what I'm doing.
      kit.peerGroup().setMaxPeersToDiscoverCount(configuration.maxPeersToDiscover);
      kit.peerGroup().setMaxConnections(configuration.maxPeersToDiscover);

      kit.peerGroup().addOnTransactionBroadcastListener(new TransactionMutator());

      log.info(format("send money to: %s", kit.wallet().freshReceiveAddress().toString()));
      log.info("done initializing");
    } catch (UnreadableWalletException e) {
      e.printStackTrace();
    }

    // Start crawling
    new CrawlerThread().start();
  }
}
