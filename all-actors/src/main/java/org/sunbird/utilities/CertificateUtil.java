package org.sunbird.utilities;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sunbird.ActorOperations;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.request.RequestParams;
import org.sunbird.response.Response;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * this class will be used to add , retrive certificates from ES
 * @author anmolgupta
 */
public class CertificateUtil {
    private static final ElasticSearchService elasticSearchService= EsClientFactory.getInstance();
    private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static Logger logger= LoggerFactory.getLogger(CertificateUtil.class);
    private static ObjectMapper mapper = new ObjectMapper();
    private static Localizer localizer = Localizer.getInstance();


    private CertificateUtil(){}



    public static boolean isIdPresent(String certificateId) {
        logger.info("CertificateUtil:isIdPresent:get id to search in ES:"+certificateId);
        Map<String,Object> response = (Map)ElasticSearchHelper.getResponseFromFuture(elasticSearchService.getDataByIdentifier(JsonKeys.CERT_ALIAS,certificateId));
        if (MapUtils.isNotEmpty(response)) {
                return true;
        }
        logger.info("CertificateUtil:isIdPresent: id not found");
        return false;
    }

    public static Response getCertRecordByID(String id) throws BaseException {
        return cassandraOperation.getRecordById(JsonKeys.SUNBIRD,JsonKeys.CERT_REGISTRY,id);
    }

    public static Boolean deleteRecord(String id, ActorRef certBackgroundActorRef) throws BaseException {
        Boolean bool = (Boolean)ElasticSearchHelper.getResponseFromFuture(elasticSearchService.delete(JsonKeys.CERT_ALIAS,id));
        logger.info("Data deleted from ES for id "+id);
        //Delete the data from cassandra
        Request req = new Request();
        req.setOperation(ActorOperations.DELETE_CERT_CASSANDRA.getOperation());
        req.getRequest().put(JsonKeys.ID,id);
        RequestParams params = new RequestParams();
        params.setMsgid(MDC.get(JsonKeys.REQUEST_MESSAGE_ID));
        req.setParams(params);
        certBackgroundActorRef.tell(req, ActorRef.noSender());
        return bool;
    }

    public static Response insertRecord(Map<String,Object>certAddReqMap, ActorRef certBackgroundActorRef) throws BaseException {
        Map<String,Object>certMap = new HashMap<>();
        long createdAt = System.currentTimeMillis();
        certAddReqMap.put(JsonKeys.CREATED_AT,createdAt);
        certAddReqMap.put(JsonKeys.UPDATED_AT,null);
        certMap.putAll(certAddReqMap);

        try{
        certMap.put(JsonKeys.CREATED_AT,new Timestamp(createdAt));
        Map<String, Object> data = (Map<String, Object>) certAddReqMap.get(JsonKeys.DATA);
        //We started with elastic search, The data object was the sole thing to start with. Then we added a Cassandra table.
        //as certificate json size is now about 650 KB, due to printUri in json which is a materialised view of svg, so we should stop pushing the printUri as part data into the
        //cassandra and ES
        if (data.containsKey(JsonKeys.PRINT_URI)) {
            ((Map<String, Object>) certAddReqMap.get(JsonKeys.DATA)).remove(JsonKeys.PRINT_URI);
        }
        certMap.put(JsonKeys.DATA,mapper.writeValueAsString(certAddReqMap.get(JsonKeys.DATA)));
        certMap.put(JsonKeys.RELATED,mapper.writeValueAsString(certAddReqMap.get(JsonKeys.RELATED)));
        certMap.put(JsonKeys.RECIPIENT,mapper.writeValueAsString(certAddReqMap.get(JsonKeys.RECIPIENT)));
        } catch (Exception ex) {
            logger.error("CertificateUtil:insertRecord: JsonProcessingException occurred.",ex);
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA,getLocalizedMessage(IResponseMessage.INVALID_REQUESTED_DATA,null), ResponseCode.CLIENT_ERROR.getCode());
        }
        Response response = cassandraOperation.insertRecord(JsonKeys.SUNBIRD,JsonKeys.CERT_REGISTRY,certMap);
        logger.info("CertificateUtil:insertRecord: record successfully inserted with id"+certAddReqMap.get(JsonKeys.ID));
        //index data to ES
        Request req = new Request();
        RequestParams params = new RequestParams();
        params.setMsgid(MDC.get(JsonKeys.REQUEST_MESSAGE_ID));
        req.setParams(params);
        req.setOperation(ActorOperations.ADD_CERT_ES.getOperation());
        req.getRequest().put(JsonKeys.REQUEST,certAddReqMap);
        certBackgroundActorRef.tell(req, ActorRef.noSender());
        return response;

    }

    public static  Map<String,Object> getCertificate(String certificateId) {
        logger.info("CertificateUtil:isIdPresent:get id to search in ES:"+certificateId);
        Map<String,Object> response = (Map)ElasticSearchHelper.getResponseFromFuture(elasticSearchService.getDataByIdentifier(JsonKeys.CERT_ALIAS,certificateId));
        logger.info("CertificateUtil:isIdPresent:got response from ES.");
        return response;
    }

    public static Future<HttpResponse<JsonNode>> makeAsyncPostCall(String apiToCall, String requestBody, Map<String,String>headerMap){
        logger.info("CertificateUtil:makePostCall:get request to make post call for API:"+apiToCall);
        Future<HttpResponse<JsonNode>> jsonResponse
                    = Unirest.post(apiToCall)
                    .headers(headerMap)
                    .body(requestBody)
                    .asJsonAsync();
            return jsonResponse;
        }
    
    public static HttpResponse<String> makeAsyncGetCallString(String apiToCall, Map<String,String>headerMap) throws ExecutionException, InterruptedException {
        logger.info("CertificateUtil:makeAsyncGetCallString:get request to make get call for API:"+apiToCall);
        HttpResponse<String> jsonResponse
          =  Unirest.get(apiToCall)
          .headers(headerMap)
          .asStringAsync().get();
        return jsonResponse;
    }
    
    public static Future<HttpResponse<JsonNode>> makeAsyncGetCall(String apiToCall, Map<String,String>headerMap){
        logger.info("CertificateUtil:makeAsyncGetCall:get request to make get call for API:"+apiToCall);
        Future<HttpResponse<JsonNode>> jsonResponse
          = Unirest.get(apiToCall)
          .headers(headerMap)
          .asJsonAsync();
        return jsonResponse;
    }
    private static String getLocalizedMessage(String key, Locale locale){
        return localizer.getMessage(key, locale);
    }

}



