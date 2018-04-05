package shapedirection;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.hibernate.validator.constraints.NotEmpty;

import static java.lang.String.format;

public class CryptoputtyConfiguration extends Configuration {
  @NotEmpty
  @JsonProperty
  private String wallet;

  @JsonProperty
  private Network network;

  @JsonProperty
  public int maxPeersToDiscover;

  @JsonProperty
  public int maxConnections;

  public String getWallet() {
    return wallet;
  }

  public NetworkParameters getNetwork() {
    switch (network) {
      case TESTNET:
        return TestNet3Params.get();
      case MAINNET:
        return MainNetParams.get();
      default:
        throw new IllegalStateException(format("unknown network: %s", network));
    }
  }

  enum Network {
    TESTNET,
    MAINNET
  }
}
