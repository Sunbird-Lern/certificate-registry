package org.sunbird.actor;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.ActorOperations;
import org.sunbird.BaseException;
import org.sunbird.CertVars;
import org.sunbird.JsonKeys;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraDACImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.ICertService;
import org.sunbird.serviceimpl.CertsServiceImpl;
import org.sunbird.utilities.CertificateUtil;
import scala.concurrent.duration.Duration;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Localizer.class,
        ICertService.class,
        CertsServiceImpl.class,
        CertificateUtil.class,
        EsClientFactory.class,
        ServiceFactory.class,
        ElasticSearchRestHighImpl.class,
        ElasticSearchService.class,
        CassandraOperation.class,
        CassandraDACImpl.class,
        CertVars.class,
        IOUtils.class,
        URL.class})
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class CertificationActorTest {


    private static ActorSystem system = ActorSystem.create("system");
    private static final Props props = Props.create(CertificationActor.class);

    @BeforeClass
    public static void setUp() throws Exception {
    }

    JSONObject object2 = null;
    public void beforeTestSetUp() throws Exception {
        PowerMockito.mockStatic(CertVars.class);
        PowerMockito.mockStatic(EsClientFactory.class);
        ElasticSearchRestHighImpl elasticSearchRestHigh = PowerMockito.mock(ElasticSearchRestHighImpl.class);
        PowerMockito.whenNew(ElasticSearchRestHighImpl.class).withNoArguments().thenReturn(elasticSearchRestHigh);
        when(EsClientFactory.getInstance()).thenReturn(elasticSearchRestHigh);
        PowerMockito.mockStatic(ServiceFactory.class);
        PowerMockito.mock(CassandraOperation.class);
        CassandraDACImpl cassandraDAC = PowerMockito.mock(CassandraDACImpl.class);
        PowerMockito.whenNew(CassandraDACImpl.class).withNoArguments().thenReturn(cassandraDAC);
        PowerMockito.mockStatic(CertificateUtil.class);
        when(CertificateUtil.isIdPresent(Mockito.anyString())).thenReturn(false);
        CertsServiceImpl certsService = PowerMockito.mock(CertsServiceImpl.class);
        PowerMockito.whenNew(CertsServiceImpl.class).withNoArguments().thenReturn(certsService);
        when(certsService.add(Mockito.any(Request.class), Mockito.any(ActorRef.class))).thenReturn("id");

        when(certsService.validate(Mockito.any(Request.class))).thenReturn(getValidateCertResponse());
        Map<String,Object> map = new HashMap<>();
        map.put(JsonKeys.ACCESS_CODE,"access_code");
        when(CertificateUtil.getCertificate(Mockito.anyString())).thenReturn(map);
        when(CertVars.getSERVICE_BASE_URL()).thenReturn("service_base_url");
        when(CertVars.getDOWNLOAD_URI()).thenReturn("download_url");
        when(CertVars.getEsSearchUri()).thenReturn("es_search_uri");
        final Future<HttpResponse<JsonNode>> mockedFuture = Mockito.mock(Future.class);
        when(CertificateUtil.makeAsyncPostCall(Mockito.anyString(),Mockito.anyString(),Mockito.anyMap())).thenReturn(mockedFuture);
        final HttpResponse<JsonNode> mockedResponse = Mockito.mock(HttpResponse.class);
        when(mockedFuture.get()).thenReturn(mockedResponse);
        when(mockedResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        final JsonNode node = Mockito.mock(JsonNode.class);
        when(mockedResponse.getBody()).thenReturn(node);
        final JSONObject object = Mockito.mock(JSONObject.class);
        when(node.getObject()).thenReturn(object);
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("value","value1");
        when(object.getJSONObject(JsonKeys.HITS)).thenReturn(jsonObject);
        object2 = Mockito.mock(JSONObject.class);
        when(object.getJSONObject(JsonKeys.RESULT)).thenReturn(object2);
        String signedUrl ="http://localhost:9000/dev-e-credentials/0125450863553740809/4d88c2e4-212b-4c00-aa83-1cd3fde7b447.json";
        when(object2.getString(JsonKeys.SIGNED_URL)).thenReturn(signedUrl);
        when(object2.get(JsonKeys.RESPONSE)).thenReturn(map);
    
        final Future<HttpResponse<JsonNode>> mockedGetFuture = Mockito.mock(Future.class);
        when(CertificateUtil.makeAsyncGetCall(Mockito.anyString(),Mockito.anyMap())).thenReturn(mockedGetFuture);
        when(mockedGetFuture.get()).thenReturn(null);
        when(certsService.download(Mockito.any(Request.class))).thenReturn(getValidateCertResponse());
        when(CertificateUtil.getCertificate(Mockito.anyString())).thenReturn(map);

        when(CertVars.getGenerateUri()).thenReturn("generate_uri");
        when(certsService.generate(Mockito.any(Request.class))).thenReturn(getValidateCertResponse());
        when(CertificateUtil.getCertificate(Mockito.anyString())).thenReturn(map);
        when(CertVars.getVerifyUri()).thenReturn("verify_uri");
        when(certsService.verify(Mockito.any(Request.class))).thenReturn(getValidateCertResponse());
        when(CertificateUtil.getCertificate(Mockito.anyString())).thenReturn(map);
        when(CertificateUtil.getCertRecordByID(Mockito.anyString())).thenReturn(getCertReadResponse());
        URL mockedURL = PowerMockito.mock(URL.class);
        PowerMockito.whenNew(URL.class).withArguments(signedUrl).thenReturn(mockedURL);
        PowerMockito.mockStatic(IOUtils.class);
        when(IOUtils.toString(mockedURL, StandardCharsets.UTF_8)).thenReturn("{\"id\":\"http://localhost:8080/_schemas/Certificate/d5a28280-98ac-4294-a508-21075dc7d475\",\"type\":[\"Assertion\",\"Extension\",\"extensions:CertificateExtension\"],\"issuedOn\":\"2019-08-31T12:52:25Z\",\"recipient\":{\"identity\":\"ntptest103\",\"type\":[\"phone\"],\"hashed\":false,\"name\":\"Aishwarya\",\"@context\":\"http://localhost:8080/_schemas/context.json\"},\"badge\":{\"id\":\"http://localhost:8080/_schemas/Badge.json\",\"type\":[\"BadgeClass\"],\"name\":\"Sunbird installation\",\"description\":\"Certificate of Appreciation in National Level ITI Grading\",\"image\":\"https://certs.example.gov/o/dgt/HJ5327VB1247G\",\"criteria\":{\"type\":[\"Criteria\"],\"id\":\"http://localhost:8080/_schemas/Certificate/d5a28280-98ac-4294-a508-21075dc7d475\",\"narrative\":\"For exhibiting outstanding performance\"},\"issuer\":{\"context\":\"http://localhost:8080/_schemas/context.json\",\"id\":\"http://localhost:8080/_schemas/Issuer.json\",\"type\":[\"Issuer\"],\"name\":\"NIIT\"},\"@context\":\"http://localhost:8080/_schemas/context.json\"},\"expires\":\"2019-09-30T12:52:25Z\",\"verification\":{\"type\":[\"SignedBadge\"],\"creator\":\"http://localhost:8080/_schemas/publicKey.json\"},\"revoked\":false,\"validFrom\":\"2019-06-21\",\"@context\":\"http://localhost:8080/_schemas/context.json\",\"printUri\":\"data:image/svg+xml,%3Csvg ....\"}");
    }

    public static void tearDown() throws Exception {
        Duration duration = Duration.create(10L, TimeUnit.SECONDS);
        TestKit.shutdownActorSystem(system, duration, true);
        system = null;
    }

    @Test
    public void addCertificate() throws Exception {
        Request request = createAddCertRequest();
        request.setOperation(ActorOperations.ADD.getOperation());
        beforeTestSetUp();
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        Response res = testKit.expectMsgClass(Duration.create(10, TimeUnit.SECONDS),Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }

    @Test
    public void validateCertificate() throws Exception {

        Request request = createValidateCertRequest();
        request.setOperation(ActorOperations.VALIDATE.getOperation());
        beforeTestSetUp();
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        Response res = testKit.expectMsgClass(Duration.create(10, TimeUnit.SECONDS),Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }

    @Test
    public void downloadCertificate() throws Exception {

        Request request = createDownloadCertRequest();
        request.setOperation(ActorOperations.DOWNLOAD.getOperation());
        beforeTestSetUp();
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        Response res = testKit.expectMsgClass(Duration.create(1000, TimeUnit.SECONDS),Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }

    @Test
    public void testReadCertificate() throws Exception {
        Request request = createCertReadRequest();
        request.setOperation(ActorOperations.READ.getOperation());
        beforeTestSetUp();
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        Response res = testKit.expectMsgClass(Duration.create(1000, TimeUnit.SECONDS),Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }

    @Test
    public void testSearchCertificate() throws Exception {
        Request request = createDownloadCertRequest();
        request.setOperation(ActorOperations.SEARCHV2.getOperation());
        beforeTestSetUp();
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        Response res = testKit.expectMsgClass(Duration.create(1000, TimeUnit.SECONDS), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }


  //  @Test
    public void testVerifyCertificate() throws Exception {
        Request request = createVerifyCertRequest();
        request.setOperation(ActorOperations.VERIFY.getOperation());
        beforeTestSetUp();
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        Response res = testKit.expectMsgClass(Duration.create(1000, TimeUnit.SECONDS), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }

    @Test
    public void testDownloadV2Certificate() throws Exception {
        Request request = createDownloadV2CertRequest();
        request.setOperation(ActorOperations.DOWNLOADV2.getOperation());
        beforeTestSetUp();
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        Response res = testKit.expectMsgClass(Duration.create(1000, TimeUnit.SECONDS),Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }

    @Test
    public void testDownloadV2CertificateFailure() throws Exception {
        Request request = createDownloadV2CertRequest();
        request.setOperation(ActorOperations.DOWNLOADV2.getOperation());
        beforeTestSetUp();
        Response readRes = new Response();
        readRes.getResult().put(JsonKeys.RESPONSE, new ArrayList<>());
        when(CertificateUtil.getCertRecordByID(Mockito.anyString())).thenReturn(readRes);
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        BaseException res = testKit.expectMsgClass(Duration.create(1000, TimeUnit.SECONDS),BaseException.class);
        Assert.assertTrue(null != res);
    }

    private Response getCertReadResponse() {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(JsonKeys.DATA, "{\"id\":\"http://localhost:8080/_schemas/Certificate/d5a28280-98ac-4294-a508-21075dc7d475\",\"type\":[\"Assertion\",\"Extension\",\"extensions:CertificateExtension\"],\"issuedOn\":\"2019-08-31T12:52:25Z\",\"recipient\":{\"identity\":\"ntptest103\",\"type\":[\"phone\"],\"hashed\":false,\"name\":\"Aishwarya\",\"@context\":\"http://localhost:8080/_schemas/context.json\"},\"badge\":{\"id\":\"http://localhost:8080/_schemas/Badge.json\",\"type\":[\"BadgeClass\"],\"name\":\"Sunbird installation\",\"description\":\"Certificate of Appreciation in National Level ITI Grading\",\"image\":\"https://certs.example.gov/o/dgt/HJ5327VB1247G\",\"criteria\":{\"type\":[\"Criteria\"],\"id\":\"http://localhost:8080/_schemas/Certificate/d5a28280-98ac-4294-a508-21075dc7d475\",\"narrative\":\"For exhibiting outstanding performance\"},\"issuer\":{\"context\":\"http://localhost:8080/_schemas/context.json\",\"id\":\"http://localhost:8080/_schemas/Issuer.json\",\"type\":[\"Issuer\"],\"name\":\"NIIT\"},\"@context\":\"http://localhost:8080/_schemas/context.json\"},\"expires\":\"2019-09-30T12:52:25Z\",\"verification\":{\"type\":[\"SignedBadge\"],\"creator\":\"http://localhost:8080/_schemas/publicKey.json\"},\"revoked\":false,\"validFrom\":\"2019-06-21\",\"@context\":\"http://localhost:8080/_schemas/context.json\"}");
        responseMap.put(JsonKeys.RELATED, "{\"type\":\"course completion certificate prad\",\"batchId\":\"0130589602973368326\",\"courseId\":\"do_11305895730108006411643\"}");
        responseMap.put(JsonKeys.RECIPIENT, "{\"name\":\"Test user\",\"email\":null,\"phone\":null,\"id\":null,\"type\":\"individual\"}");
        responseMap.put(JsonKeys.ACCESS_CODE, "access_code");
        responseMap.put(JsonKeys.JSON_URL, "http://localhost:9000/certs/0125450863553740809/0ec760f3-a82e-4d3a-b97b-7be2e3a8298f.json");
        Response response = new Response();
        response.put(JsonKeys.RESPONSE, Lists.newArrayList(responseMap));
        return response;
    }

    @Test
    public void testReadCertificateMetaDataInvalidCertId() throws Exception {
        Request request = createCertReadRequest();
        request.setOperation(ActorOperations.READ_CERT_META_DATA.getOperation());
        beforeTestSetUp();
        Response readRes = new Response();
        readRes.getResult().put(JsonKeys.RESPONSE, new ArrayList<>());
        when(CertificateUtil.getCertRecordByID(Mockito.anyString())).thenReturn(readRes);
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = system.actorOf(props);
        actorRef.tell(request, testKit.getRef());
        BaseException res = testKit.expectMsgClass(Duration.create(1000, TimeUnit.SECONDS),BaseException.class);
        Assert.assertTrue(null != res);
    }

    private Request createVerifyCertRequest() {
        Request request = new Request();
        request.getRequest().put(JsonKeys.ID,"some_id");
        request.getRequest().put(JsonKeys.DATA,new HashMap<>());
        request.setOperation(ActorOperations.VERIFY.getOperation());
        return request;
    }

    private Response getValidateCertResponse(){
        Map<String,Object>responseMap=new HashMap<>();
        responseMap.put(JsonKeys.JSON,new HashMap<>());
        responseMap.put(JsonKeys.RELATED,new HashMap<>());
        Response response=new Response();
        response.put(JsonKeys.RESPONSE,responseMap);
        return response;
    }
    private Request createDownloadCertRequest() {
        Request reqObj = new Request();
        Map<String,Object> reqMap = new HashMap<>();
        reqMap.put(JsonKeys.PDF_URL,"pdf_url");
        reqObj.getRequest().putAll(reqMap);
        return reqObj;
    }

    private Request createDownloadV2CertRequest() {
        Request reqObj = new Request();
        Map<String,Object> reqMap = new HashMap<>();
        reqMap.put(JsonKeys.ID,"anyMockId");
        reqObj.getRequest().putAll(reqMap);
        return reqObj;
    }

    private Request createCertReadRequest() {
        Request reqObj = new Request();
        Map<String,Object> reqMap = new HashMap<>();
        reqMap.put(JsonKeys.ID,"anyMockId");
        reqObj.getRequest().putAll(reqMap);
        return reqObj;
    }
    private Request createValidateCertRequest() {
        Request reqObj = new Request();
        Map<String,Object> reqMap = new HashMap<>();
        reqMap.put(JsonKeys.CERT_ID,"certId");
        reqMap.put(JsonKeys.ACCESS_CODE,"access_code");
        reqObj.getRequest().putAll(reqMap);
        return reqObj;
    }
    private Request createAddCertRequest() {
        Request reqObj = new Request();
        Map<String,Object> reqMap = new HashMap<>();
        Map<String,Object> innerMap = new HashMap<>();
        innerMap.put("recipientId", "b69b056d-1d7d-47a9-a3b6-584fce840fd3");
        innerMap.put("recipientName", "Akash Kumar");
        innerMap.put("id", "id5dd4");
        innerMap.put("accessCode", "EANAB13");
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
        Map<String, Object> context = new HashMap<>();
        context.put(JsonKeys.VERSION, JsonKeys.VERSION_1);
        reqMap.put("request",innerMap);
        reqObj.setContext(context);
        reqObj.getRequest().putAll(reqMap);
        return reqObj;
    }
}
