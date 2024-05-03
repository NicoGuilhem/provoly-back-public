package com.provoly.ref.metadata;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.provoly.ref.dashboard.dto.DashboardWriteDto;
import com.provoly.ref.entity.EntityType;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

public class MetadataServiceTest {

    MetadataService metadataService;

    @Test
    public void updateMetadataByEntityType_with_no_metadata_skipping() {
        metadataService = spy(new MetadataService(null, null, null, null, mock(Logger.class)));
        var dashboard = new DashboardWriteDto(UUID.randomUUID(), "name", null, "description", false,
                List.of(UUID.randomUUID()), null, null, Map.of());

        metadataService.updateMetadataByEntityType(dashboard, EntityType.DASHBOARD);

        assertThatNoException();
        verify(metadataService, times(0)).getMetadataValueByEntityId(any());
    }
}
