package com.provoly.virt.file;

import static com.provoly.virt.file.FileService.MAX_SIZE;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.user.Role;
import com.provoly.virt.entity.FileType;

import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/file")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FileController {

    FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ Role.STR_ITEM_WRITE })
    public String receiveFile(
            @RestForm("file") FileUpload fileUpload,
            @RestForm("filename") String fileName) {
        if (fileUpload == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File is required.");
        }
        try (InputStream is = new FileInputStream(fileUpload.uploadedFile().toFile())) {
            return "\"%s\"".formatted(fileService.receive(is, fileName, FileType.valueOf(fileUpload.contentType())));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @GET
    @Path("/id/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DASHBOARD_READ, Role.STR_DATASET_READ, Role.STR_WIDGET_CATALOG_READ })
    public RestResponse<InputStream> getFile(
            String id,
            @Context HttpHeaders headers) {
        String range = String.join("", headers.getRequestHeader("Range"));
        String[] tokens = range.split("bytes=");
        Long start = null, end = null;
        if (tokens.length > 1) {
            String[] rangeToken = tokens[1].split("-");
            if (!rangeToken[0].isEmpty()) {
                start = Long.parseLong(rangeToken[0]);
            } else {
                start = 0L;
            }
            if (rangeToken.length > 1) {
                end = Long.parseLong(rangeToken[1]);
            } else {
                end = -1L;
            }
            if ((end - start) < 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "End range can't be before start range.");
            }
            if ((end - start) > MAX_SIZE || end == -1) {
                end = start + MAX_SIZE - 1;
            }
        }
        return getResponseBuilder(fileService.getFile(id, start, end));
    }

    @POST
    @Path("/icons")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ Role.STR_CLASS_WRITE, Role.STR_DASHBOARD_WRITE, Role.STR_DATASET_WRITE,
            Role.STR_WIDGET_CATALOG_WRITE })
    @ResponseStatus(RestResponse.StatusCode.CREATED)
    public String receiveIcon(
            @RestForm("file") FileUpload fileUpload,
            @RestForm String type) {
        try (InputStream is = new FileInputStream(fileUpload.uploadedFile().toFile())) {
            return "\"%s\"".formatted(
                    fileService.receiveIcon(is, fileUpload.fileName(), FileType.valueOf(fileUpload.contentType()), type));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @GET
    @Path("/icons")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH, Role.STR_DASHBOARD_READ, Role.STR_DATASET_READ,
            Role.STR_WIDGET_CATALOG_READ })
    public List<ImageInfoDto> getIconList(@RestQuery("type") String type) {
        return fileService.getAllIcons(type);
    }

    @GET
    @Path("/icons/{name}")
    @RolesAllowed({ Role.STR_CLASS_READ, Role.STR_SEARCH, Role.STR_DASHBOARD_READ, Role.STR_DATASET_READ,
            Role.STR_WIDGET_CATALOG_READ })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public RestResponse<InputStream> getIcon(
            String name) {
        return getResponseBuilder(fileService.getIcon(name));
    }

    @PUT
    @Path("/icons/{id}/{type}")
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_CLASS_WRITE, Role.STR_DASHBOARD_WRITE, Role.STR_DATASET_WRITE,
            Role.STR_WIDGET_CATALOG_WRITE })
    public void setIconType(String id,
            String type) {
        fileService.setIconTags(id, type);
    }

    @DELETE
    @Path("/icons/{id}")
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_CLASS_WRITE, Role.STR_DASHBOARD_WRITE, Role.STR_DATASET_WRITE,
            Role.STR_WIDGET_CATALOG_WRITE })
    public void deleteIcon(String id) {
        fileService.deleteIcon(id);
    }

    private RestResponse<InputStream> getResponseBuilder(FileInformation fileInformation) {
        RestResponse.ResponseBuilder<InputStream> builder;
        if (fileInformation.end() + 1 < fileInformation.objectStats().size()) {
            builder = RestResponse.ResponseBuilder.create(Response.Status.PARTIAL_CONTENT);
            builder.header("Content-Range", "bytes " + fileInformation.start() + "-" + fileInformation.end() + "/"
                    + fileInformation.objectStats().size());
        } else {
            builder = RestResponse.ResponseBuilder.create(Response.Status.OK);
        }

        return builder
                .header("Content-Disposition", "attachment;filename=" + fileInformation.id())
                .header("Content-Type", fileInformation.objectStats().contentType())
                .header("Accept-Ranges", "bytes")
                .entity(fileInformation.is())
                .build();
    }
}
