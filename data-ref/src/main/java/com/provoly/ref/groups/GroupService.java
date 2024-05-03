package com.provoly.ref.groups;

import static com.provoly.common.user.SystemGroup.ALL;
import static com.provoly.common.user.SystemGroup.AUTHENTICATED;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.provoly.common.dataset.GroupRights;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

import org.jboss.logging.Logger;

@ApplicationScoped
public class GroupService {
    private GroupMapper groupMapper;
    private Logger log;
    @PersistenceContext
    private GroupRepository groupRepository;

    public GroupService(GroupMapper groupMapper,
            Logger log, GroupRepository groupRepository) {
        this.groupMapper = groupMapper;
        this.groupRepository = groupRepository;
        this.log = log;
    }

    public void addGroup(GroupWrite groupWrite) {
        groupRepository.save(groupMapper.toModel(groupWrite));
    }

    public Group getById(UUID groupId) {
        return groupRepository.getById(groupId);
    }

    public void updateEntityGroups(Map<String, List<GroupRights>> groupsName, UUID entityId,
            WithGroupEntityType entityType) {

        if (groupsName == null) {
            log.debugf("No group provided for %s entity %s", entityType.name(), entityId);
            return;
        }
        groupRepository.getGroupsByEntityId(entityId).forEach(groupRelations -> {
            log.debugf("Remove group %s association to %s %s", groupRelations.getId(), entityType, entityId);
            groupRepository.deleteGroupFromEntity(entityId, groupRelations.getGroup());
        });
        associateGroupToEntity(groupsName, entityId, entityType);
    }

    @Transactional
    public void associateGroupToEntity(Map<String, List<GroupRights>> accessRightsByGroup, UUID entityId,
            WithGroupEntityType entityType) {

        var rightsByExistingGroup = accessRightsByGroup.entrySet().stream()
                .map(k -> new AbstractMap.SimpleEntry<>(groupRepository.getGroupByName(k.getKey()), k.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        var systemGroupWithWrite = rightsByExistingGroup.entrySet().stream()
                .anyMatch(entry -> entry.getKey().isSystem()
                        && entry.getValue().contains(GroupRights.WRITE));

        if (systemGroupWithWrite) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Write right cannot be assigned to %s and %s groups".formatted(AUTHENTICATED, ALL));
        }

        rightsByExistingGroup.forEach((group, rights) -> {
            if (!groupRepository.isGroupAssignedToEntity(entityId, group)) {
                log.debugf("Associate group %s to %s %s", group.getName(), entityType, entityId);
                var canWrite = !group.isSystem() && rights.contains(GroupRights.WRITE);
                groupRepository.saveGroupRelation(entityType, group, entityId, canWrite);
            }
        });
    }

    public Collection<String> getGroupsNames(Map<String, List<GroupRights>> groups) {
        if (groups == null) {
            return null;
        }
        return groups.keySet();
    }
}
