package com.provoly.test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class KeycloakServiceHeader implements ClientHeadersFactory {
    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        String bearer = Base64.getEncoder().encodeToString("provoly:secret".getBytes(StandardCharsets.US_ASCII)); // TODO : Placer le client en paramètre
        result.add("Authorization", "Basic " + bearer);
        return result;
    }
}
