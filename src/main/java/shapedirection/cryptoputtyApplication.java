package shapedirection;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class cryptoputtyApplication extends Application<cryptoputtyConfiguration> {
  public static void main(final String[] args) throws Exception {
    new cryptoputtyApplication().run(args);
  }

  @Override
  public String getName() {
    return "cryptoputty";
  }

  @Override
  public void initialize(final Bootstrap<cryptoputtyConfiguration> bootstrap) {
    // TODO: application initialization
  }

  @Override
  public void run(final cryptoputtyConfiguration configuration, final Environment environment) {
    // TODO: implement application
  }
}
