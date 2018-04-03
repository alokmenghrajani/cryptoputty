package shapedirection.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import shapedirection.views.IndexView;

@Path("/")
public class IndexResource {

  @GET
  @Produces(MediaType.TEXT_HTML)
  public IndexView handleGet() {
    return new IndexView();
  }

}
