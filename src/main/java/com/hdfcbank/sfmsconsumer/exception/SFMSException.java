package com.hdfcbank.sfmsconsumer.exception;

import com.hdfcbank.sfmsconsumer.model.Fault;

import java.util.List;

public class SFMSException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    List<Fault> errors;

    public SFMSException(String message) {
        super(message);
    }

    public SFMSException() {

    }

    public SFMSException(String message, Throwable e) {
        super(message, e);
    }

    public SFMSException(String message, List<Fault> errors) {
        super(message);
        this.errors = errors;
    }

    public List<Fault> getErrors() {
        return errors;
    }

    public void setErrors(List<Fault> errors) {
        this.errors = errors;
    }
}
