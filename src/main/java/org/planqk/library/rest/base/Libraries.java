package org.planqk.library.rest.base;

import java.io.IOException;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.planqk.library.core.properties.ServerPropertyService;
import org.planqk.library.core.repository.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries")
@Tag(name = "Libraries")
public class Libraries {
    private final LibraryService libraryService;

    public Libraries() {
        libraryService = LibraryService.getInstance(ServerPropertyService.getInstance().getWorkingDirectory());
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getLibraryNames() {
        try {
            return Response.ok(libraryService.getLibraryNames().toString())
                           .build();
        } catch (IOException e) {
            return Response.serverError()
                           .entity(e.getMessage())
                           .build();
        }
    }

    @POST
    @Consumes({MediaType.TEXT_PLAIN})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewLibrary(String libraryName) {
        if (libraryService.libraryExists(libraryName)) {
            return Response.status(Response.Status.CONFLICT).entity("The given library name is already in use.").build();
        }
        try {
            libraryService.createLibrary(libraryName);
        } catch (IOException e) {
            return Response.serverError()
                           .entity(e.getMessage())
                           .build();
        }
        return Response.ok("Library with name " + libraryName + " created.")
                       .build();
    }

    @Path("{libraryName}")
    public Library getLibraryResource(@PathParam("libraryName") String libraryName) {
        return new Library(libraryName);
    }
}
