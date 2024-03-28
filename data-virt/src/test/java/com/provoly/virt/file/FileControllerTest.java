package com.provoly.virt.file;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

import io.minio.StatObjectResponse;
import io.minio.Time;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowingConsumer;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.jaxrs.HttpHeadersImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.Headers;

class FileControllerTest {

    FileController fileController;
    FileService fileService;

    @BeforeEach
    public void beforeEach() {
        fileService = mock(FileService.class);
        given(fileService.getFile(any(), any(), any())).willReturn(new FileInformation("id", new StatObjectResponse(
                new Headers.Builder()
                        .add("Last-Modified", LocalDateTime.now().format(Time.HTTP_HEADER_DATE_FORMAT))
                        .add("Content-Length", "80").build(),
                "tutu",
                "toto",
                "tata"), null, 0, 80));
        fileController = new FileController(fileService);
    }

    @Test
    void file_should_accept_no_range() {
        //given
        var headers = new HttpHeadersImpl(Map.of("NoRange", "tutu").entrySet());
        //when
        fileController.getFile("tutu", headers);
        //then
        then(fileService).should().getFile(eq("tutu"), eq(null), eq(null));
    }

    @Test
    void file_should_return_partial_content() {
        //given
        var headers = new HttpHeadersImpl(Map.of("Range", "bytes=1-2").entrySet());
        given(fileService.getFile(any(), any(), any())).willReturn(new FileInformation("id", new StatObjectResponse(
                new Headers.Builder()
                        .add("Last-Modified", LocalDateTime.now().format(Time.HTTP_HEADER_DATE_FORMAT))
                        .add("Content-Length", "80").build(),
                "tutu",
                "toto",
                "tata"), null, 0, 2));
        //when
        RestResponse<InputStream> response = fileController.getFile("tutu", headers);
        //then
        assertThat(response.getStatus()).isEqualTo(RestResponse.StatusCode.PARTIAL_CONTENT);

    }

    @Test
    void file_should_return_full_content() {
        //given
        var headers = new HttpHeadersImpl(Map.of("Range", "bytes=1-2").entrySet());
        given(fileService.getFile(any(), any(), any())).willReturn(new FileInformation("id",
                new StatObjectResponse(new Headers.Builder()
                        .add("Last-Modified", LocalDateTime.now().format(Time.HTTP_HEADER_DATE_FORMAT)).build(), "tutu", "toto",
                        "tata"),
                null, 0, 1));
        //when
        RestResponse<InputStream> response = fileController.getFile("tutu", headers);
        //then
        assertThat(response.getStatus()).isEqualTo(RestResponse.StatusCode.OK);
    }

    @Test
    void file_should_throw_conflict_when_range_is_illegal() {
        ThrowingConsumer<Throwable> isCode403 = th -> ((BusinessException) th).getCode().equals(ErrorCode.BAD_REQUEST);
        //given
        var headers = new HttpHeadersImpl(Map.of("Range", "bytes=2-1").entrySet());
        //when, then
        assertThatThrownBy(() -> fileController.getFile("tutu", headers))
                .isInstanceOf(BusinessException.class)
                .satisfies(isCode403)
                .extracting(Throwable::getMessage, as(InstanceOfAssertFactories.STRING))
                .containsIgnoringCase("start")
                .containsIgnoringCase("end")
                .containsIgnoringCase("range")
                .containsIgnoringCase("before");
    }
}
