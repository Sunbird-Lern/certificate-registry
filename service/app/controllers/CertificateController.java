package controllers;

import akka.actor.ActorRef;
import org.sunbird.JsonKeys;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import validators.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * this controller will help you in understanding the process of passing request to Actors with operation.
 * @author anmolgupta
 */
public class CertificateController extends BaseController {


    @Inject
    @Named("certification_actor")
    private ActorRef certificationActorRef;

    /**
     * this action method will be called for adding certificate
     * @return CompletionStage of Result
     */
    public CompletionStage<Result> add(Http.Request httpRequest)
    {
        IRequestValidator requestValidator=new CertAddRequestValidator();
        return handleRequest(certificationActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    Map<String, Object> context = new HashMap<>();
                    context.put(JsonKeys.VERSION, JsonKeys.VERSION_1);
                    req.setContext(context);
                    requestValidator.validate(req);
                    return null;
                }, JsonKeys.CERT_ADD);
    }

    /**
     * this action method will be called for adding certificate
     * @return CompletionStage of Result
     */
    public CompletionStage<Result> addV2(Http.Request httpRequest)
    {
        IRequestValidator requestValidator=new CertAddRequestValidator();
        return handleRequest(certificationActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    Map<String, Object> context = new HashMap<>();
                    context.put(JsonKeys.VERSION, JsonKeys.VERSION_2);
                    req.setContext(context);
                    requestValidator.validate(req);
                    return null;
                }, JsonKeys.CERT_ADD_V2);
    }

    /**
     * this action method will be called for verifying certificate
     * @return CompletionStage of Result
     */
    public CompletionStage<Result> validate(Http.Request httpRequest)
    {
        IRequestValidator requestValidator=new CertValidateRequestValidator();
        return handleRequest(certificationActorRef, httpRequest,
            request -> {
                Request req = (Request) request;
                requestValidator.validate(req);
                return null;
            }, JsonKeys.CERT_VALIDATE);
    }

    /**
     * this action method will be called for downloading certificate
     * @return CompletionStage of Result
     */
    public CompletionStage<Result> download(Http.Request httpRequest)
    {
        IRequestValidator requestValidator=new CertDownloadRequestValidator();
        return handleRequest(certificationActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    requestValidator.validate(req);
                    return null;
                },  JsonKeys.CERT_DOWNLOAD);
    }


    /**
     * this action method will be called for downloading certificate
     *
     * @return CompletionStage of Result
     */
    public CompletionStage<Result> downloadV2(String id, Http.Request httpRequest) {
        IRequestValidator requestValidator = new CertDownloadV2RequestValidator();
        return handleRequest(certificationActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    req.getRequest().put(JsonKeys.ID, id);
                    requestValidator.validate(req);
                    return null;
                }, JsonKeys.CERT_DOWNLOAD_V2);
    }

    /**
     * this action method will be called for verify certificate
     * @return CompletionStage of Result
     */
    public CompletionStage<Result> verify(Http.Request httpRequest)
    {
        IRequestValidator requestValidator=new CertVerifyRequestValidator();
        return handleRequest(certificationActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    requestValidator.validate(req);
                    return null;
                }, JsonKeys.CERT_VERIFY);
    }

    /**
     * this action method will be called for reading certificate with id
     * @return CompletionStage of Result
     */
    public CompletionStage<Result> read(String id, Http.Request httpRequest) {
        IRequestValidator requestValidator = new CertReadRequestValidator();
        return handleRequest(certificationActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    req.getRequest().put(JsonKeys.ID, id);
                    requestValidator.validate(req);
                    return null;
                }, JsonKeys.READ);


    }


    public CompletionStage<Result> search(Http.Request httpRequest)
    {
        IRequestValidator requestValidator=new CertSearchRequestValidator();
        return handleRequest(certificationActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    requestValidator.validate(req);
                    return null;
                }, JsonKeys.SEARCH);
    }
  
  public CompletionStage<Result> searchV2(Http.Request httpRequest)
  {
    IRequestValidator requestValidator=new CertSearchRequestValidator();
    return handleRequest(certificationActorRef, httpRequest,
      request -> {
        Request req = (Request) request;
        requestValidator.validate(req);
        return null;
      }, JsonKeys.SEARCH_V2);
  }

}
