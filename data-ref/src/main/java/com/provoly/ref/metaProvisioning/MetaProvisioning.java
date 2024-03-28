package com.provoly.ref.metaProvisioning;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import com.provoly.common.Default;
import com.provoly.ref.entity.EntityNamed;
import com.provoly.ref.metadata.MetadataDef;
import com.provoly.ref.user.metadata.UserProfile;

@Entity
public class MetaProvisioning extends EntityNamed {
    @ManyToOne
    @JoinColumn(name = "metadata_def_id")
    private MetadataDef metadata;

    @ManyToOne
    private UserProfile userProfile;

    protected MetaProvisioning() {
        super();
    }

    @Default
    public MetaProvisioning(UUID id) {
        super(id);
    }

    public MetadataDef getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataDef metadata) {
        this.metadata = metadata;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }
}
