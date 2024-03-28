package com.provoly.ref.abac;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import com.provoly.common.VariableType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;

@Entity
public class ContextVariable {
    @Id
    String name;

    @Enumerated(EnumType.STRING)
    VariableType type;

    String valueChar;
    int valueInteger;
    Double valueDouble;
    Date valueDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
        this.type = type;
    }

    public String getValueChar() {
        return valueChar;
    }

    public void setValueChar(String valueChar) {
        this.valueChar = valueChar;
    }

    public int getValueInteger() {
        return valueInteger;
    }

    public void setValueInteger(int valueInteger) {
        this.valueInteger = valueInteger;
    }

    public Double getValueDouble() {
        return valueDouble;
    }

    public void setValueDouble(Double valueDouble) {
        this.valueDouble = valueDouble;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public Object getValue() {
        return switch (getType()) {
            case INTEGER -> getValueInteger();
            case DOUBLE -> getValueDouble();
            case STRING -> getValueChar();
            case DATE -> getValueDate();
            case LIST, UUID -> throw new BusinessException(ErrorCode.TECHNICAL, "Unhandled type: %s".formatted(getType()));
        };
    }

    public void setValue(Object value) {
        try {
            switch (getType()) {
                case INTEGER:
                    setValueInteger((int) value);
                    break;
                case DOUBLE:
                    setValueDouble((Double) value);
                    break;
                case STRING:
                    setValueChar((String) value);
                    break;
                case DATE:
                    Date date = new SimpleDateFormat("yyyy/MM/dd").parse((String) value);
                    setValueDate(date);
                    break;
                case LIST, UUID:
                    throw new BusinessException(ErrorCode.TECHNICAL, "Unhandled type: %s".formatted(getType()));
            }
        } catch (ClassCastException | ParseException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Mismatch between type and value : " + e.getMessage());
        }
    }

}
