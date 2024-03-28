package org.elasticsearch.client;

import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;

// Exist for overiding package default visibility for Response
public class ResponseBuilder {

    public static Response build() {
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        return new Response(
                new BasicRequestLine("", "", protocolVersion),
                new HttpHost("acme.com"),
                new BasicHttpResponse(new BasicStatusLine(protocolVersion, 413, "Entity")));
    }
}
