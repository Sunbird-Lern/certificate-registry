package controllers.health;

import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import play.mvc.Http;
import play.mvc.Result;

/**
 * This controller class will responsible to check health of the services.
 *
 * @author Anmol
 */
public class HealthController extends BaseController {

  /**
   * This action method is responsible for checking Health.
   *
   * @return a CompletableFuture of success response
   */
  public CompletionStage<Result> getHealth(Http.Request httpRequest) {
    CompletionStage<Result> response = handleRequest(httpRequest);
    return response;
  }
  /**
   * This action method is responsible for checking Health.
   *
   * @return a CompletableFuture of success response
   */
  public CompletionStage<Result> getServiceHealth(String service, Http.Request httpRequest) {
    CompletionStage<Result> response = handleRequest(httpRequest);
    return response;
  }

}
