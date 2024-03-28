package com.provoly.virt.imports;

import jakarta.ws.rs.*;

import com.provoly.clients.ModelService;
import com.provoly.common.error.ProvolyResponseExceptionMapper;

import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/*
* Add new model service that use OidcClientRequestReactiveFilter instead of  ProvolyAuthentRequestFilter
* to avoid generating a request context when there isn't one,
* and so avoid getting a 401 error from the AccessTokenRequestReactiveFilter.
*
* In fact, the call to the ProvolyAuthentRequestFilter in the ImportRunner manages to resolve the access token
* as if it were in a request context (whereas the call to data-ref is made in a uni), and triggers an error
* in the AccessTokenRequestReactiveFilter because there is effectively no request context.
*
* We haven't found a way to tell the ProvolyAuthentRequestFilter that it's not in a request context at that time.
* Hence the direct use of the OidcClientRequestReactiveFilter to retrieve information from
*  the model that is useful for importing.
* */
@Path("/model")
@RegisterRestClient(configKey = "data-ref")
@RegisterProvider(ProvolyResponseExceptionMapper.class)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
interface OidcModelService extends ModelService {

}