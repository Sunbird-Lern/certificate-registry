package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.JsonKeys;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import utils.module.ACTOR_NAMES;
import utils.module.OnRequestHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@PrepareForTest({OnRequestHandler.class,BaseController.class})
public class CertificateControllerTest extends BaseApplicationTest {
    @Before
    public void before() {
        setup(Arrays.asList(ACTOR_NAMES.CERTIFICATION_ACTOR),DummyActor.class);
    }

    @Test
    public void testAddCertificateV1Success() {
        Result result =
                performTest(
                        "/certs/v1/registry/add",
                        "POST",
                        createCertRequest(false));
        assertEquals(HttpStatus.SC_OK, getResponseStatus(result));
    }

    @Test
    public void testAddCertificateV1Failure() {
        Result result =
                performTest(
                        "/certs/v1/registry/add",
                        "POST",
                        createCertRequest(true));
        assertEquals(HttpStatus.SC_BAD_REQUEST, getResponseStatus(result));
    }

   @Test
    public void testAddCertificateSuccess() {
        Result result =
                performTest(
                        "/certs/v2/registry/add",
                        "POST",
                        createCertRequest(false));
        assertEquals(HttpStatus.SC_OK, getResponseStatus(result));
    }

    @Test
    public void testAddCertificateFailure() {
        Result result =
                performTest(
                        "/certs/v2/registry/add",
                        "POST",
                        createCertRequest(true));
        assertEquals(HttpStatus.SC_BAD_REQUEST, getResponseStatus(result));
    }

    @Test
    public void testDownloadCertificateSuccess() {
        Result result =
                performTest(
                        "/certs/v1/registry/download",
                        "POST",
                        getCertDownloadReq(false));
        assertEquals(HttpStatus.SC_OK, getResponseStatus(result));
    }

    @Test
    public void testDownloadCertificateFailure() {
        Result result =
                performTest(
                        "/certs/v1/registry/download",
                        "POST",
                        getCertDownloadReq(true));
        assertEquals(HttpStatus.SC_BAD_REQUEST, getResponseStatus(result));
    }

    @Test
    public void testValidateCertificateSuccess() {
        Result result =
                performTest(
                        "/certs/v1/registry/validate",
                        "POST",
                        getCertValidateMap(false));
        assertEquals(HttpStatus.SC_OK, getResponseStatus(result));
    }

    @Test
    public void testSearchCertificateSuccess() {
        Result result =
                performTest(
                        "/certs/v1/registry/search",
                        "POST",
                        getCertValidateMap(false));
        assertEquals(HttpStatus.SC_OK,getResponseStatus(result));
    }


    @Test
    public void testValidateCertificateFailure() {
        Result result =
                performTest(
                        "/certs/v1/registry/validate",
                        "POST",
                        getCertValidateMap(true));
        assertEquals(HttpStatus.SC_BAD_REQUEST, getResponseStatus(result));
    }

    @Test
    public void testVerifyCertificateFailure() {
        Result result =
                performTest(
                        "/certs/v1/registry/verify",
                        "POST",
                        getCertVerifyMap(true));
        assertEquals(HttpStatus.SC_BAD_REQUEST, getResponseStatus(result));
    }

    @Test
    public void testReadCertificateSuccess() {
        Result result =
                performTest(
                        "/certs/v1/registry/read/123",
                        "GET",
                        null);
        assertEquals(HttpStatus.SC_OK, getResponseStatus(result));
    }

    @Test
    public void testDownloadV2CertificateSuccess() {
        Result result =
                performTest(
                        "/certs/v2/registry/download/123",
                        "GET",
                        null);
        assertEquals(HttpStatus.SC_OK, getResponseStatus(result));
    }

    @Test
    public void testVerifyCertificateSuccess() {
        Result result =
                performTest(
                        "/certs/v1/registry/verify",
                        "POST",
                        getCertVerifyMap(false));
        assertEquals(HttpStatus.SC_OK, getResponseStatus(result));
    }


    private Map<String,Object> createCertRequest(boolean isBad) {
        Map<String,Object> reqMap = new HashMap<>();
        Map<String,Object> innerMap = new HashMap<>();
        innerMap.put("recipientId", "b69b056d-1d7d-47a9-a3b6-584fce840fd3");
        innerMap.put("recipientName", "Akash Kumar");
        if(!isBad) {
            innerMap.put("id", "id5dd4");
        }
        innerMap.put("accessCode", "EANAB13");
        innerMap.put("jsonUrl", "http://localhost:9000/certs/0125450863553740809/d7707145-68bc-4005-8b05-d72c959e7886.json  dfrte fits");
        Map<String,Object> jsonData = new HashMap<>();
        jsonData.put("id", "http://localhost:8080/_schemas/Certificate/d5a28280-98ac-4294-a508-21075dc7d475");
        jsonData.put("type",new String[]{"Assertion","Extension"});
        jsonData.put("issuedOn", "2019-08-31T12:52:25Z");
        Map<String,Object> recipient = new HashMap<>();
        recipient.put("identity", "ntptest103");
        recipient.put("name", "Aishwarya");
        jsonData.put("recipient", recipient);
        Map<String,Object> badge = new HashMap<>();
        badge.put("id", "http://localhost:8080/_schemas/Badge.json");
        badge.put("name", "Sunbird installation");
        badge.put("description", "Certificate of Appreciation in National Level ITI Grading");
        jsonData.put("badge",badge);
        innerMap.put("pdfUrl", "djdjd/r01284093466818969624/275acee4-1964-4986-9dc3-06e08e9a1aa0.pdf");
        Map<String,Object> related = new HashMap<>();
        related.put("courseId", "course-idsd");
        related.put("type", "hell");
        related.put("batchId", "batchId");
        related.put("introUrl", "intro url");
        related.put("completionUrl", "completionUrl");
        innerMap.put("related", related);
        innerMap.put("jsonData", jsonData);
        reqMap.put("request",innerMap);
        return reqMap;
    }

    private Map<String,Object> getCertDownloadReq(boolean isBad){
        Map<String,Object>reqMap = new HashMap<>();
        Map<String,Object>request = new HashMap<>();
        if(!isBad) {
            request.put(JsonKeys.PDF_URL, "ANYpDFuR;");
        }
        reqMap.put(JsonKeys.REQUEST,request);
        return reqMap;
    }


    private Map<String,Object> getCertValidateMap(boolean isBad){
        Map<String,Object>reqMap = new HashMap<>();
        Map<String,Object>request = new HashMap<>();
        if(!isBad) {
            request.put(JsonKeys.CERT_ID, "anyCertId");
            request.put(JsonKeys.ACCESS_CODE,"anyAccessCode");
        }
        reqMap.put(JsonKeys.REQUEST,request);
        return reqMap;
    }

    private Map<String,Object> getCertVerifyMap(boolean isBad){
        Map<String,Object>reqMap = new HashMap<>();
        Map<String,Object>request = new HashMap<>();
        if(!isBad) {
            request.put(JsonKeys.ID, "anyCertId;");
        }
        reqMap.put(JsonKeys.REQUEST,request);
        return reqMap;
    }



    public Result performTest(String url, String method, Map map) {
        String data = mapToJson(map);
        Http.RequestBuilder req;
        if (StringUtils.isNotBlank(data)) {
            JsonNode json = Json.parse(data);
            req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
        } else {
            req = new Http.RequestBuilder().uri(url).method(method);
        }
        Result result = Helpers.route(application, req);
        return result;
    }

    public String mapToJson(Map map) {
        ObjectMapper mapperObj = new ObjectMapper();
        String jsonResp = "";

        if (map != null) {
            try {
                jsonResp = mapperObj.writeValueAsString(map);
            } catch (IOException e) {
            }
        }
        return jsonResp;
    }

}
