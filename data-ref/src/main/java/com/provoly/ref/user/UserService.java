package com.provoly.ref.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Path;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.UserMetadataValueWriteDto;
import com.provoly.common.metadata.UserProfileValueReadDto;
import com.provoly.common.user.UserDto;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.entity.EntityId_;
import com.provoly.ref.groups.GroupRepository;
import com.provoly.ref.user.metadata.*;
import com.provoly.security.AnonymousConfiguration;
import com.provoly.security.CurrentSubjectProvider;

import org.hibernate.query.criteria.JpaExpression;
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
    private EntityIdRepository entityIdRepository;
    private EntityManager em;
    private AnonymousConfiguration anonymousConf;

    private GroupRepository groupRepository;

    public UserService(Logger log,
            CurrentSubjectProvider currentSubjectProvider,
            UserProfileService userProfileService,
            UserProfileMapper userProfileMapper,
            EntityIdRepository entityIdRepository,
            EntityManager em,
            AnonymousConfiguration anonymousConf, GroupRepository groupRepository) {
        this.log = log;
        this.currentSubjectProvider = currentSubjectProvider;
        this.userProfileService = userProfileService;
        this.userProfileMapper = userProfileMapper;
        this.entityIdRepository = entityIdRepository;
        this.em = em;
        this.anonymousConf = anonymousConf;
        this.groupRepository = groupRepository;
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
        q = q.where(
                cb.notEqual(root.get(ProvolyUser_.subject), ((JpaExpression<UUID>) root.get(EntityId_.id)).cast(String.class)));
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
                        currentSubjectProvider.getEmail(),
                        currentSubjectProvider.getRoles());
                log.infof("First time for user %s => add it to local database", currentUser);
                em.persist(currentUser);
                currentUser.setGroups(groupRepository.getGroupByNames(currentSubjectProvider.getGroups()));
                return currentUser;
            }
            case 1 -> {
                var user = users.getFirst();
                user.setGroups(groupRepository.getGroupByNames(currentSubjectProvider.getGroups().stream().toList()));
                user.setRoles(currentSubjectProvider.getRoles());
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
        return entityIdRepository.getById(userProfileId, UserProfile.class);
    }

    @Transactional
    public void checkProvolyUserEntityExists(UUID id) {
        entityIdRepository.checkEntityExists(id, ProvolyUser.class);
    }

    @Transactional
    public void checkUserProfileEntityExists(UUID id) {
        entityIdRepository.checkEntityExists(id, UserProfile.class);
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
    public void addUserProfiles(UUID provolyUserId, UUID userProfileId, UserMetadataValueWriteDto metadataValueWriteDto) {
        var userProfile = userProfileService.getById(userProfileId);
        checkProvolyUserEntityExists(provolyUserId);

        var userProfileValues = getUserProfilesAssignedToUser(provolyUserId, userProfileId);
        entityIdRepository.removeEntities(userProfileValues, UserProfileValue.class);

        metadataValueWriteDto.getValues().stream()
                .map(value -> new UserProfileValue(userProfile, provolyUserId, value))
                .forEach(entityIdRepository::saveEntity);
    }

    @Transactional
    public void deleteProfileForUser(UUID userId, UUID userProfileId) {
        var userProfileValues = getUserProfilesAssignedToUser(userId, userProfileId);
        if (userProfileValues.isEmpty()) {
            checkProvolyUserEntityExists(userId);
            checkUserProfileEntityExists(userProfileId);
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "User profile %s is not assigned to user %s".formatted(userProfileId, userId));
        }

        entityIdRepository.removeEntities(userProfileValues, UserProfileValue.class);
    }

    private List<UserProfileValue> getUserProfilesAssignedToUser(UUID userId, UUID userProfileId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UserProfileValue.class);
        var root = q.from(UserProfileValue.class);
        q = q.where(cb.and(
                cb.equal(root.get(UserProfileValue_.provolyUserId), userId),
                cb.equal(root.get(UserProfileValue_.userProfileId), userProfileId)));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public List<UUID> getAllUserIdsExcept(String subject) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(UUID.class);
        var root = q.from(ProvolyUser.class);
        Path<UUID> idPath = root.get(ProvolyUser_.ID);
        q = q.select(idPath);
        q = q.where(cb.notEqual(root.get(ProvolyUser_.subject), subject));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public List<ProvolyUser> getAllUsersWithIds(List<UUID> userIds) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(ProvolyUser.class);
        var root = query.from(ProvolyUser.class);
        query = query.select(root).where(root.get(ProvolyUser_.id).in(userIds));
        return em.createQuery(query).getResultList();
    }

}
