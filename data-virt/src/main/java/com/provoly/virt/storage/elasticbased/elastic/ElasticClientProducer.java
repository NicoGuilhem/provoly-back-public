package com.provoly.virt.storage.elasticbased.elastic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import com.provoly.virt.DataVirtProperties;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.jboss.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.instrumentation.Instrumentation;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@ApplicationScoped
public class ElasticClientProducer {

    private Optional<DataVirtProperties.ElasticClientConfig> config;

    private Logger log;

    /*
     * We need to use the same Elastic RestClient for healthcheck and for the rest of the application,
     * but it's not possible to set the @ApplicationScoped of ElasticsearchClient or RestClient
     * because they do not have default constructor.
     */
    private volatile RestClient restClient;

    public ElasticClientProducer(DataVirtProperties config,
            Logger log) {
        this.config = config.elasticsearch();
        this.log = log;
    }

    @Produces
    public ElasticsearchClient elasticsearchClient(Instance<Instrumentation> maybeInstrumentation, RestClient restClient) {
        return config.map(clientConfig -> {
            log.infof("Creating a new elasticsearch client to %s", clientConfig.host());
            if (maybeInstrumentation.isResolvable()) {
                return new ElasticsearchClient(
                        new RestClientTransport(restClient, new JacksonJsonpMapper(), null,
                                maybeInstrumentation.get()));
            }
            return new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
        }).orElse(null);
    }

    @Produces
    public RestClient restClient() {
        return config.map(clientConfig -> {
            if (restClient == null) {
                restClient = RestClient
                        .builder(new HttpHost(clientConfig.host(), clientConfig.port().orElse(9200), clientConfig.protocol()))
                        .setHttpClientConfigCallback(
                                httpClientBuilder -> this.getHttpAsyncClientBuilder(httpClientBuilder, clientConfig))
                        .setCompressionEnabled(true)
                        .setRequestConfigCallback(
                                requestConfigBuilder -> requestConfigBuilder
                                        .setSocketTimeout(clientConfig.requestTimeout().orElse(6000)))
                        .build();
            }
            return restClient;
        }).orElse(null);
    }

    private HttpAsyncClientBuilder getHttpAsyncClientBuilder(HttpAsyncClientBuilder httpClientBuilder,
            DataVirtProperties.ElasticClientConfig clientConfig) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(clientConfig.username(),
                clientConfig.password());
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);
        clientConfig.rootCertificate().ifPresent(path -> httpClientBuilder.setSSLContext(buildSSlContext(path)));
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        return httpClientBuilder;
    }

    private SSLContext buildSSlContext(Path elasticRootCertificate) {
        try {
            List<X509Certificate> certs = readCertificateChain(elasticRootCertificate);

            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null);
            trustStore.setCertificateEntry("elastic", certs.get(0));

            SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(trustStore, null);
            return sslBuilder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to build SSLContext", e);
        }
    }

    private List<X509Certificate> readCertificateChain(Path certificatePem) throws IOException, CertificateException {
        List<X509Certificate> result = new ArrayList<>();

        try (var r = Files.newBufferedReader(certificatePem)) {
            String s = r.readLine();
            if (s == null || !s.contains("BEGIN CERTIFICATE")) {
                throw new IllegalArgumentException("No CERTIFICATE found");
            }
            StringBuilder b = new StringBuilder();
            while (s != null) {
                if (s.contains("END CERTIFICATE")) {
                    String hexString = b.toString();
                    final byte[] bytes = Base64.getDecoder().decode(hexString);
                    X509Certificate cert = generateCertificateFromDER(bytes);
                    result.add(cert);
                    b = new StringBuilder();
                } else {
                    if (!s.startsWith("----")) {
                        b.append(s);
                    }
                }
                s = r.readLine();
            }
        }

        return result;
    }

    private static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

}
