package shapedirection;

import com.bendb.dropwizard.jooq.JooqFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.hibernate.validator.constraints.NotEmpty;

import static java.lang.String.format;

public class CryptoputtyConfiguration extends Configuration {
  @NotEmpty
  @JsonProperty
  private String testWallet;

  @JsonProperty
  private Network network;

  @JsonProperty
  public int maxPeersToDiscover;

  @JsonProperty
  public int maxConnections;

  public String getTestWallet() {
    return testWallet;
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
  // TODO: implement service configuration
  @Valid
  @NotNull
  @JsonProperty("database")
  private DataSourceFactory database = new DataSourceFactory();

  @Valid
  @NotNull
  @JsonProperty("jooq")
  private JooqFactory jooq = new JooqFactory();

  public DataSourceFactory getDataSourceFactory() {
    return database;
  }

  public JooqFactory getJooqFactory() {
    return jooq;
  }
}
