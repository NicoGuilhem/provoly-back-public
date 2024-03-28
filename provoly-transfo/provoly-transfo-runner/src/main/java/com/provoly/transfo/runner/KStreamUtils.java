package com.provoly.transfo.runner;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KStreamUtils {

    private static final Logger log = LoggerFactory.getLogger(KStreamUtils.class);

    private final AdminClient admin;

    KStreamUtils() {
        this.admin = getAdminClient("localhost:9901");
    }

    public boolean finishHandler(Map<MetricName, ? extends Metric> metrics, CountDownLatch latch) {
        //        var groupe = admin.listConsumerGroupOffsets("provoly-transfo-xxx");
        //        groupe.all().whenComplete((offsetMaps, throwable) -> {
        //            var appMap = offsetMaps.get("provoly-transfo-xxx");
        //            for (OffsetAndMetadata offsetAndMetadata : appMap.values()) {
        //                if (offsetAndMetadata.offset() > 0) latch.countDown();
        //            }
        //
        //        });
        //
        return true;

    }

    //    private void test() {
    //        var consumer = admin.describeConsumerGroups(Collections.singleton("provoly-transfo-xxx"));
    //        consumer.all().whenComplete(groupResult -> {
    //            for (ConsumerGroupDescription group : groupResult.values()) {
    //                group.members().forEach(m -> m.notify());
    //
    //            }
    //        });
    //    }

    private AdminClient getAdminClient(String bootstrapServerConfig) {

        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
        return AdminClient.create(config);
    }

    //    private Map<TopicPartition, Long> getConsumerGrpOffsets(String groupId)
    //            throws ExecutionException, InterruptedException {
    //        ListConsumerGroupOffsetsResult info = adminClient.listConsumerGroupOffsets(groupId);
    //        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = info.partitionsToOffsetAndMetadata().get();
    //
    //        Map<TopicPartition, Long> groupOffset = new HashMap<>();
    //        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : topicPartitionOffsetAndMetadataMap.entrySet()) {
    //            TopicPartition key = entry.getKey();
    //            OffsetAndMetadata metadata = entry.getValue();
    //            groupOffset.putIfAbsent(new TopicPartition(key.topic(), key.partition()), metadata.offset());
    //        }
    //        return groupOffset;
    //    }
}
