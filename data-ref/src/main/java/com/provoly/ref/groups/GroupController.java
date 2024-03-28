package com.provoly.ref.groups;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.common.user.Role;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @POST
    @RolesAllowed({ Role.STR_DASHBOARD_WRITE, Role.STR_DATASET_WRITE })
    public void saveGroup(GroupWrite groupWrite) {
        groupService.addGroup(groupWrite);
    }

    @GET
    @RolesAllowed({ Role.STR_DASHBOARD_READ, Role.STR_DATASET_WRITE })
    public List<Group> getAll() {
        return groupService.getAll();
    }
}
