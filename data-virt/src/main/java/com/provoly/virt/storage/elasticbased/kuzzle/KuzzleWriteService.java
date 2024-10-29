package com.provoly.virt.storage.elasticbased.kuzzle;

import static com.provoly.common.model.Type.INSTANT;
import static com.provoly.virt.storage.elasticbased.kuzzle.KuzzleLayout.COLLECTION_NAME;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.Storage;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.item.ItemUpdateMode;
import com.provoly.virt.GeoHolder;
import com.provoly.virt.entity.AttributeSimpleValue;
import com.provoly.virt.entity.AttributeValue;
import com.provoly.virt.entity.Item;
import com.provoly.virt.storage.InsertionError;
import com.provoly.virt.storage.StorageQualifier;
import com.provoly.virt.storage.StorageWriteService;
import com.provoly.virt.storage.elasticbased.ElasticSupport;
import com.provoly.virt.storage.elasticbased.KuzzleClient;

import org.jboss.logging.Logger;

@StorageQualifier(Storage.KUZZLE)
@ApplicationScoped
public class KuzzleWriteService implements StorageWriteService {
    private final KuzzleClient kuzzleClient;
    private final Logger logger;
    private final ElasticSupport elasticSupport;

    public KuzzleWriteService(KuzzleClient kuzzleClient, Logger logger, ElasticSupport elasticSupport) {
        this.kuzzleClient = kuzzleClient;
        this.logger = logger;
        this.elasticSupport = elasticSupport;
    }

    @Override
    public List<InsertionError> addOrUpdate(Collection<Item> items, ItemUpdateMode updateMode) {

        if (updateMode == ItemUpdateMode.MERGE) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Update mode 'MERGE' is not supported by Kuzzle storage");
        }

        var index = items.stream().toList().getFirst().getoClass().getSlug();
        logger.debugf("Store %s items in Kuzzle storage on index %s", items.size(), index);

        var documents = items
                .stream()
                .map(this::convertItemAsKuzzleDocument)
                .toList();

        return kuzzleClient.insertDocuments(index, COLLECTION_NAME, documents);
    }

    private Map<String, Object> convertItemAsKuzzleDocument(Item item) {
        Map<String, Object> body = new HashMap<>();

        // attribute management
        var attributes = item.getAttributes()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, this::getValue));

        // metadata management
        var metadata = new HashMap<>();
        item.getMetadata()
                .forEach(meta -> metadata.put(meta.getName(), meta.getValue()));
        attributes.put("metadata", metadata);

        body.put("_id", item.getIdAsString());
        body.put("body", attributes);
        return body;
    }

    private Object getValue(Map.Entry<String, AttributeValue> entry) {
        if (entry.getValue().getAttributeDef().isMultiValued()) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Multi value is not allowed in Kuzzle storage");
        }
        var value = (AttributeSimpleValue) entry.getValue();
        if (value.getAttributeDef().getField().getType().isGeo()) {
            var geo = (GeoHolder) value.readValueEvenIfNotVisible();
            return geo.getStringAs(GeoFormat.WKT);
        }
        if (value.getAttributeDef().getField().getType() == INSTANT) {
            return elasticSupport.toIsoDate(((AttributeSimpleValue) entry.getValue()).readValueEvenIfNotVisible().toString())
                    .toEpochMilli(); // to avoid date format error, convert to timestamp
        }
        return ((AttributeSimpleValue) entry.getValue()).readValueEvenIfNotVisible();
    }
}
