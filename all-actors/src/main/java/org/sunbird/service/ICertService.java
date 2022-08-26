package org.sunbird.service;


import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.sunbird.BaseException;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

import java.util.concurrent.ExecutionException;

/**
 * this is an interface class for implementing certificate related operations
 * @author anmolgupta
 *
 */
public interface ICertService{

    Response delete(Request request, ActorRef certBackgroundActorRef) throws BaseException;

    String add(Request request, ActorRef certBackgroundActorRef) throws BaseException;

    Response  validate(Request request) throws BaseException;

    Response download(Request request) throws BaseException;

    Response downloadV2(Request request) throws BaseException;

    Response generate(Request request) throws BaseException;

    Response verify(Request request) throws  BaseException;

    Response read(Request request) throws  BaseException;

    Response search(Request request) throws BaseException;
    
    Response searchV2(Request request) throws BaseException;

}
