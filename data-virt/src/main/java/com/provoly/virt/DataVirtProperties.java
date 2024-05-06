package com.provoly.virt;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "provoly.virt", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface DataVirtProperties {

    @WithDefault("files")
    String filesBucketName();

    @WithDefault("icons")
    String iconsBucketName();

    @WithDefault("false")
    Boolean elasticEnableImmediateRefreshPolicy();

    @WithDefault("1000")
    int chunkSize();

    @WithDefault("1000")
    int importChunkSize();

    @WithDefault("10000")
    int maxImportChunkSize();

    @WithDefault("1000")
    int importMaxErrors();

    @WithDefault("1000")
    int searchLimit();

    @WithDefault("1000")
    int maxSizeLimit();

    String user();

    String password();

    @WithDefault("relation")
    String relationIndexName();

    @WithDefault("false")
    Boolean notification();

    Kuzzle kuzzle();

    Optional<ElasticClientConfig> elasticsearch();

    interface ElasticClientConfig {

        String host();

        OptionalInt port();

        String username();

        String password();

        String protocol();

        OptionalInt requestTimeout();

        @WithName("ca.file")
        Optional<Path> rootCertificate();
    }

    interface Kuzzle {
        Optional<String> host();

        Optional<String> tenant();

        @WithName("url")
        Optional<String> kuzzleUrl(); // used for testing because the kuzzle client does not have allt he functions needed to initialise the environment.

    }

}
