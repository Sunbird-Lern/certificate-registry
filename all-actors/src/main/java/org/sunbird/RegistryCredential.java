package org.sunbird;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class will help in getting the env values for calling the cert service
 * @author anmolgupta
 */
public class RegistryCredential {
    
    private static Logger logger= LoggerFactory.getLogger(RegistryCredential.class);
    private  static final String SERVICE_BASE_URL = getPropsFromEnvs(JsonKeys.REGISTRY_CREDENTIAL_SERVICE_BASE_URL);
    private  static final String CERTIFICATE_TYPE = getPropsFromEnvs(JsonKeys.RC_ENTITY);
    private static final String API = "/api/v1/%s";

    public static String getSERVICE_BASE_URL() {
        if(StringUtils.isBlank(SERVICE_BASE_URL)){
            logger.error("RegistryCredential:getPropsFromEnvs:no suitable host found");
            System.exit(-1);
        }
        return SERVICE_BASE_URL;
    }
    public static String getDOWNLOAD_URI() {
        return String.format(API, CERTIFICATE_TYPE);
    }

    private static String getPropsFromEnvs(String props){
        String propValue = System.getenv(props);
        return propValue;
    }


    public static String getRCSearchUri(){
        String apiUrl = String.format(API, CERTIFICATE_TYPE);
        String rcSearchApi = String.format("%s/%s/search", getSERVICE_BASE_URL().split(",")[0], apiUrl);
        logger.info("RegistryCredential:getRCSearchUri:es uri formed: "+rcSearchApi);
        return rcSearchApi;
    }
}
