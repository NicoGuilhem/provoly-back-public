package com.provoly.ref.user;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.Path;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataValueWriteDto;
import com.provoly.common.metadata.UserProfileValueReadDto;
import com.provoly.common.user.UserDto;
import com.provoly.ref.entity.EntityIdService;
import com.provoly.ref.entity.EntityId_;
import com.provoly.ref.groups.GroupService;
import com.provoly.ref.user.metadata.*;
import com.provoly.security.AnonymousConfiguration;
import com.provoly.security.CurrentSubjectProvider;

import org.jboss.logging.Logger;

/**
 * {@link ProvolyUser} in the sense database.
 */
@ApplicationScoped
public class UserService {
    private Logger log;
    private CurrentSubjectProvider currentSubjectProvider;
    private UserProfileService userProfileService;
    private UserProfileMapper userProfileMapper;
    private EntityIdService entityIdService;
    private EntityManager em;
    private AnonymousConfiguration anonymousConf;

    private GroupService groupService;

    public UserService(Logger log,
            CurrentSubjectProvider currentSubjectProvider,
            UserProfileService userProfileService,
            UserProfileMapper userProfileMapper,
            EntityIdService entityIdService,
            EntityManager em,
            AnonymousConfiguration anonymousConf, GroupService groupService) {
        this.log = log;
        this.currentSubjectProvider = currentSubjectProvider;
        this.userProfileService = userProfileService;
        this.userProfileMapper = userProfileMapper;
        this.entityIdService = entityIdService;
        this.em = em;
        this.anonymousConf = anonymousConf;
        this.groupService = groupService;
    }

    @Transactional
    public UserDto getCurrentUserDto() {
        String subject = currentSubjectProvider.getSub();
        if (!anonymousConf.isAnonymousEnabled() && subject == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Anonymous access is not allowed.");
        }

        return isAnonymous() ? constructAnonymousUserDto()
                : getCurrentUserDto(subject);
    }

    public boolean isAnonymous() {
        String subject = currentSubjectProvider.getSub();
        return anonymousConf.isAnonymousEnabled() && anonymousConf.anonymousSub().equals(subject);
    }

    @Transactional
    public ProvolyUser getCurrentUser() {
        String sub = currentSubjectProvider.getSub();
        log.infof("Requesting the current user for claim=[%s]", sub);
        return getOrCreateProvolyUserByClaim(sub);
    }

    public boolean isCurrentUser(ProvolyUser user) {
        return user.getSubject().equals(currentSubjectProvider.getSub());
    }

    @Transactional
    public List<ProvolyUser> getAll() {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(ProvolyUser.class);
        var root = q.from(ProvolyUser.class);
        q = q.where(cb.notEqual(root.get(ProvolyUser_.subject), root.get(EntityId_.id).as(String.class)));
        return em.createQuery(q).getResultList();
    }

    private UserDto getCurrentUserDto(String claim) {
        log.infof("Requesting the current user Dto for claim %s", claim);
        var provolyUser = getOrCreateProvolyUserByClaim(claim);
        UserDto userDto = getCurrentUserDtoFrom(provolyUser);
        log.debugf("-> Current user %s", userDto);
        return userDto;
    }

    private ProvolyUser getOrCreateProvolyUserByClaim(String subject) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(ProvolyUser.class);
        var root = q.from(ProvolyUser.class);
        q = q.where(cb.equal(root.get(ProvolyUser_.subject), subject));
        var users = em.createQuery(q).getResultList();

        switch (users.size()) {
            case 0 -> {
                // Add a user in database and return it
                var currentUser = new ProvolyUser(UUID.randomUUID(), subject,
                        currentSubjectProvider.getGivenName(),
                        currentSubjectProvider.getFamilyName(),
                        currentSubjectProvider.getEmail());
                log.infof("First time for user %s => add it to local database", currentUser);
                em.persist(currentUser);
                currentUser.setGroups(groupService.getGroupByNames(currentSubjectProvider.getGroups()));
                return currentUser;
            }
            case 1 -> {
                var user = users.getFirst();
                user.setGroups(groupService.getGroupByNames(currentSubjectProvider.getGroups().stream().toList()));
                return users.getFirst();
            }
            default ->
                throw new BusinessException(ErrorCode.TECHNICAL, "Multiple users with same claim in db : " + users);
        }
    }

    private UserDto getCurrentUserDtoFrom(ProvolyUser provolyUser) {
        var userDto = new UserDto();
        userDto.setId(provolyUser.getId());
        userDto.setName(currentSubjectProvider.getGivenName());
        userDto.setEmail(currentSubjectProvider.getEmail());
        userDto.setFamilyName(currentSubjectProvider.getFamilyName());
        userDto.setRoles(currentSubjectProvider.getRoles());
        userDto.setGroups(currentSubjectProvider.getGroups()); // FIX ME Should be provoly known groups  ?
        return userDto;
    }

    private UserDto constructAnonymousUserDto() {
        var userDto = new UserDto();
        userDto.setId(UUID.fromString(anonymousConf.anonymousSub()));
        userDto.setRoles(currentSubjectProvider.getRoles());
        userDto.setGroups(currentSubjectProvider.getGroups());
        userDto.setIsAnonymous(true);
        return userDto;
    }

    @Transactional
    public UserProfile getUserProfile(UUID userProfileId) {
        return entityIdService.getById(userProfileId, UserProfile.class);
    }

    @Transactional
    public void checkProvolyUserEntityExists(UUID id) {
        entityIdService.checkEntityExists(id, ProvolyUser.class);
    }

    @Transactional
    public void checkUserProfileEntityExists(UUID id) {
        entityIdService.checkEntityExists(id, UserProfile.class);
    }

    @Transactional
    public List<UserProfileValue> getMetadataValueByUserId(UUID userId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UserProfileValue.class);
        var root = q.from(UserProfileValue.class);
        q = q.where(cb.equal(root.get(UserProfileValue_.provolyUserId), userId));
        return em.createQuery(q).getResultList();
    }

    public List<UserProfileValueReadDto> getUserProfileValueReadDtos(UUID userId) {
        List<UserProfileValue> userProfileValues = getMetadataValueByUserId(userId);
        List<UserProfileValueReadDto> userProfileValueReadDtos = new ArrayList<>();

        userProfileValues
                .forEach(userProfileValue -> userProfileValueReadDtos
                        .add(userProfileMapper.toUserProfileValueDto(userProfileValue,
                                getUserProfile(userProfileValue.getUserProfileId()))));
        return userProfileValueReadDtos;
    }

    @Transactional
    public void addUserProfile(UUID provolyUserId, UUID userProfileId, MetadataValueWriteDto metadataValueWriteDto) {
        var userProfile = userProfileService.getById(userProfileId);
        checkProvolyUserEntityExists(provolyUserId);

        var metadataValue = getUserProfileAssignedToUser(provolyUserId, userProfileId);
        metadataValue.ifPresentOrElse(
                mv -> mv.validateAndSetValue(metadataValueWriteDto.getValue(), userProfile.getType(), userProfile.getValues()),
                () -> {
                    var newUserProfileValue = new UserProfileValue(userProfile.getId(), provolyUserId);
                    newUserProfileValue.validateAndSetValue(metadataValueWriteDto.getValue(), userProfile.getType(),
                            userProfile.getValues());
                    entityIdService.saveEntity(newUserProfileValue, false);
                });
    }

    @Transactional
    public void deleteProfileForUser(UUID userId, UUID userProfileId) {
        checkProvolyUserEntityExists(userId);
        checkUserProfileEntityExists(userProfileId);
        var userProfileValue = getUserProfileAssignedToUser(userId, userProfileId);
        userProfileValue.ifPresentOrElse(
                mv -> em.remove(em.merge(mv)),
                () -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "User profile %s is not assigned to user %s".formatted(userProfileId, userId));
                });
    }

    private Optional<UserProfileValue> getUserProfileAssignedToUser(UUID userId, UUID userProfileId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UserProfileValue.class);
        var root = q.from(UserProfileValue.class);
        q = q.where(cb.and(
                cb.equal(root.get(UserProfileValue_.provolyUserId), userId),
                cb.equal(root.get(UserProfileValue_.userProfileId), userProfileId)));
        try {
            return Optional.of(em.createQuery(q).setMaxResults(1).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }

    }

    public List<UUID> getAllUserIdExceptCurrent() {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UUID.class);
        var root = q.from(ProvolyUser.class);
        Path<UUID> idPath = root.get(ProvolyUser_.ID);
        q = q.select(idPath);
        q = q.where(cb.notEqual(root.get(ProvolyUser_.subject), currentSubjectProvider.getSub()));
        return em.createQuery(q).getResultList();
    }

}
