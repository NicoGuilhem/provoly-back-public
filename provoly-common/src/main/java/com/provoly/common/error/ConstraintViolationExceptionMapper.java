package com.provoly.common.error;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Inject
    BusinessExceptionMapper businessExceptionMapper;

    @Override
    public Response toResponse(ConstraintViolationException e) {
        var businessException = new BusinessException(ErrorCode.BAD_REQUEST,
                e.getConstraintViolations().stream()
                        .findFirst()
                        .map(ConstraintViolation::toString)
                        .orElse(e.getMessage()));

        return businessExceptionMapper.toResponse(businessException);
    }
}
