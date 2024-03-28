package com.provoly.exec.provision;

import static com.provoly.exec.provision.ProvolyExecEngineMock.TEST_FILE_CONTENT;
import static com.provoly.exec.provision.ProvolyExecEngineMock.TEST_FILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AppProvisionTest {

    @Inject
    AppProvision appProvision;

    @ConfigProperty(name = "provoly.exec.file-base-path")
    Path fileBasePath;

    @Test
    public void checkFileCreated() throws IOException {
        appProvision.run();
        Path filePath = fileBasePath.resolve(TEST_FILE_NAME);
        assertThat(filePath).hasContent(TEST_FILE_CONTENT);
        Files.delete(filePath);
    }

}
