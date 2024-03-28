package com.provoly.virt.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

import com.provoly.common.dataset.DatasetVersionDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.imports.ExtractMessageCode;
import com.provoly.common.imports.ExtractedMessage;
import com.provoly.common.imports.ImportsMessage;
import com.provoly.common.imports.MessageLevel;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.FileType;
import com.provoly.virt.entity.ItemId;
import com.provoly.virt.event.VirtEventEmitter;
import com.provoly.virt.imports.ImportAllowedTypes;
import com.provoly.virt.item.ReadItemsService;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;

import org.jboss.logging.Logger;

@ApplicationScoped
public class FileService {
    private static final String IMAGE_TYPE = "type";
    private static final String TAG_SEPARATOR = "-";
    private static final String ITEMID_SEPARATOR = "@";
    private static final List<MediaType> iconMimeTypes = FileType.getAuthorizedIconTypes();
    static final int MAX_SIZE = 1000000;
    private final Integer partSize = 52428800;
    private Logger log;
    private ReadItemsService getItemsService;
    private MinioClient minio;
    private DataVirtProperties dataVirtProperties;
    private VirtEventEmitter virtEventEmitter;

    public FileService(Logger log, ReadItemsService getItemsService, MinioClient minio, DataVirtProperties dataVirtProperties,
            VirtEventEmitter virtEventEmitter) {
        this.log = log;
        this.getItemsService = getItemsService;
        // If you want minio http tracing :
        // this.minio.traceOn(System.err);
        this.minio = minio;
        this.dataVirtProperties = dataVirtProperties;
        this.virtEventEmitter = virtEventEmitter;
    }

    public String receive(InputStream inputStream, MediaType mediaType) {
        String fileName = UUID.randomUUID().toString();
        receive(inputStream, fileName, mediaType, null, dataVirtProperties.filesBucketName());
        return fileName;
    }

    public void receive(InputStream inputStream, MediaType mediaType, UUID fileId) {
        receive(inputStream, fileId.toString(), mediaType, null, dataVirtProperties.filesBucketName());
    }

    public void associateFileToDataset(InputStream is, MediaType mediaType, DatasetVersionDto datasetVersionDto) {
        if (ImportAllowedTypes.findByType(mediaType).isEmpty()) {
            ExtractedMessage message = new ExtractedMessage(MessageLevel.ERROR, ExtractMessageCode.MEDIA_TYPE);
            virtEventEmitter.sendImportMessage(new ImportsMessage(datasetVersionDto.getId(), null, List.of(message)));

            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Media-type %s not supported".formatted(mediaType));
        }
        receive(is, datasetVersionDto.getId().toString(), mediaType, null, dataVirtProperties.filesBucketName());
    }

    public String receiveIcon(InputStream inputStream, String fileName, MediaType fileType, String type) {
        if (type == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Type is required.");
        }
        if (!iconMimeTypes.contains(fileType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "File type : %s is not supported for icon imports.".formatted(fileType));
        }
        return receive(inputStream, fileName, fileType, type, dataVirtProperties.iconsBucketName());
    }

    public void deleteRawFile(DatasetVersionDto dto) throws ErrorResponseException {
        String formattedPath = getFormattedPath(dto.getId().toString());
        StatObjectResponse statObjectResponse = null;
        try {
            statObjectResponse = minio.statObject(StatObjectArgs.builder()
                    .bucket(dataVirtProperties.filesBucketName())
                    .object(formattedPath).build());
            log.infof("Deleting raw file %s", formattedPath);
            if (statObjectResponse != null) {
                minio.removeObject(RemoveObjectArgs.builder()
                        .bucket(dataVirtProperties.filesBucketName())
                        .object(formattedPath).build());
            }
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey"))
                log.errorf(e, "Raw file not found %s", formattedPath);
            else
                throw e;
        } catch (ServerException | InsufficientDataException | IOException | NoSuchAlgorithmException | InvalidKeyException
                | InvalidResponseException | XmlParserException | InternalException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "An error as occured during delete raw file %s".formatted(dto.getId().toString()));
        }
    }

    private String getFormattedPath(String fileNameSha1) {
        return UriBuilder.fromPath(dataVirtProperties.filesBucketName()).path(fileNameSha1).build().toString();
    }

    private String receive(InputStream inputStream, String fileName, MediaType mediaType, String type, String bucket) {
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "InputStream is required.");
        }

        if (fileName == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File name is required.");
        }
        String uploadFileName = getFormattedPath(fileName, bucket);

        try {
            if (getStatObjectResponse(uploadFileName, bucket) != null) {
                if (getTags(uploadFileName).contains(type)) {
                    throw new BusinessException(ErrorCode.NAME_ALREADY_USED,
                            "File name %s is already used.".formatted(fileName));
                }
                setTags(fileName, type);
            } else {
                uploadFile(new MinIoFileUpload(inputStream, uploadFileName, mediaType.toString(), bucket, type));
            }
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
        return fileName;
    }

    public void setTags(String name, String type) {
        getIcon(name);
        name = getFormattedIconPath(name);
        try {
            var tags = new ArrayList<>(getTags(name));

            if (tags.contains(type)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Type %s already exists for file %s".formatted(type, name));
            }

            tags.add(type);
            String types = String.join(TAG_SEPARATOR, tags);

            minio.setObjectTags(
                    SetObjectTagsArgs.builder().bucket(dataVirtProperties.iconsBucketName()).object(name)
                            .tags(Map.of(IMAGE_TYPE, types)).build());
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public FileInformation getFile(String id, Long start, Long end) {
        if (!id.contains(ITEMID_SEPARATOR)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "id must be formatted {itemId@attributeName}");
        }

        String[] infos = id.split(ITEMID_SEPARATOR);
        String attributeName = infos[infos.length - 1];
        String itemId = id.split(ITEMID_SEPARATOR + attributeName)[0];

        com.provoly.virt.entity.Item item = getItemsService.get(new ItemId(itemId));
        String fileId = item.getAttributeSimple(attributeName).getValue().toString();

        StatObjectResponse objectStats = getStatObjectResponse(fileId, dataVirtProperties.filesBucketName());
        if (objectStats == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "File not found");
        }
        long objectSize = objectStats.size();

        if (end == null) {
            end = objectSize - 1L;
            start = 0L;
        }

        if (end > objectSize) {
            end = objectSize - 1L;
        }

        GetObjectArgs.Builder builded = GetObjectArgs.builder()
                .bucket(dataVirtProperties.filesBucketName())
                .object(fileId)
                .offset(start)
                .length(end + 1);

        try (InputStream is = minio.getObject(builded.build())) {
            return new FileInformation(id, objectStats, is, start, end);
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public FileInformation getIcon(String filename) {
        return getFileFromBucket(filename, dataVirtProperties.iconsBucketName());
    }

    public FileInformation getFile(String filename) {
        return getFileFromBucket(filename, dataVirtProperties.filesBucketName());
    }

    public FileInformation getFileFromBucket(String filename, String bucket) {
        filename = getFormattedPath(filename, bucket);
        StatObjectResponse objectStats = getStatObjectResponse(filename, bucket);

        if (objectStats == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "File %s not found".formatted(filename));
        }
        GetObjectArgs.Builder builded = GetObjectArgs.builder()
                .bucket(bucket)
                .object(filename);

        try {
            InputStream is = minio.getObject(builded.build());
            return new FileInformation(filename, objectStats, is, 0, objectStats.size()); // Is it necessary to specify a start and end, or is it better to set as null
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<ImageInfoDto> getAllIcons(String type) {
        createBucketIfNeeded(dataVirtProperties.iconsBucketName());
        log.tracef("List icons from bucket %s", dataVirtProperties.iconsBucketName());
        Iterable<Result<Item>> results = minio
                .listObjects(ListObjectsArgs.builder().bucket(dataVirtProperties.iconsBucketName())
                        .prefix(dataVirtProperties.iconsBucketName()).recursive(true).build());

        List<ImageInfoDto> responses = new ArrayList<>();
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                Path p = Paths.get(item.objectName());
                var tags = getTags(item.objectName());

                if (type == null || tags.contains(type)) {
                    responses.add(new ImageInfoDto(p.getFileName().toString(), tags));
                }
            } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }
        return responses;
    }

    public void deleteIcon(String filename) {
        getIcon(filename);
        try {
            filename = getFormattedIconPath(filename);
            minio.removeObject(
                    RemoveObjectArgs.builder().bucket(dataVirtProperties.iconsBucketName()).object(filename).build());
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void uploadFile(MinIoFileUpload fileUpload) throws MinioException, GeneralSecurityException, IOException {
        createBucketIfNeeded(fileUpload.bucket());
        log.tracef("Upload %s to bucket %s", fileUpload.fileName(), fileUpload.bucket());
        var builder = PutObjectArgs.builder()
                .bucket(fileUpload.bucket())
                .object(fileUpload.fileName())
                .contentType(fileUpload.mediaType())
                .stream(fileUpload.inputStream(), -1, partSize);

        if (fileUpload.type() != null) {
            builder.tags(Map.of(IMAGE_TYPE, fileUpload.type()));
        }

        ObjectWriteResponse response = minio.putObject(builder.build());
        log.debugf("Uploaded %s with tag %s and id %s to bucket %s : \r\n %s", response.object(), response.etag(),
                response.versionId(), response.bucket(), response.headers().toString());
    }

    private void createBucketIfNeeded(String bucket) {
        try {
            log.debugf("Check if bucket %s exists", bucket);
            if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                log.debugf("Create bucket %s ", bucket);
                minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            } else {
                log.debugf("Bucket %s already exists", bucket);
            }
        } catch (IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.errorv("Impossible to create bucket : {0}", e.getMessage());
        }
    }

    private StatObjectResponse getStatObjectResponse(String fileId, String bucket) {
        StatObjectResponse objectStats = null;
        try {
            log.debugf("get info for file: %s", fileId);
            objectStats = minio.statObject(StatObjectArgs.builder().bucket(bucket).object(fileId).build());
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.info(e.getMessage());
        }
        return objectStats;
    }

    private List<String> getTags(String fileName)
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        var tags = minio.getObjectTags(
                GetObjectTagsArgs.builder()
                        .bucket(dataVirtProperties.iconsBucketName())
                        .object(fileName).build())
                .get()
                .get(IMAGE_TYPE);
        if (tags == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(tags.split(TAG_SEPARATOR));
    }

    private String getFormattedIconPath(String fileNameSha1) {
        return UriBuilder.fromPath(dataVirtProperties.iconsBucketName()).path(fileNameSha1).build().toString();
    }

    private String getFormattedPath(String fileNameSha1, String bucket) {
        return UriBuilder.fromPath(bucket).path(fileNameSha1).build().toString();
    }

}
