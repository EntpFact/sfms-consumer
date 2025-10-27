package com.hdfcbank.sfmsconsumer.model;


import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class ExceptionDetail {

    private String exceptionType;
    private String exceptionDesc;

}
