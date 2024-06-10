package com.provoly.ref.datasetversion;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import com.provoly.common.imports.ExtractedMessage;
import com.provoly.common.imports.ImportsMessage;
import com.provoly.ref.entity.EntityIdRepository;

import org.jboss.logging.Logger;

@ApplicationScoped
public class DatasetVersionMessageService {

    private Logger log;

    private EntityIdRepository entityIdRepository;

    private EntityManager em;

    public DatasetVersionMessageService(Logger log, EntityIdRepository entityIdRepository, EntityManager em) {
        this.log = log;
        this.entityIdRepository = entityIdRepository;
        this.em = em;
    }

    public void save(ImportsMessage importsMessage) {
        log.debugf("Convert event message into %s DatasetVersionMessage", importsMessage.messages().size());
        importsMessage
                .messages()
                .forEach(message -> {
                    DatasetVersionMessage datasetVersionMessage = generateDatasetVersionMessage(importsMessage, message);
                    log.tracef("save %s", datasetVersionMessage);
                    entityIdRepository.saveEntity(datasetVersionMessage);
                });
    }

    private DatasetVersionMessage generateDatasetVersionMessage(ImportsMessage importsMessage, ExtractedMessage message) {
        DatasetVersionMessage datasetVersionMessage = new DatasetVersionMessage(
                UUID.randomUUID(),
                message.messageLevel(),
                importsMessage.datasetVersionId(),
                importsMessage.recordId());

        datasetVersionMessage.setExtractMessageCode(message.code());
        if (message.params() != null) {
            datasetVersionMessage.setName(message.params().getName());
            datasetVersionMessage.setType(message.params().getType());
            datasetVersionMessage.setReceivedValue(message.params().getReceivedValue());
        }
        return datasetVersionMessage;
    }

    @Transactional
    public void deleteAllDatasetVersionMessage(UUID datasetVersionId) {
        log.infof("Delete all messages from dataset version %s", datasetVersionId);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete deleteCriteria = cb.createCriteriaDelete(DatasetVersionMessage.class);
        Root<DatasetVersionMessage> root = deleteCriteria.from(DatasetVersionMessage.class);
        deleteCriteria.where(cb.equal(root.get(DatasetVersionMessage_.DATASET_VERSION_ID), datasetVersionId));
        em.createQuery(deleteCriteria).executeUpdate();
    }
}