package com.provoly.ref.group;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.inject.Inject;

import com.provoly.common.user.Role;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.groups.GroupController;
import com.provoly.ref.groups.GroupWrite;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

import org.junit.jupiter.api.Test;

@QuarkusTest
public class GroupServiceTest {
    @Inject
    GroupController groupController;

    @Test
    @TestSecurity(user = "testUser", roles = { Role.STR_DASHBOARD_READ, Role.STR_DASHBOARD_WRITE })
    public void saveGroup_shouldSucceed() {
        GroupWrite groupWrite = new GroupWrite(UUID.randomUUID(), "TEST");
        groupController.saveGroup(groupWrite);

        assertThat(groupController.getAll())
                .extracting(EntityNamed::getName)
                .contains("TEST");
    }
}
