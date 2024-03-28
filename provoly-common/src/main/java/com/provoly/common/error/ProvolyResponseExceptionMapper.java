package com.provoly.common.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import io.quarkus.rest.client.reactive.runtime.ResteasyReactiveResponseExceptionMapper;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProvolyResponseExceptionMapper implements ResteasyReactiveResponseExceptionMapper<BusinessException> {

    @Inject
    ObjectMapper mapper;

    @Override
    public BusinessException toThrowable(Response response, RestClientRequestContext context) {

        String errorAsString;

        try {
            errorAsString = response.readEntity(String.class);
        } catch (ProcessingException e) {
            return buildUnknownBusinessException(response, context, "Processing response error is himself in error");
        }

        try {
            var errorDto = mapper.readValue(errorAsString, ErrorDto.class);
            if (errorDto.getCode() != null) {
                return new BusinessException(errorDto);
            } else {
                return buildUnknownBusinessException(response, context, errorAsString);
            }
        } catch (JsonProcessingException e) {
            return buildUnknownBusinessException(response, context, errorAsString);
        }
    }

    private BusinessException buildUnknownBusinessException(Response response, RestClientRequestContext context,
            String errorAsString) {
        var status = response.getStatus();

        switch (status) {
            case 401:
            case 403:
                return new BusinessException(ErrorCode.UNAUTHORIZED,
                        "We received an unauthorized %d response from remote server %s requesting %s".formatted(status,
                                errorAsString, context.getClientRequestContext().getUri()));
            default:
                String message = "Sorry we received a response which is not an ErrorDto. Uri = %s Status = %d Response = %s"
                        .formatted(context.getClientRequestContext().getUri(), status, errorAsString);
                return new BusinessException(ErrorCode.TECHNICAL, message);
        }

    }

}
