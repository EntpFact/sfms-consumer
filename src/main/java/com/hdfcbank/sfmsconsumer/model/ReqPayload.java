package com.hdfcbank.sfmsconsumer.model;


import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReqPayload {

    private Header header;
    private Body body;
    private ExceptionDetail exceptionDetail;
}
