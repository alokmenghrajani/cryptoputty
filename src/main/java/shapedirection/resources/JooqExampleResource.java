package shapedirection.resources;

import java.util.Arrays;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static shapedirection.jooq.tables.Peers.PEERS;

@Path("/jooq-example")
public class JooqExampleResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(JooqExampleResource.class);

  public JooqExampleResource() {
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String get(@Context DSLContext database) {
    return String.join("\n", Arrays.stream(database
        .selectFrom(PEERS)
        .fetchArray())
        .map(r -> format("%s:%d", r.getAddress(), r.getPort().intValue()))
        .toArray(String[]::new));
  }
}
