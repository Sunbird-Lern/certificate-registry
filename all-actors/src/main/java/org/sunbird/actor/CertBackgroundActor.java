package org.sunbird.actor;

import org.sunbird.BaseActor;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.request.Request;
import java.util.Map;

public class CertBackgroundActor extends BaseActor {
    private ElasticSearchService elasticSearchService = getESService();
    private CassandraOperation cassandraOperation = getCassandraOperation();
    private static CassandraOperation getCassandraOperation(){
        return ServiceFactory.getInstance();
    }

    private static ElasticSearchService getESService(){
        return EsClientFactory.getInstance();
    }
    @Override
    public void onReceive(Request request) throws Throwable {
        logger.info("CertificationActor:onReceive:request arrived with operation" + request.getOperation());
        String operation = request.getOperation();
        switch (operation) {
            case "add_cert_es":
                add(request);
                break;

            case "delete_cert_cassandra":
                delete(request);
                break;

            default:
                onReceiveUnsupportedMessage("CertificationActor");
        }
    }

    private void delete(Request request) throws BaseException {
        String id = (String) request.getRequest().get(JsonKeys.ID);
        try {
            cassandraOperation.deleteRecord(JsonKeys.SUNBIRD, JsonKeys.CERT_REGISTRY, id);
            logger.info("Data deleted from cassandra for id " + id);
        }catch (Exception ex){
            logger.error("Exception occurred while deleting data from cert_registry for id : "+id,ex);
        }
    }


    private void add(Request request) {
        Map<String,Object> certAddReqMap = (Map<String, Object>) request.getRequest().get(JsonKeys.REQUEST);
        String id = (String)ElasticSearchHelper.getResponseFromFuture(elasticSearchService.save(JsonKeys.CERT_ALIAS,(String)certAddReqMap.get(JsonKeys.ID),certAddReqMap));
        logger.info("ES save response for id "+id);
    }
}