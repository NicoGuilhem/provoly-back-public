package com.provoly.ref.metaProvisioning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.VariableType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.metadata.MetaProvisioningDto;
import com.provoly.common.user.Role;
import com.provoly.ref.metadata.MetadataDef;
import com.provoly.ref.metadata.MetadataDefService;
import com.provoly.ref.user.metadata.UserProfile;
import com.provoly.ref.user.metadata.UserProfileService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.assertj.core.api.AutoCloseableBDDSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MetaProvisioningControllerTest {

    private static final UUID userProfileId = UUID.randomUUID();
    private static final UUID metadataId = UUID.randomUUID();
    private static final UUID metaProvisioningId = UUID.randomUUID();
    private static final String metadataName = "test_metadata";
    private static final String metaProvisioningName = "test_meta_provisioning";
    private static final String userProfileName = "test_user_profile";
    private final VariableType typeString = VariableType.STRING;
    @Inject
    UserProfileService userProfileService;
    @Inject
    MetadataDefService metadataDefService;
    @Inject
    MetaProvisioningController metaProvisioningController;
    @Inject
    MetaProvisioningService metaProvisioningService;

    private MetaProvisioningDto metaProvisioningDto;

    @BeforeEach
    public void init() {
        userProfileService
                .saveUserProfile(new UserProfile(userProfileId, userProfileName, typeString, null, "slug_userProfile"));
        metadataDefService.addMetadata(new MetadataDef(metadataId, metadataName, typeString, null, "slug_metadata"));
        metaProvisioningDto = new MetaProvisioningDto(metaProvisioningId, metaProvisioningName, metadataId, userProfileId);
    }

    @AfterEach
    public void clean() {
        metaProvisioningService.removeIfExists(metaProvisioningId);
        metadataDefService.delete(metadataId);
        userProfileService.removeIfExists(userProfileId);
    }

    public MetadataDef initWrongMetadata() {
        var metadata = new MetadataDef(UUID.randomUUID(), "second_metadata", VariableType.DATE, null, "slug_2");
        metadataDefService.addMetadata(metadata);
        return metadata;
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_METADATA_ITEM_REF_READ })
    public void should_addMetaProvisioning_returnOK() {
        // When
        metaProvisioningController.add(metaProvisioningDto);

        var result = metaProvisioningController.getById(metaProvisioningId);

        // Then
        try (AutoCloseableBDDSoftAssertions softly = new AutoCloseableBDDSoftAssertions()) {
            softly.then(result).isNotNull();
            softly.then(result.name()).isEqualTo(metaProvisioningName);
            softly.then(result.metadata().id).isEqualTo(metadataId);
            softly.then(result.metadata().name).isEqualTo(metadataName);
            softly.then(result.userProfile().id).isEqualTo(userProfileId);
            softly.then(result.userProfile().name).isEqualTo(userProfileName);
        }
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE })
    public void should_failWhenUserProfileAlreadyAssociatedWithMetadata_returnKO() {
        // Given
        metaProvisioningService.saveOrUpdate(metaProvisioningDto);

        // Then
        assertThatThrownBy(() -> metaProvisioningController.add(metaProvisioningDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("This user profile is already linked to this metadata item");
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_ITEM_REF_WRITE, Role.STR_METADATA_ITEM_REF_READ })
    public void should_failWhenUserProfileAndMetadataHaveDifferentTypes_returnKO() {

        // Given
        var updatedProvisioningDto = new MetaProvisioningDto(metaProvisioningId, metaProvisioningName,
                initWrongMetadata().getId(), userProfileId);

        // Then
        assertThatThrownBy(() -> metaProvisioningController.add(updatedProvisioningDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Metadata and user profile must have same type");
    }

}