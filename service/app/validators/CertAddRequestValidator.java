package validators;

import com.google.common.collect.Lists;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import scala.jdk.javaapi.CollectionConverters;

/**
 * this is a validator class for adding certificates
 * @author anmolgupta
 */
public class CertAddRequestValidator implements IRequestValidator {
    static Logger logger= LoggerFactory.getLogger(CertAddRequestValidator.class);
    private Request request;
    private Localizer localizer = Localizer.getInstance();
    static List<String> mandatoryParamsList;
    static List<String> allowedRecipientsType=Lists.newArrayList(JsonKeys.INDIVIDUAL,JsonKeys.ENTITY);
    public CertAddRequestValidator() {
    }

    @Override
    public void validate(Request request) throws BaseException {
        this.request=request;
        if (((String) request.getContext().get(JsonKeys.VERSION)).equalsIgnoreCase(JsonKeys.VERSION_2)) {
            mandatoryParamsList = Lists.newArrayList(JsonKeys.ID, JsonKeys.ACCESS_CODE, JsonKeys.JSON_URL);
        } else {
            mandatoryParamsList = Lists.newArrayList(JsonKeys.ID, JsonKeys.ACCESS_CODE, JsonKeys.PDF_URL);
        }
        validateMandatoryParams();
        validateMandatoryJsonData();
        if(request.getRequest().containsKey(JsonKeys.RECIPIENT_TYPE)) {
            validateRecipientType();
        }
        if(request.getRequest().containsKey(JsonKeys.RELATED)){
            validateRelatedObject();
        }
        logger.info("CertAddRequestValidator:validateMandatoryParams:correct request provided");
    }

    private void validateMandatoryJsonData() throws BaseException {
        Object jsonDataObj = request.getRequest().get(JsonKeys.JSON_DATA);
        Map<?, ?> jsonDataMap = null;
        if (jsonDataObj instanceof scala.collection.Map) {
            // Convert Scala Map to Java Map
            jsonDataMap = CollectionConverters.asJava((scala.collection.Map<?, ?>) jsonDataObj);
        } else if (jsonDataObj instanceof Map) {
            // Already a Java Map
            jsonDataMap = (Map<?, ?>) jsonDataObj;
        }
        if(MapUtils.isEmpty(jsonDataMap)){
            logger.error("CertAddRequestValidator:validateMandatoryJsonData:incorrect request provided");
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.EMPTY_MANDATORY_PARAM,null),JsonKeys.JSON_DATA), ResponseCode.CLIENT_ERROR.getCode());
        }
        validateDataType();
    }
    private void validateDataType() throws BaseException {
        Object jsonDataObj = request.get(JsonKeys.JSON_DATA);
        if (!(jsonDataObj instanceof Map) && !(jsonDataObj instanceof scala.collection.Map)) {
            logger.error("CertAddRequestValidator:validateDataType:incorrect request provided");
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.DATA_TYPE_ERROR,null),JsonKeys.JSON_DATA,"map"), ResponseCode.CLIENT_ERROR.getCode());

        }
    }

    private void validateMandatoryParams() throws BaseException {
        Map<String,Object>certAddReqMap=request.getRequest();
        if(MapUtils.isEmpty(request.getRequest())){
            logger.error("CertAddRequestValidator:validateMandatoryParams:incorrect request provided");
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, getLocalizedMessage(IResponseMessage.INVALID_REQUESTED_DATA,null), ResponseCode.CLIENT_ERROR.getCode());
        }
        for (String param :mandatoryParamsList) {
            if(!certAddReqMap.containsKey(param)){
                logger.error("CertAddRequestValidator:validateMandatoryParams:incorrect request provided");
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.MISSING_MANDATORY_PARAMS,null),param), ResponseCode.CLIENT_ERROR.getCode());
            }
            if(!(certAddReqMap.get(param) instanceof String)){
                logger.error("CertAddRequestValidator:validateMandatoryParams:incorrect request provided");
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.DATA_TYPE_ERROR,null),param,"string"), ResponseCode.CLIENT_ERROR.getCode());
            }
            validatePresence(param,(String)certAddReqMap.get(param));
        }
    }

    private void validatePresence(String key,String value) throws BaseException {
        if (StringUtils.isBlank(value)) {
            logger.error("CertAddRequestValidator:validatePresence:incorrect request provided");
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.EMPTY_MANDATORY_PARAM,null),key), ResponseCode.CLIENT_ERROR.getCode());
        }
    }


    private void validateRelatedObject() throws BaseException {
        Object relatedObj = request.getRequest().get(JsonKeys.RELATED);
        if(!(relatedObj instanceof Map) && !(relatedObj instanceof scala.collection.Map)){
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.DATA_TYPE_ERROR,null),JsonKeys.RELATED,"map"), ResponseCode.CLIENT_ERROR.getCode());
        }
        Map<String, Object> relatedMap = null;
        if (relatedObj instanceof scala.collection.Map) {
            relatedMap = (Map<String, Object>) CollectionConverters.asJava((scala.collection.Map<?, ?>) relatedObj);
        } else if (relatedObj instanceof Map) {
            relatedMap = (Map<String, Object>) relatedObj;
        }
        if(!relatedMap.containsKey(JsonKeys.TYPE)){
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.MISSING_MANDATORY_PARAMS,null), JsonKeys.TYPE.concat(" inside related map")), ResponseCode.CLIENT_ERROR.getCode());
        }
        String relatedType=(String)relatedMap.get(JsonKeys.TYPE);
        if(StringUtils.isBlank(relatedType)){
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA,getLocalizedMessage(IResponseMessage.INVALID_RELATED_TYPE,null), ResponseCode.CLIENT_ERROR.getCode());
        }
    }

    private void validateRecipientType() throws BaseException {

        String recType=(String)request.getRequest().get(JsonKeys.RECIPIENT_TYPE);
        if(!allowedRecipientsType.contains(recType)){
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, MessageFormat.format(getLocalizedMessage(IResponseMessage.INVALID_RECIPIENT_TYPE,null),recType,allowedRecipientsType), ResponseCode.CLIENT_ERROR.getCode());
        }
    }

    private String getLocalizedMessage(String key, Locale locale){
        return localizer.getMessage(key, locale);
    }
}