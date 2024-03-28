package com.provoly.common.exec;

import java.util.UUID;

public record ExecEvent(
        UUID jobExecutionId,
        ExecEventKind event,
        ExecContext context) {

    public static final String TOPIC_NAME = "exec-event";

    @Override
    public String toString() {
        return "ExecEvent{" +
                "event=" + event.toString() +
                ", jobExecutionId='" + jobExecutionId.toString() + '\'' +
                ", context=" + context +
                '}';
    }
}
