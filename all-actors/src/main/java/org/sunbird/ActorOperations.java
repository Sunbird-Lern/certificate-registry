package org.sunbird;

/**
 * this is an actor operation class
 * @author anmolgupta
 */
public enum ActorOperations {

    ADD("add"),
    VALIDATE("validate"),
    DOWNLOAD("download"),
    GENERATE("generate"),
    VERIFY("verify"),
    ADD_CERT_ES("add_cert_es"),
    DELETE_CERT_CASSANDRA("delete_cert_cassandra"),
    READ("read"),
    SEARCH("search"),
    SEARCHV2("searchV2"),
    READ_CERT_META_DATA("readCertMetaData"),
    DOWNLOADV2("downloadV2");

    private String operation;

    ActorOperations(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}

