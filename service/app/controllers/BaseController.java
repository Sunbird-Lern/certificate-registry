package controllers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

import akka.actor.ActorRef;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import utils.RequestMapper;
import utils.RequestValidatorFunction;
import utils.module.SignalHandler;


/**
 * This controller we can use for writing some common method to handel api
 * request. CompletableFuture: A Future that may be explicitly completed
 * (setting its value and status), and may be used as a CompletionStage,
 * supporting dependent functions and actions that trigger upon its completion.
 * CompletionStage: A stage of a possibly asynchronous computation, that
 * performs an action or computes a value when another CompletionStage completes
 *
 * @author Anmol
 */
public class BaseController extends Controller {
    Logger logger = LoggerFactory.getLogger(BaseController.class);

    @Inject
    SignalHandler signalHandler;

    /**
     * We injected HttpExecutionContext to decrease the response time of APIs.
     */
    @Inject
    protected HttpExecutionContext httpExecutionContext;
    protected final static Localizer locale = Localizer.getInstance();
    public static final String RESPONSE = "Response";
    public static final String SUCCESS = "Success";

    public CompletionStage<Result> handleRequest(play.mvc.Http.Request req) {
        try {
            handleSigTerm();
            startTrace("handelRequest");
            CompletableFuture<JsonNode> future = new CompletableFuture<>();
            Response response = new Response();
            response.put(RESPONSE, SUCCESS);
            future.complete(Json.toJson(response));
            endTrace("handelRequest");
            return future.thenApplyAsync(Results::ok, httpExecutionContext.current());
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> {
                return RequestHandler.handleFailureResponse(e, req);
            });
        }
    }

    public void startTrace(String tag) {
        logger.info("Method call started.");
    }

    /**
     * This method we used to print the logs of ending time of methods
     *
     * @param tag
     */
    public void endTrace(String tag) {
        logger.info("Method call ended.");
    }

    /**
     * this method will take play.mv.http request and a validation function and
     * lastly operation(Actor operation) this method is validating the request and ,
     * it will map the request to our sunbird Request class and make a call to
     * requestHandler which is internally calling ask to actor this method is used
     * to handle all the request type which has requestBody
     *
     * @param req
     * @param validatorFunction
     * @param operation
     * @return
     */
    public CompletionStage<Result> handleRequest(ActorRef actorRef, play.mvc.Http.Request req, RequestValidatorFunction validatorFunction,
                                                 String operation) {
        try {
            Request request = new Request();
            if (req.body() != null && req.body().asJson() != null) {
                request = (Request) RequestMapper.mapRequest(req, Request.class);
            }
            if (req.getHeaders() != null && request.getHeaders() != null) {
                Map<String, Object> map = getAllRequestHeaders(req);
                request.setHeaders(map);
            }
            if (validatorFunction != null) {
                validatorFunction.apply(request);
            }
            return new RequestHandler().handleRequest(request, actorRef, operation, req);
        } catch (Exception ex) {
            return CompletableFuture.supplyAsync(() -> {
                return RequestHandler.handleFailureResponse(ex,req);
            });
        }}

    private void handleSigTerm() throws BaseException {
        if (signalHandler.isShuttingDown()) {
            logger.info(
                    "SIGTERM is "
                            + signalHandler.isShuttingDown()
                            + ", So play server will not allow any new request.");
            throw new BaseException(IResponseMessage.SERVICE_UNAVAILABLE, IResponseMessage.SERVICE_UNAVAILABLE, ResponseCode.SERVICE_UNAVAILABLE.getCode());
        }
    }
    
    public Map<String, Object> getAllRequestHeaders(Http.Request request) {
        Map<String, Object> map = new HashMap<>();
        Map<String, List<String>> headers = request.getHeaders().toMap();
        Iterator<Map.Entry<String, List<String>>> itr = headers.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, List<String>> entry = itr.next();
            map.put(entry.getKey(), entry.getValue().get(0));
        }
        return map;
    }
}
