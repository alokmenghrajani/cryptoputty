package shapedirection.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import shapedirection.views.PeersView;

@Path("/peers")
public class PeersResource {

  @GET
  @Produces(MediaType.TEXT_HTML)
  public PeersView handleGet() {
    return new PeersView();
  }

}
