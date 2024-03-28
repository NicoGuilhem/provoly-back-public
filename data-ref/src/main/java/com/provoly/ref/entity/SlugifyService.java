package com.provoly.ref.entity;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SlugifyService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final int MAX_LENTGH = 100;

    @PersistenceContext
    EntityManager em;

    @Inject
    Logger log;

    public String makeSlug(String name) {
        if (name == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Name is null");
        }
        log.tracef("Generating slug for entity with name %s", name);
        return slug(name, DigestUtils.sha1Hex(UUID.randomUUID().toString()).substring(0, 10));
    }

    public String makeAttributeSlug(String name, String oClassSlug) {
        if (name == null || oClassSlug == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Attribute name or oclass slug is null");
        }
        log.tracef("Generating slug for attribute with name %s", name);
        return slug(name, DigestUtils.sha1Hex(UUID.randomUUID().toString()).substring(0, 10));
    }

    private String slug(String name, String sha1) {
        String noWhitespace = WHITESPACE.matcher(name).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slugName = sha1 + "_" + NONLATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);
        if (slugName.length() > MAX_LENTGH) {
            slugName = slugName.substring(0, MAX_LENTGH);
        }

        log.infof("slug for name %s is : %s", name, slugName);
        return slugName;
    }

}
