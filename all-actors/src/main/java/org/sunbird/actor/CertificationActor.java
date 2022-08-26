package org.sunbird.actor;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.sunbird.BaseActor;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.ICertService;
import org.sunbird.serviceimpl.CertsServiceImpl;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ExecutionException;

public class CertificationActor extends BaseActor {
    private ICertService certService = getCertServiceImpl();

    @Inject
    @Named("certificate_background_actor")
    private ActorRef certBackgroundActorRef;


    private ICertService getCertServiceImpl(){
        return new CertsServiceImpl();
    }

    @Override
    public void onReceive(Request request) throws BaseException, InterruptedException, ExecutionException, JsonProcessingException {
        logger.info("CertificationActor:onReceive:request arrived with operation" + request.getOperation());
        String operation = request.getOperation();
        switch (operation) {
            case "add" :
                add(request);
                break;
            case "addV2" :
                add(request);
                break;
            case  "validate" :
                validate(request);
                break;
            case "download" :
                download(request);
                break;
            case "generate" :
                generate(request);
                break;
            case "verify" :
                verify(request);
                break;
            case "read" :
                read(request);
                break;
            case "search":
                search(request);
                break;
            case "downloadV2" :
                downloadV2(request);
                break;
            case "searchV2":
                searchV2(request);
                break;
            default:
                onReceiveUnsupportedMessage("CertificationActor");
        }
    }

    private void add(Request request) throws BaseException {
        String id = certService.add(request, certBackgroundActorRef);
        Response response = new Response();
        response.put(JsonKeys.ID, id);
        sender().tell(response, self());
    }

    private void validate(Request request) throws BaseException {
        sender().tell(certService.validate(request), self());
    }

    private void download(Request request) throws BaseException
    {
        sender().tell(certService.download(request),self());

    }

    private void generate(Request request) throws BaseException {
        sender().tell(certService.generate(request),self());
    }

    private void verify(Request request) throws BaseException{
        sender().tell(certService.verify(request),self());
    }

    private void read(Request request) throws BaseException {
        sender().tell(certService.read(request), self());
    }
    private void search(Request request) throws BaseException{
        sender().tell(certService.search(request),self());
    }
    
    private void searchV2(Request request) throws BaseException{
        sender().tell(certService.searchV2(request),self());
    }

    private void downloadV2(Request request) throws BaseException {
        sender().tell(certService.downloadV2(request), self());
    }
}
