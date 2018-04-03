package shapedirection.views;

import io.dropwizard.views.View;
import java.util.List;
import org.bitcoinj.core.Address;
import shapedirection.CryptoputtyApplication;

public class IndexView extends View {

  private Address receiveAddress;
  private boolean ok;

  public IndexView() {
    super("index.mustache");
    receiveAddress = CryptoputtyApplication.kit.wallet().currentReceiveAddress();
    List<Address> watchedAddresses = CryptoputtyApplication.kit.wallet().getWatchedAddresses();
    ok = false;
    for (Address a : watchedAddresses) {
      if (a.equals(receiveAddress)) {
        ok = true;
      }
    }
  }

  public String getReceiveAddress() {
    return receiveAddress.toString();
  }

  public boolean getOk() {
    return ok;
  }
}
