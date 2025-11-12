package com.hdfcbank.sfmsconsumer.exception;


import com.hdfcbank.sfmsconsumer.model.ReqPayload;

public class SFMSConsumerException extends RuntimeException {
    public static class DBException extends RuntimeException {

        private final String request;
        private final ReqPayload payload;
        public DBException(String message, ReqPayload payload, Throwable e) {
            super(message, e);
            this.payload = payload;
            this.request = null;
        }

        public DBException(String message, String request, Throwable e) {
            super(message,e);
            this.request = request;
            this.payload=null;
        }

        public ReqPayload getReqPayload(){
            return this.payload;
        }

        public String getRequest(){
            return this.request;
        }
    }

    // 🔹 Kafka-related exception
    public static class KafkaException extends RuntimeException {
        private final String request;
        private final ReqPayload payload;
        public KafkaException(String message, ReqPayload payload, Throwable e) {
            super(message, e);
            this.payload = payload;
            this.request = null;
        }

        public KafkaException(String message, String request, Throwable e) {
            super(message,e);
            this.request = request;
            this.payload=null;
        }
    }

    public static class NILException extends RuntimeException {
        private final String request;
        private final ReqPayload payload;
        public NILException(String message, ReqPayload payload, Throwable e) {
            super(message, e);
            this.payload = payload;
            this.request = null;
        }

        public NILException(String message, String request, Throwable e) {
            super(message,e);
            this.request = request;
            this.payload=null;
        }

        public ReqPayload getReqPayload(){
            return this.payload;
        }

        public String getRequest(){
            return this.request;
        }
    }
}
