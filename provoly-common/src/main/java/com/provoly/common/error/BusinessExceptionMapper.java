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
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    @Inject
    Logger log;

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @Override
    public Response toResponse(BusinessException e) {
        log.error("Error :", e);
        return Response
                .status(e.getStatus())
                .entity(new ErrorDto(applicationName, e)).build();
    }
}
