package com.provoly.ref.customclass;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.validation.constraints.Size;

@Entity
@IdClass(CustomClassId.class)
public class CustomClass {
    /**
     * The max size of content is 100Ko.
     */
    private static final int CONTENT_MAX_SIZE = 100000;

    @Id
    @Size(min = 1, max = 30)
    String domain;

    @Id
    UUID oClass;

    @Size(max = CONTENT_MAX_SIZE)
    String content;

    protected CustomClass() {
        // Only for JPA
    }

    public CustomClass(UUID oClass, String domain, String content) {
        this.oClass = oClass;
        this.domain = domain;
        this.content = content;
    }

    public UUID getoClass() {
        return oClass;
    }

    public void setoClass(UUID oClass) {
        this.oClass = oClass;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
