package com.provoly.ref.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

@QuarkusTest
public class SlugifyServiceTest {

    @Inject
    SlugifyService slugifyService;

    @Test
    public void test_make_same_slug() {
        var slug = "slug";
        var slugColor = slugifyService.makeSlug(slug);
        var slugColorBis = slugifyService.makeSlug(slug);
        assertThat(slugColor).isNotEqualTo(slugColorBis);
    }

    @Test
    public void test_make_same_attribute_slug() {
        var slug = "slug";
        var oclassName = "oclass";
        var slugColor = slugifyService.makeAttributeSlug(slug, oclassName);
        var slugColorBis = slugifyService.makeAttributeSlug(slug, oclassName);
        assertThat(slugColor).isNotEqualTo(slugColorBis);
    }

}
