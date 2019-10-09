package validators;

import org.sunbird.BaseException;
import org.sunbird.request.Request;

/**
 * this is an interface class for validating the request
 * @author anmolgupta
 */
public interface IRequestValidator {
    void validate(Request request) throws BaseException;

}
