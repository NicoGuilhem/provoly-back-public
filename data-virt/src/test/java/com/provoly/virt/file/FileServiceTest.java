package com.provoly.virt.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.test.AuthService;
import com.provoly.test.ProvolyTestContainers;
import com.provoly.virt.entity.FileType;

import io.minio.errors.ErrorResponseException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(ProvolyTestContainers.class)
public class FileServiceTest {
    @Inject
    AuthService authService;

    @Inject
    FileService fileService;

    private FileUpload fileUpload;

    private InputStream fileUploadIs;

    private String type;
    static final private UUID fileName = UUID.randomUUID();

    private final String filePath = "src/test/resources/dwight.jpg";

    @BeforeEach
    public void prepareData() throws FileNotFoundException {
        authService.authenticate();
        type = "restit";
        fileUpload = new MockFileUpload(fileName.toString(), filePath, FileType.JPEG_TYPE);
        fileUploadIs = new FileInputStream(fileUpload.uploadedFile().toFile());

    }

    @AfterEach
    public void clean() throws IOException {
        if (fileUploadIs != null) {
            fileUploadIs.close();
        }
        for (ImageInfoDto icon : fileService.getAllIcons(null)) {
            fileService.deleteIcon(icon.image());
        }
    }

    @Test
    public void add_get_icons_empty() {
        assertThat(fileService.getAllIcons(null)).hasSize(0);
    }

    @Test
    public void add_icon_sha1() {
        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);

        assertThat(fileService.getAllIcons(null)).hasSize(1);
        System.out.println(fileService.getAllIcons(null));
        assertThat(fileService.getIcon(fileUpload.fileName())).isNotNull();
    }

    @Test
    public void add_icon_already_exists() {
        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);
        Assertions.assertThrows(BusinessException.class,
                () -> {
                    fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()),
                            type);
                });
    }

    @Test
    public void add_icon_wrong_mime_type() {
        fileUpload = new MockFileUpload("filename", filePath, FileType.CSV_TYPE);
        assertThatThrownBy(() -> {
            fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);
        })
                .isInstanceOf(BusinessException.class)
                .hasMessage("File type : text/csv is not supported for icon imports.");
    }

    @Test
    public void add_icon_already_imported_with_different_tag() {
        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);

        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()),
                "illustration");
        assertThat(fileService.getAllIcons(null)).hasSize(1);
        assertThat(fileService.getAllIcons(null).stream().findFirst().map(iconParams -> iconParams.type()).get())
                .isEqualTo(List.of(type, "illustration"));
    }

    @Test
    public void add_icon_missing_type() {
        Assertions.assertThrows(BusinessException.class,
                () -> fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()),
                        null));
    }

    @Test
    public void get_icon_not_found() {
        Assertions.assertThrows(BusinessException.class,
                () -> fileService.getIcon(fileUpload.fileName()));
    }

    @Test
    public void get_all_icons_filtered() throws IOException {
        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);
        type = "presentation";
        MockFileUpload secondFile = new MockFileUpload("filename2", filePath, FileType.JPEG_TYPE);
        try (InputStream fileUpload2Is = new FileInputStream(secondFile.uploadedFile().toFile())) {
            fileService.receiveIcon(fileUpload2Is, secondFile.fileName(), secondFile.mediaType(), type);

            assertThat(fileService.getAllIcons("toto")).hasSize(0);
            assertThat(fileService.getAllIcons("presentation")).hasSize(1);
            assertThat(fileService.getAllIcons(null)).hasSize(2);
        }
    }

    @Test
    public void set_tag_icon() {
        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);
        fileService.setTags(fileUpload.fileName(), "type2");
        ImageInfoDto info = fileService.getAllIcons(null).get(0);
        assertThat(info.type()).containsExactlyInAnyOrder("type2", "restit");
    }

    @Test
    public void set_tag_icon_tag_already_exists() {
        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);
        Assertions.assertThrows(BusinessException.class,
                () -> fileService.setTags("08deae8d9ea9bc0b84f94475d868351830e9f7e7", "restit"));
    }

    @Test
    public void set_tag_icon_not_found() {
        Assertions.assertThrows(BusinessException.class,
                () -> fileService.setTags("toto", "type2"));
    }

    @Test
    public void delete_icon_no_exists() {
        Assertions.assertThrows(BusinessException.class,
                () -> fileService.deleteIcon("toto"));
    }

    @Test
    public void delete_icon() {
        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);
        fileService.deleteIcon(fileUpload.fileName());
        assertThat(fileService.getAllIcons(null)).hasSize(0);
    }

    @Test
    public void delete_file_should_success() throws ErrorResponseException {
        fileService.receiveIcon(fileUploadIs, fileUpload.fileName(), MediaType.valueOf(fileUpload.contentType()), type);

        fileService.deleteRawFile(new DatasetVersionDto(fileName));

        assertThatThrownBy(() -> fileService.getFile(fileName.toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("File provoly/%s not found".formatted(fileName.toString()));
    }

}
