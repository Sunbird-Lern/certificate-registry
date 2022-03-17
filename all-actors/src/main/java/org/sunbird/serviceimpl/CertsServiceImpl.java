package org.sunbird.serviceimpl;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.CertVars;
import org.sunbird.JsonKeys;
import org.sunbird.RegistryCredential;
import org.sunbird.builders.Certificate;
import org.sunbird.builders.Recipient;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.ICertService;
import org.sunbird.utilities.CertificateUtil;
import org.sunbird.utilities.ESResponseMapper;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * this is an implementation class for the cert service , it will handel all the certificate related transactions
 * @author anmolgupta
 */
public class CertsServiceImpl implements ICertService {
    private static Logger logger = LoggerFactory.getLogger(CertsServiceImpl.class);
    private static Localizer localizer = Localizer.getInstance();
    private static ObjectMapper requestMapper = new ObjectMapper();
    static Map<String, String> headerMap = new HashMap<>();
    static {
        headerMap.put("Content-Type", "application/json");
    }

    @Override
    public Response delete(Request request, ActorRef certBackgroundActorRef) throws BaseException {
        Map<String, Object> certAddReqMap = request.getRequest();
        Response response = new Response();
        if(StringUtils.isNotBlank((String)certAddReqMap.get(JsonKeys.OLD_ID))){
            boolean bool = CertificateUtil.deleteRecord((String)certAddReqMap.get(JsonKeys.OLD_ID), certBackgroundActorRef);
            response.getResult().put(JsonKeys.RESPONSE,bool);
            logger.info("CertsServiceImpl:delete Deleted the record from cert_registry table for id "+certAddReqMap.get(JsonKeys.OLD_ID));
        }
        return response;
    }

    @Override
    public String add(Request request, ActorRef certBackgroundActorRef) throws BaseException {
        Map<String,Object> reqMap = request.getRequest();
        if(isPresentRecipientIdAndCertId(request)){
            validateCertAndRecipientId(reqMap);
            deleteOldCertificate((String) reqMap.get(JsonKeys.OLD_ID),certBackgroundActorRef);
        }
        Map<String, Object> certAddReqMap = request.getRequest();
        assureUniqueCertId((String) certAddReqMap.get(JsonKeys.ID));
        processRecord(certAddReqMap,(String) request.getContext().get(JsonKeys.VERSION), certBackgroundActorRef);
        logger.info("CertsServiceImpl:add:record successfully processed with request:"+certAddReqMap.get(JsonKeys.ID));
        return (String)certAddReqMap.get(JsonKeys.ID);
    }

    private void deleteOldCertificate(String oldCertId, ActorRef certBackgroundActorRef) throws BaseException {
        CertificateUtil.deleteRecord(oldCertId, certBackgroundActorRef);
    }

    private void validateCertAndRecipientId(Map<String,Object> reqMap) throws BaseException {
        String certId = (String) reqMap.get(JsonKeys.OLD_ID);
        String recipientId = (String) reqMap.get(JsonKeys.RECIPIENT_ID);
        Response response = CertificateUtil.getCertRecordByID(certId);
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.getResult().get(JsonKeys.RESPONSE);
        if(CollectionUtils.isNotEmpty(resultList)) {
            Map<String, Object> result = resultList.get(0);
            try {
                Map<String,Object> recipient = requestMapper.readValue((String)result.get(JsonKeys.RECIPIENT), Map.class);
                if(StringUtils.isNotBlank((String)recipient.get(JsonKeys.ID)) && !recipientId.equalsIgnoreCase((String)recipient.get(JsonKeys.ID))){
                    throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA,localizer.getMessage(IResponseMessage.INVALID_REQUESTED_DATA,null),ResponseCode.BAD_REQUEST.getCode());
                }
            }catch (IOException ex){
                throw new BaseException(IResponseMessage.SERVER_ERROR,getLocalizedMessage(IResponseMessage.SERVER_ERROR,null),ResponseCode.SERVER_ERROR.getCode());
            }
        }
    }

    private boolean isPresentRecipientIdAndCertId(Request request) {
        Map<String,Object> reqMap = request.getRequest();
        String certId = (String) reqMap.get(JsonKeys.OLD_ID);
        String recipientId = (String) reqMap.get(JsonKeys.RECIPIENT_ID);
        if(StringUtils.isNotBlank(certId) && StringUtils.isNotBlank(recipientId)){
            return true;
        }
        return false;
    }

    private void assureUniqueCertId(String certificatedId) throws BaseException {
        if (CertificateUtil.isIdPresent(certificatedId)) {
            logger.error(
                    "CertificateActor:addCertificate:provided certificateId exists in record:" + certificatedId);
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, getLocalizedMessage(IResponseMessage.ID_ALREADY_EXISTS,null), ResponseCode.CLIENT_ERROR.getCode());
        }
        logger.info("CertificateActor:addCertificate:successfully certId not found in records creating new record");
    }


    private Response processRecord(Map<String, Object> certReqAddMap, String version, ActorRef certBackgroundActorRef) throws BaseException {
        Certificate certificate=getCertificate(certReqAddMap);
        if(version.equalsIgnoreCase(JsonKeys.VERSION_1)) {
            certificate.setPdfUrl((String)certReqAddMap.get(JsonKeys.PDF_URL));
        }
        Map<String,Object>recordMap= requestMapper.convertValue(certificate,Map.class);
        return CertificateUtil.insertRecord(recordMap, certBackgroundActorRef);
    }
    private Certificate getCertificate(Map<String, Object> certReqAddMap) {
        Certificate certificate = new Certificate.Builder()
                .setId((String) certReqAddMap.get(JsonKeys.ID))
                .setData(getData(certReqAddMap))
                .setRevoked(false)
                .setAccessCode((String)certReqAddMap.get(JsonKeys.ACCESS_CODE))
                .setJsonUrl((String)certReqAddMap.get(JsonKeys.JSON_URL))
                .setRecipient(getCompositeReciepientObject(certReqAddMap))
                .setRelated((Map)certReqAddMap.get(JsonKeys.RELATED))
                .setReason((String)certReqAddMap.get(JsonKeys.REASON))
                .build();
        logger.info("CertsServiceImpl:getCertificate:certificate object formed.");
        return certificate;
    }
    private Recipient getCompositeReciepientObject(Map<String, Object> certAddRequestMap) {
        Recipient recipient = new Recipient.Builder()
                .setName((String) certAddRequestMap.get(JsonKeys.RECIPIENT_NAME))
                .setId((String) certAddRequestMap.get(JsonKeys.RECIPIENT_ID))
                .setType((String) certAddRequestMap.get(JsonKeys.RECIPIENT_TYPE))
                .build();
    return recipient;
    }

    private Map<String, Object> getData(Map<String, Object> certAddRequestMap) {
        return (Map) certAddRequestMap.get(JsonKeys.JSON_DATA);
    }

    @Override
    public Response validate(Request request) throws BaseException {
        Map<String,Object> valCertReq = request.getRequest();
        String certificatedId = (String) valCertReq.get(JsonKeys.CERT_ID);
        String accessCode = (String) valCertReq.get(JsonKeys.ACCESS_CODE);
        Response certResponse = CertificateUtil.getCertRecordByID(certificatedId);
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) certResponse.getResult().get(JsonKeys.RESPONSE);
        if (CollectionUtils.isNotEmpty(resultList) && MapUtils.isNotEmpty(resultList.get(0)) &&
                StringUtils.equalsIgnoreCase((String) resultList.get(0).get(JsonKeys.ACCESS_CODE), accessCode)) {
            Map<String, Object> result = resultList.get(0);
            Map<String,Object>responseMap=new HashMap<>();
            try {
                responseMap.put(JsonKeys.RELATED, requestMapper.readValue((String) result.get(JsonKeys.RELATED), new TypeReference<Map<String, Object>>(){}));
                responseMap.put(JsonKeys.JSON, requestMapper.readValue((String) result.get(JsonKeys.DATA), new TypeReference<Map<String, Object>>(){}));
            } catch (Exception e) {
                logger.error("CertsServiceImpl:validate:exception occurred:" + e);
                throw new BaseException(IResponseMessage.INTERNAL_ERROR, getLocalizedMessage(IResponseMessage.INTERNAL_ERROR, null), ResponseCode.SERVER_ERROR.getCode());
            }
            Response response=new Response();
            response.put(JsonKeys.RESPONSE,responseMap);
            return response;
        }
        else{
            logger.error("NO valid record found with provided certificate Id and accessCode respectively:"+certificatedId+":"+accessCode);
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.INVALID_ID_PROVIDED,null),certificatedId,accessCode), ResponseCode.CLIENT_ERROR.getCode());
        }

    }
    private Certificate getCertObject(Map<String,Object>esCertMap){
        Certificate certificate=requestMapper.convertValue(esCertMap,Certificate.class);
        return certificate;
    }


    @Override
    public Response download(Request request) throws BaseException {
        Response response = new Response();
        try {
            Map<String, Object> certReqMap = new HashMap<>();
            Map<String,String>requestMap=new HashMap<>();
            requestMap.put(JsonKeys.PDF_URL,(String)request.getRequest().get(JsonKeys.PDF_URL));
            certReqMap.put(JsonKeys.REQUEST,requestMap);
            String requestBody = requestMapper.writeValueAsString(certReqMap);
            logger.info("CertsServiceImpl:download:request body found.");
            String apiToCall = CertVars.getSERVICE_BASE_URL().concat(CertVars.getDOWNLOAD_URI());
            logger.info("CertsServiceImpl:download:complete url found:" + apiToCall);
            Future<HttpResponse<JsonNode>>responseFuture=CertificateUtil.makeAsyncPostCall(apiToCall,requestBody,headerMap);
            HttpResponse<JsonNode> jsonResponse = responseFuture.get();
            if (jsonResponse != null && jsonResponse.getStatus() == HttpStatus.SC_OK) {
                String signedUrl=jsonResponse.getBody().getObject().getJSONObject(JsonKeys.RESULT).getString(JsonKeys.SIGNED_URL);
                response.put(JsonKeys.SIGNED_URL,signedUrl);
            } else {
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.INVALID_PROVIDED_URL, null), (String) request.getRequest().get(JsonKeys.PDF_URL)), ResponseCode.CLIENT_ERROR.getCode());
            }

        } catch (Exception e) {
            logger.error("CertsServiceImpl:download:exception occurred:" + e);
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, getLocalizedMessage(IResponseMessage.INTERNAL_ERROR,null), ResponseCode.SERVER_ERROR.getCode());
        }
        return response;
    }

    @Override
    public Response downloadV2(Request request) throws BaseException {
        String certId = (String) request.getRequest().get(JsonKeys.ID);
        logger.info("CertServiceImpl:downloadV2:idProvided:" + certId);
        Response certData = CertificateUtil.getCertRecordByID(certId);
        Response response = new Response();
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) certData.getResult().get(JsonKeys.RESPONSE);
        String printUri;
        if (CollectionUtils.isNotEmpty(resultList) && MapUtils.isNotEmpty(resultList.get(0))) {
            Map<String, Object> certInfo = resultList.get(0);
            try {
                String jsonUrl = (String) certInfo.get(JsonKeys.JSON_URL);
                //in-some cases jsonUrl was not filled(1.5.0 prior to fix), After fix jsonUrl is being filled (because of svg content growth,now we are uploading cert to cloud)
                if (StringUtils.isEmpty(jsonUrl)) {
                    logger.info("getJsonSignedUrl: jsonUrl is empty , print uri is present in data object");
                    Map<String, Object> certificate = requestMapper.readValue((String) certInfo.get(JsonKeys.DATA), new TypeReference<Map<String, Object>>() {});
                    printUri = (String) certificate.get(JsonKeys.PRINT_URI);
                } else {
                    Request req = new Request();
                    req.put(JsonKeys.PDF_URL,certInfo.get(JsonKeys.JSON_URL));
                    logger.info("getJsonSignedUrl: getting signedUrl for the json url {}", certInfo.get(JsonKeys.JSON_URL));
                    Response downloadRes = download(req);
                    String signedJsonUrl = (String) downloadRes.getResult().get(JsonKeys.SIGNED_URL);
                    printUri = getPrintUri(signedJsonUrl);
                }
                response.put(JsonKeys.PRINT_URI, printUri);
            } catch (Exception e) {
                logger.error("CertsServiceImpl:downloadV2:exception occurred:" + e);
                throw new BaseException(IResponseMessage.INTERNAL_ERROR, getLocalizedMessage(IResponseMessage.INTERNAL_ERROR, null), ResponseCode.SERVER_ERROR.getCode());
            }
        } else {
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put(JsonKeys.ACCEPT, JsonKeys.APPLICATION_VC_LD_JSON);
            String rcTemplateApi = RegistryCredential.getSERVICE_BASE_URL().concat(RegistryCredential.getDOWNLOAD_URI())+"/"+certId;
            Future<HttpResponse<JsonNode>> rcResponseFuture=CertificateUtil.makeAsyncGetCall(rcTemplateApi,headerMap);
            try {
                HttpResponse<JsonNode> rcJsonResponse = rcResponseFuture.get();
                if (rcJsonResponse != null && rcJsonResponse.getStatus() == HttpStatus.SC_OK) {
                    String templateUrl = rcJsonResponse.getBody().getObject().getJSONObject(JsonKeys.RESULT).getString(JsonKeys.TEMPLATE_URL);
                    String rcDownloadApi = RegistryCredential.getSERVICE_BASE_URL().concat(RegistryCredential.getDOWNLOAD_URI()) + "/" + certId;
                    headerMap.put(JsonKeys.ACCEPT, JsonKeys.IMAGE_SVG_XML);
                    headerMap.put("templateurl", templateUrl);
                    Future<HttpResponse<String>> rcDownloadResFuture = CertificateUtil.makeAsyncGetCallString(rcDownloadApi, headerMap);
                    HttpResponse<String> rcDownloadJsonResponse = rcDownloadResFuture.get();
                    printUri = rcDownloadJsonResponse.getBody();
                    response.put(JsonKeys.PRINT_URI, printUri);
                } else {
                    throw new BaseException(IResponseMessage.RESOURCE_NOT_FOUND, localizer.getMessage(IResponseMessage.RESOURCE_NOT_FOUND, null), ResponseCode.RESOURCE_NOT_FOUND.getCode());
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new BaseException(IResponseMessage.RESOURCE_NOT_FOUND, localizer.getMessage(IResponseMessage.RESOURCE_NOT_FOUND, null), ResponseCode.RESOURCE_NOT_FOUND.getCode());
            }
        }
        
        return response;
    }

    private String getPrintUri(String signedJsonUrl) throws BaseException {
        String printUri;
        try {
            URL url = new URL(signedJsonUrl);
            String data = IOUtils.toString(url, StandardCharsets.UTF_8);
            Map<String, Object> certificate = requestMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
            printUri = (String) certificate.get(JsonKeys.PRINT_URI);
        } catch (IOException e) {
            logger.error("getPrintUri:exception occurred {} {}", e.getMessage(), e);
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, getLocalizedMessage(e.getLocalizedMessage(), null), ResponseCode.SERVER_ERROR.getCode());
        }
        return printUri;
    }

    @Override
    public Response generate(Request request) throws BaseException {
        Response response = new Response();
        try {
            Map<String, Object> certReqMap = new HashMap<>();
            certReqMap.put(JsonKeys.REQUEST,request.getRequest());
            String requestBody = requestMapper.writeValueAsString(certReqMap);
            logger.info("CertsServiceImpl:generate:request body found.");
            String apiToCall = CertVars.getSERVICE_BASE_URL().concat(CertVars.getGenerateUri());
            logger.info("CertsServiceImpl:generate:complete url found:" + apiToCall);
            Future<HttpResponse<JsonNode>>responseFuture=CertificateUtil.makeAsyncPostCall(apiToCall,requestBody,headerMap);
            HttpResponse<JsonNode> jsonResponse = responseFuture.get();
            if (jsonResponse != null && jsonResponse.getStatus() == HttpStatus.SC_OK) {
                String stringifyResponse=jsonResponse.getBody().getObject().getJSONObject(JsonKeys.RESULT).get(JsonKeys.RESPONSE).toString();
                List<Map<String,Object>> apiRespList=requestMapper.readValue(stringifyResponse,List.class);
                response.put(JsonKeys.RESPONSE,apiRespList);
            } else {
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.INVALID_PROVIDED_URL,null),"2222"), ResponseCode.CLIENT_ERROR.getCode());
            }

        } catch (Exception e) {
            logger.error("CertsServiceImpl:generate:exception occurred:" + e);
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, IResponseMessage.INTERNAL_ERROR, ResponseCode.SERVER_ERROR.getCode());
        }
        return response;
    }

    @Override
    public Response verify(Request request) throws BaseException {
        Response response = new Response();
        try {
            Map<String, Object> certVerifyReqMap = new HashMap<>();
            certVerifyReqMap.put(JsonKeys.REQUEST,composeCertVerifyRequest(request));
            String requestBody = requestMapper.writeValueAsString(certVerifyReqMap);
            logger.info("CertsServiceImpl:verify:request body prepared.");
            String apiToCall = CertVars.getSERVICE_BASE_URL().concat(CertVars.getVerifyUri());
            logger.info("CertsServiceImpl:verify:complete url prepared:" + apiToCall);
            Future<HttpResponse<JsonNode>>responseFuture=CertificateUtil.makeAsyncPostCall(apiToCall,requestBody,headerMap);
            HttpResponse<JsonNode> jsonResponse = responseFuture.get();
            if (jsonResponse != null && jsonResponse.getStatus() == HttpStatus.SC_OK) {
                String stringifyResponse=jsonResponse.getBody().getObject().getJSONObject(JsonKeys.RESULT).get(JsonKeys.RESPONSE).toString();
                Map<String,Object> apiResp=requestMapper.readValue(stringifyResponse,Map.class);
                response.put(JsonKeys.RESPONSE,apiResp);
            }
            else if(jsonResponse!=null && jsonResponse.getStatus() == HttpStatus.SC_BAD_REQUEST){
                String stringifyResponse=jsonResponse.getBody().getObject().getJSONObject(JsonKeys.RESULT).get(JsonKeys.MESSAGE).toString();
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA,stringifyResponse, ResponseCode.CLIENT_ERROR.getCode());
            }
            else {
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA,IResponseMessage.INVALID_REQUESTED_DATA, ResponseCode.CLIENT_ERROR.getCode());
            }

        } catch (Exception e) {
            logger.error("CertsServiceImpl:verify:exception occurred:" + e);
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, e.getMessage(), ResponseCode.SERVER_ERROR.getCode());
        }
        return response;
    }

    @Override
    public Response read(Request request) throws BaseException {
        String id=(String)request.getRequest().get(JsonKeys.ID);
        logger.info("CertServiceImpl:read:idProvided: {}",id);
        Response cassandraResponse = CertificateUtil.getCertRecordByID(id);
        Response response = new Response();
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) cassandraResponse.getResult().get(JsonKeys.RESPONSE);
        if (CollectionUtils.isNotEmpty(resultList) && MapUtils.isNotEmpty(resultList.get(0))) {
            Map<String, Object> certInfo = resultList.get(0);
            try {
                Map<String, Object> data = requestMapper.readValue((String) certInfo.get(JsonKeys.DATA), new TypeReference<Map<String, Object>>() {});
                Map<String, Object> recipient = requestMapper.readValue((String) certInfo.get(JsonKeys.RECIPIENT), new TypeReference<Map<String, Object>>() {});
                if (StringUtils.isNotEmpty((String) certInfo.get(JsonKeys.RELATED))) {
                    Map<String, Object> related = requestMapper.readValue((String) certInfo.get(JsonKeys.RELATED), new TypeReference<Map<String, Object>>() {});
                    certInfo.put(JsonKeys.RELATED, related);
                }
                certInfo.put(JsonKeys.DATA, data);
                certInfo.put(JsonKeys.RECIPIENT, recipient);
                response.put(JsonKeys.RESPONSE, certInfo);
            } catch (Exception e) {
                logger.error("CertsServiceImpl:read:exception occurred: {}", e.getMessage());
                throw new BaseException(IResponseMessage.INTERNAL_ERROR, getLocalizedMessage(IResponseMessage.INTERNAL_ERROR, null), ResponseCode.SERVER_ERROR.getCode());
            }
        } else {
            throw new BaseException(IResponseMessage.RESOURCE_NOT_FOUND, localizer.getMessage(IResponseMessage.RESOURCE_NOT_FOUND, null), ResponseCode.RESOURCE_NOT_FOUND.getCode());
        }
        return response;
    }

    private Map<String,Object> composeCertVerifyRequest(Request request){
        Map<String,Object>certificate=new HashMap<>();
        Map<String,Object>certVerifyMap=new HashMap<>();
        if(MapUtils.isNotEmpty((Map)request.getRequest().get(JsonKeys.DATA))) {
            certificate.put(JsonKeys.DATA, request.getRequest().get(JsonKeys.DATA));
        }
        if(StringUtils.isNotBlank((String)request.getRequest().get(JsonKeys.ID))) {
            certificate.put(JsonKeys.ID, request.getRequest().get(JsonKeys.ID));
        }
        certVerifyMap.put(JsonKeys.CERTIFICATE,certificate);
        return certVerifyMap;
    }

    private static String getLocalizedMessage(String key, Locale locale){
        return localizer.getMessage(key, locale);
    }


    @Override
    public Response search(Request request) throws BaseException{
        Response response = new Response();
        Map<String, String> headerMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        headerMap.put(JsonKeys.CONTENT_TYPE, JsonKeys.APPLICATION_JSON);
        try {
            ESResponseMapper mappedResponse = null;
            mappedResponse = searchEsPostCall(request);
            String rcSearchApiCall = RegistryCredential.getRCSearchUri();
            logger.info("RegistryCredential:rcSearchApiCall:complete url found:" + rcSearchApiCall);
            Map<String, Object> req = request.getRequest();
            ObjectNode jsonNode = mapper.convertValue(req, ObjectNode.class);
            ObjectNode jsonNode1 = (ObjectNode) jsonNode.at(JsonKeys.QUERY_MATCH_PHRASE);
            Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = jsonNode1.fields();
            Map<String, Object> filters = new HashMap<>();
            Map<String, Object> fieldKeyMap = new HashMap<>();
            fields.forEachRemaining(field -> {
                Map<String, Object> fieldValueMap = new HashMap<>();
                fieldValueMap.put("eq", field.getValue().asText());
                fieldKeyMap.put(field.getKey(), fieldValueMap);
            });
            filters.put(JsonKeys.FILTERS, fieldKeyMap);
            String filterString = mapper.writeValueAsString(filters);
            Future<HttpResponse<JsonNode>> rcResponseFuture = CertificateUtil.makeAsyncPostCall(rcSearchApiCall, filterString, headerMap);
            HttpResponse<JsonNode> rcJsonResponse = rcResponseFuture.get();
            if (rcJsonResponse != null && rcJsonResponse.getStatus() == HttpStatus.SC_OK) {
                String rcJsonArray = rcJsonResponse.getBody().getArray().toString();
                List<Map<String, Object>> rcSearchApiResp = requestMapper.readValue(rcJsonArray, List.class);
                if(mappedResponse != null) {
                    mappedResponse.setCount(mappedResponse.getCount() + rcSearchApiResp.size());
                    mappedResponse.getContent().addAll(rcSearchApiResp);
                } else {
                    mappedResponse = new ESResponseMapper();
                    mappedResponse.setCount(rcSearchApiResp.size());
                    mappedResponse.setContent(rcSearchApiResp);
                }
            } else {
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA,rcJsonResponse.getBody().toString(), ResponseCode.CLIENT_ERROR.getCode());
            }
            response.put(JsonKeys.RESPONSE, mappedResponse);
            
        } catch (Exception e) {
            logger.error("CertsServiceImpl:search:exception occurred:" + e);
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, IResponseMessage.INTERNAL_ERROR, ResponseCode.SERVER_ERROR.getCode());
        }
        return response;
    }
    
    @Override
    public Response searchV2(Request request) throws BaseException{
        Response response = new Response();
        ESResponseMapper mappedResponse = searchEsPostCall(request);
        response.put(JsonKeys.RESPONSE, mappedResponse);
        return response;
    }
    
    private ESResponseMapper searchEsPostCall(Request request) throws BaseException {
        ESResponseMapper mappedResponse = null;
        try {
            String requestBody = requestMapper.writeValueAsString(request.getRequest());
            logger.info("CertsServiceImpl:search:request body found.");
            String apiToCall = CertVars.getEsSearchUri();
            logger.info("CertsServiceImpl:search:complete url found:" + apiToCall);
            Future<HttpResponse<JsonNode>> responseFuture = CertificateUtil.makeAsyncPostCall(apiToCall, requestBody, headerMap);
            HttpResponse<JsonNode> jsonResponse = responseFuture.get();
            if (jsonResponse != null && jsonResponse.getStatus() == HttpStatus.SC_OK) {
                String jsonArray = jsonResponse.getBody().getObject().getJSONObject(JsonKeys.HITS).toString();
                Map<String, Object> apiResp = requestMapper.readValue(jsonArray, Map.class);
                mappedResponse = new ObjectMapper().convertValue(apiResp, ESResponseMapper.class);
            } else {
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, jsonResponse.getBody().toString(), ResponseCode.CLIENT_ERROR.getCode());
            }
        } catch (Exception e) {
            logger.error("CertsServiceImpl:search:exception occurred:" + e);
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, IResponseMessage.INTERNAL_ERROR, ResponseCode.SERVER_ERROR.getCode());
        }
        return mappedResponse;
    }
}