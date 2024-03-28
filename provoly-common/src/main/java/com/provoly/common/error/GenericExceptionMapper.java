package com.provoly.common.error;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Provider
@ApplicationScoped
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Inject
    Logger log;

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @Override
    public Response toResponse(Exception e) {
        log.error("Error :", e);

        if (e instanceof jakarta.ws.rs.NotFoundException) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto(applicationName, ErrorCode.NOT_FOUND, e.getMessage())).build();
        }
        if (e.getCause() != null && e.getCause().getCause() instanceof BusinessException be) {
            return Response
                    .status(be.getStatus())
                    .entity(new ErrorDto(applicationName, be)).build();
        }
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorDto(applicationName, ErrorCode.TECHNICAL, buildMessage(e))).build();
    }

    private String buildMessage(Exception e) {
        var msg = "Unhandled exception thrown : " + e.getClass().getName();
        if (e.getMessage() != null) {
            msg += " Message: " + e.getMessage() + ".";
        }
        msg += " See logs for complete stack";
        return msg;
    }
}
