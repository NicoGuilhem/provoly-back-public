package com.provoly.ref.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.VariableType;
import com.provoly.common.metadata.UserMetadataValueWriteDto;
import com.provoly.common.metadata.UserProfileValueReadDto;
import com.provoly.common.user.Role;
import com.provoly.common.user.UserDto;
import com.provoly.ref.KeycloakClientBuilder;
import com.provoly.ref.user.metadata.UserProfile;
import com.provoly.ref.user.metadata.UserProfileService;
import com.provoly.security.CurrentSubjectProvider;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserControllerTest {

    @Inject
    UserController userController;

    @Inject
    UserProfileService userProfileService;
    @InjectMock
    CurrentSubjectProvider currentSubjectProvider;
    @Inject
    KeycloakClientBuilder keycloakClientBuilder;
    private String iamuseradminSub;
    private static final UUID userProfileId = UUID.randomUUID();

    private UserProfile addUserProfile(String suffixe) {
        return new UserProfile(userProfileId, "nameMetadataDef" + suffixe, VariableType.STRING, null,
                UUID.randomUUID().toString());
    }

    private void addProfileToUser(UserProfile metadataDefDto, UserDto originalUser, String... profileValues) {
        UserMetadataValueWriteDto metadataValueWriteDto = new UserMetadataValueWriteDto();
        List<String> list;
        if (profileValues == null || profileValues.length == 0) {
            list = List.of("nouvelleValeur");
        } else {
            list = Arrays.asList(profileValues);
        }
        metadataValueWriteDto.setValues(list);
        userController.addProfilesForUser(originalUser.getId(), metadataDefDto.getId(), metadataValueWriteDto);
    }

    @BeforeEach
    public void init() {
        try (var keycloak = keycloakClientBuilder.build()) {
            var users = keycloak.realm("provoly").users().search("iamuseradmin");
            if (users.size() != 1) {
                throw new IllegalArgumentException("Unable to retrieve keycloak admin user id " + users);
            }
            var user = users.getFirst();
            iamuseradminSub = user.getId();
        }
        given(currentSubjectProvider.getSub()).willReturn(iamuseradminSub);

    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_USER_READ })
    public void shouldGetAllUser() {
        given(currentSubjectProvider.getSub()).willReturn(iamuseradminSub);

        UserDto userSaved = userController.getCurrentUserInfo();
        assertThat(userSaved.getId()).isNotNull();

        List<UserDto> savedUsers = userController.getAllUsers();
        assertThat(savedUsers).contains(userSaved);
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_USER_REF_WRITE, Role.STR_METADATA_USER_READ,
            Role.STR_METADATA_USER_WRITE })
    void shouldAddMetadata() {
        var userProfile = addUserProfile("addMethod");
        userProfileService.saveUserProfile(userProfile);

        UserDto originalUser = userController.getCurrentUserInfo();
        List<UserProfileValueReadDto> userProfiles = userController.getProfileValuesByUserId(originalUser.getId());
        assertThat(userProfiles).isEmpty();
        addProfileToUser(userProfile, originalUser);

        List<UserProfileValueReadDto> userMetadatas = userController.getProfileValuesByUserId(originalUser.getId());
        assertThat(userMetadatas).extracting("value", "userProfile.id")
                .containsExactly(tuple("nouvelleValeur", userProfileId));

        // cleaning
        userController.deleteProfileForUser(originalUser.getId(), userProfile.getId());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_USER_REF_WRITE, Role.STR_METADATA_USER_READ,
            Role.STR_METADATA_USER_WRITE })
    void shouldAddMetadatas() {
        var userProfile = addUserProfile("fakeProfile");
        userProfileService.saveUserProfile(userProfile);

        UserDto originalUser = userController.getCurrentUserInfo();
        List<UserProfileValueReadDto> userProfiles = userController.getProfileValuesByUserId(originalUser.getId());
        assertThat(userProfiles).isEmpty();
        addProfileToUser(userProfile, originalUser, "nouvelleValeur", "nouvelleAutreValeur");

        List<UserProfileValueReadDto> userMetadatas = userController.getProfileValuesByUserId(originalUser.getId());
        assertThat(userMetadatas).extracting("value", "userProfile.id")
                .containsExactly(tuple("nouvelleValeur", userProfileId), tuple("nouvelleAutreValeur", userProfileId));

        // cleaning
        userController.deleteProfileForUser(originalUser.getId(), userProfile.getId());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_USER_REF_WRITE, Role.STR_METADATA_USER_READ,
            Role.STR_METADATA_USER_WRITE })
    void shouldUpdateExistingMetadatas() {
        var userProfile = addUserProfile("fakeUpdateProfile");
        userProfileService.saveUserProfile(userProfile);

        UserDto originalUser = userController.getCurrentUserInfo();
        List<UserProfileValueReadDto> userProfiles = userController.getProfileValuesByUserId(originalUser.getId());
        assertThat(userProfiles).isEmpty();
        addProfileToUser(userProfile, originalUser, "nouvelleValeur", "nouvelleAutreValeur");

        List<UserProfileValueReadDto> userMetadatas = userController.getProfileValuesByUserId(originalUser.getId());
        assertThat(userMetadatas).extracting("value", "userProfile.id")
                .containsExactly(tuple("nouvelleValeur", userProfileId), tuple("nouvelleAutreValeur", userProfileId));

        addProfileToUser(userProfile, originalUser, "nouvelleValeur", "nouvelleAutreValeurUpdated", "valeurFinale");

        userMetadatas = userController.getProfileValuesByUserId(originalUser.getId());
        assertThat(userMetadatas).extracting("value", "userProfile.id")
                .containsExactly(tuple("nouvelleValeur", userProfileId),
                        tuple("nouvelleAutreValeurUpdated", userProfileId),
                        tuple("valeurFinale", userProfileId));
        // cleaning
        userController.deleteProfileForUser(originalUser.getId(), userProfile.getId());
    }

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_METADATA_USER_REF_WRITE, Role.STR_METADATA_USER_WRITE,
            Role.STR_METADATA_USER_READ })
    void shouldDeleteMetadata() {
        var userProfile = addUserProfile("deleteMethod");
        userProfileService.saveUserProfile(userProfile);
        UserDto user = userController.getCurrentUserInfo();
        addProfileToUser(userProfile, user);

        userController.deleteProfileForUser(user.getId(), userProfile.getId());

        List<UserProfileValueReadDto> updatedProfiles = userController.getProfileValuesByUserId(user.getId());
        assertThat(updatedProfiles).extracting(u -> u.getUserProfile().id).doesNotContain(userProfile.getId());

    }

    @Test
    void getUser_should_throw_unauthenticated_when_user_is_not_connected() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("users/me")
                .then()
                .statusCode(401);
    }
}
