package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import play.Application;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import utils.module.ACTOR_NAMES;
import utils.module.StartModule;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static play.inject.Bindings.bind;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*", "jdk.internal.reflect.*"})
public abstract class BaseApplicationTest {
  protected Application application;

  public <T> void setup(Class<T> actorClass, ACTOR_NAMES actor) {
    try {
      application =
              new GuiceApplicationBuilder()
                      .in(new File("path/to/app"))
                      .in(Mode.TEST)
                      .disable(StartModule.class)
//                      .disable(ActorStartModule.class)
                      .overrides(bind(actor.getActorClass()).to(actorClass))
                      .build();
      Helpers.start(application);
    } catch (Exception e) {
    }
  }

  public <T> void setup(List<ACTOR_NAMES> actors, Class actorClass) {
    GuiceApplicationBuilder applicationBuilder =
            new GuiceApplicationBuilder()
                    .in(new File("path/to/app"))
                    .in(Mode.TEST)
                    .disable(StartModule.class);
//                    .disable(ActorStartModule.class);
    for (ACTOR_NAMES actor : actors) {
      applicationBuilder = applicationBuilder.overrides(bind(actor.getActorClass()).to(actorClass));
    }
    application = applicationBuilder.build();
    Helpers.start(application);
  }

  public String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();
    try {
      Response response = mapper.readValue(responseStr, Response.class);
      if (response != null) {
        ResponseParams params = response.getParams();
        return params.getStatus();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public int getResponseStatus(Result result) {
    return result.status();
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
