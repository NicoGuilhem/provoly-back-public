package com.provoly.replay.entity;

import java.util.ArrayList;
import java.util.Collection;

import com.provoly.common.error.BusinessException;

public class ErrorReport {

    private String topic;
    private String oClass;
    private Collection<ErrorInfo> errors = new ArrayList<>();

    public ErrorReport() {
    }

    public ErrorReport accumulate(ErrorLine errorLine) {
        if (this.topic != null && !this.topic.equals(errorLine.getTopic())) {
            throw new BusinessException(com.provoly.common.error.ErrorCode.TECHNICAL, "not same topic name");
        }
        this.topic = errorLine.getTopic();
        if (errorLine.isReset()) {
            this.errors.forEach(e -> e.setCount(0));
        } else {
            for (ErrorInfo e : this.errors) {
                if (e.getLabel().equals(errorLine.getCodeError())) {
                    e.setCount(e.getCount() + 1);
                    return this;
                }
            }
            this.errors.add(new ErrorInfo(errorLine.getCodeError(), 1));
        }
        return this;
    }

    public String getTopic() {
        return topic;
    }

    public String getoClass() {
        return oClass;
    }

    public void setoClass(String classId) {
        this.oClass = classId;
    }

    public Collection<ErrorInfo> getErrors() {
        return errors;
    }
}
