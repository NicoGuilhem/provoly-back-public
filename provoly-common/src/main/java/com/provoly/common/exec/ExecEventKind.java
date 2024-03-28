package com.provoly.common.exec;

// TODO : Should be in provoly-exec

public enum ExecEventKind {
    TOPIC_CREATED, // Dump of dataset in topic started
    TOPIC_LOADED, // All dataset items dump to topic
    JOB_TERMINATED
}
