package org.planqk.library.rest.base;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.planqk.library.core.properties.ServerPropertyService;
import org.planqk.library.core.repository.LibraryService;
import org.planqk.library.core.serialization.BibEntryAdapter;
import org.planqk.library.rest.model.BibEntryDTO;
import org.planqk.library.rest.model.BibEntries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Library {
    private final LibraryService libraryService;
    private final String libraryName;

    private static final Logger LOGGER = LoggerFactory.getLogger(Library.class);

    public Library(String libraryName) {
        this.libraryName = libraryName;
        libraryService = LibraryService.getInstance(ServerPropertyService.getInstance().getWorkingDirectory());
    }

    public Library(java.nio.file.Path directory, String libraryName) {
        this.libraryName = libraryName;
        libraryService = LibraryService.getInstance(directory);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLibraryEntries() {
        try {
            List<BibEntry> entries = libraryService.getLibraryEntries(libraryName);
            return Response.ok(new BibEntries(entries))
                           .build();
        } catch (IOException e) {
            LOGGER.error("Error retrieving entries.", e);
            return Response.serverError()
                           .entity(e.getMessage())
                           .build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createEntryInLibrary(BibEntryDTO bibEntry) {
        Gson gson = new GsonBuilder().registerTypeAdapter(BibEntry.class, new BibEntryAdapter()).create();
        try {
            libraryService.addEntryToLibrary(libraryName, bibEntry.entry);
        } catch (IOException e) {
            LOGGER.error("Error creating entry.", e);
            return Response.serverError()
                           .entity(e.getMessage())
                           .build();
        }
        return Response.ok()
                       .build();
    }

    @DELETE
    public Response deleteLibrary() {
        try {
            if (libraryService.deleteLibrary(libraryName)) {
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.GONE)
                               .build();
            }
        } catch (IOException e) {
            LOGGER.error("Error deleting library.", e);
            return Response.serverError()
                           .entity(e.getMessage())
                           .build();
        }
    }

    @GET
    @Path("{citeKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBibEntryMatchingCiteKey(@PathParam("citeKey") String citeKey) {
        try {
            Optional<BibEntry> entry = libraryService.getLibraryEntryMatchingCiteKey(libraryName, citeKey);
            if (entry.isPresent()) {
                return Response.ok(new BibEntryDTO(entry.get()))
                               .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (IOException e) {
            LOGGER.error("Error finding matching entry.", e);
            return Response.serverError()
                           .entity(e.getMessage())
                           .build();
        }
    }

    // TODO:
    //  The issue with this is still the fact that the cite key does not have to be unique, leading to unexpected behaviour.
    //  This might be fixable using the ID field (if it is truly unique?), but this would require the ID field to be de-/marshalled.
    //  But in the given implementation this was explicitly not done.
    //  The reason for this is currently unclear? Maybe because these IDs are just volatile between executions, but would that result in a problem?
    @PUT
    @Path("{citeKey}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateEntry(@PathParam("citeKey") String citeKey, BibEntryDTO bibEntry) {
        try {
            Gson gson = new GsonBuilder().registerTypeAdapter(BibEntry.class, new BibEntryAdapter()).create();
            BibEntry updatedEntry = bibEntry.entry;
            libraryService.updateEntry(libraryName, citeKey, updatedEntry);
            return Response.ok()
                           .build();
        } catch (IOException e) {
            LOGGER.error("Error updating entry.", e);
            return Response.serverError()
                           .entity(e.getMessage())
                           .build();
        }
    }

    @DELETE
    @Path("{citeKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEntryFromLibrary(@PathParam("citeKey") String citeKey) {
        try {
            boolean deleted = libraryService.deleteEntryByCiteKey(libraryName, citeKey);
            if (deleted) {
                return Response.ok("Entry deleted.")
                               .build();
            } else {
                return Response.ok("Either the database does not exist, or there was no entry with the specified citation key in it.")
                               .build();
            }
        } catch (IOException e) {
            LOGGER.error("Error deleting entry.", e);
            return Response.serverError()
                           .entity(e.getMessage())
                           .build();
        }
    }
}
