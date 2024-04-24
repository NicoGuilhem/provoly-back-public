package com.provoly.link;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import com.provoly.clients.LinkService;
import com.provoly.common.link.LinkDetailsDto;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.scheduler.Scheduled;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import kafka.KafkaTools;

@ApplicationScoped
public class LinkToRelationService {

    @Inject
    Logger log;

    @Inject
    KafkaTools kafkaTools;

    @Inject
    TopologyProducer topologyProducer;

    @Inject
    @RestClient
    LinkService linkService;

    private final Map<LinkDetailsDto, KafkaStreams> runningTopologies = new HashMap<>();

    @Scheduled(every = "{provoly.link.refresh}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void refreshTopologyList() {
        log.infof("Checking kafka streams states. %d running", runningTopologies.size());

        Collection<LinkDetailsDto> expectedLinks = linkService.getAllLinks();

        // Remove all streams in error from running topologies
        log.debugf("Remove all streams in error state and all deleted links...");
        runningTopologies.entrySet().removeIf(runningEntry -> {
            // Remove if stream is in error state
            var runningLink = runningEntry.getKey();
            var runningStream = runningEntry.getValue();
            if (runningStream.state() == KafkaStreams.State.ERROR) {
                log.warnf("Remove stream %s as it is in error state", runningLink);
                runningStream.close();
                return true;
            }

            // Remove if it is a deleted link
            if (!expectedLinks.contains(runningLink)) {
                log.infof("Stopping stream for link %s", runningLink);
                runningStream.close();
                return true;
            }

            return false;
        });

        log.debug("Adding missing topologies");
        for (LinkDetailsDto link : expectedLinks) {
            if (runningTopologies.containsKey(link))
                continue;
            Topology topology = topologyProducer.buildTopology(link);
            if (topology == null) {
                log.warnf("Skipping link " + link + " as input topic not yet exists");
                continue;
            }
            log.infof("Starting a new Topology for %s", link);
            var kafkaStream = kafkaTools.buildStream(link.id.toString(), topology);
            // kafkaStream.cleanUp(); // Uncomment in dev when localstorage should be reset for testing purpose
            kafkaStream.start();
            runningTopologies.put(link, kafkaStream);
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("Stopping Kafka Streams pipeline");
        runningTopologies.values().forEach(KafkaStreams::close);
    }

}
