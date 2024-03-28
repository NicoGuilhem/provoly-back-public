package com.provoly.exec;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.provoly.common.exec.ExecContext;
import com.provoly.common.exec.ExecEvent;
import com.provoly.common.exec.ExecEventKind;

import io.smallrye.reactive.messaging.kafka.Record;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventEmitter {

    @Inject
    Logger log;

    @Inject
    @Channel("exec-event")
    Emitter<Record<UUID, ExecEvent>> eventEmitter;

    // Suspend transaction needed to avoid error :
    // Error trying to transactionCommit local transaction: Enlisted connection used without active transaction]
    // Because eventEmitter.send is an async method

    // Event key are the executionId to ensure all message for an execution are in the correct order

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void topicCreated(ExecContext context, UUID executionId) {
        log.infof("Sending a topic created event for execution %s", executionId);
        ExecEvent event = new ExecEvent(executionId, ExecEventKind.TOPIC_CREATED, context);
        eventEmitter.send(Record.of(executionId, event));
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void topicLoaded(ExecContext context, UUID executionId) {
        log.infof("Sending a topic loaded event for execution %s", executionId);
        ExecEvent event = new ExecEvent(executionId, ExecEventKind.TOPIC_LOADED, context);
        eventEmitter.send(Record.of(executionId, event));
    }

    public void jobTerminated(ExecContext context, UUID executionId) {
        log.infof("Sending a job terminated event for execution %s", executionId);
        ExecEvent event = new ExecEvent(executionId, ExecEventKind.JOB_TERMINATED, context);
        eventEmitter.send(Record.of(executionId, event));
    }
}
