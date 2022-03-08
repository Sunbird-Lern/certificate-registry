package org.sunbird;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class will help in getting the env values for calling the cert service
 * @author anmolgupta
 */
public class RegistryCredential {

    public static final String REGISTRY_CREDENTIAL_SERVICE_BASE_URL= "registry_credential_service_base_url";
    private static Logger logger= LoggerFactory.getLogger(RegistryCredential.class);
    private  static final String SERVICE_BASE_URL=getPropsFromEnvs(REGISTRY_CREDENTIAL_SERVICE_BASE_URL);
    private static final String DOWNLOAD_URI= "/rc/certificate/v1/download";
    public static final String SUNBIRD_ES_IP = "sunbird_es_host";

    public static String getSERVICE_BASE_URL() {
        if(StringUtils.isBlank(SERVICE_BASE_URL)){
            logger.error("RegistryCredential:getPropsFromEnvs:no suitable host found for downloadUri");
            System.exit(-1);
        }
        return SERVICE_BASE_URL;
    }
    public static String getDOWNLOAD_URI() {
        return DOWNLOAD_URI;
    }

    private static String getPropsFromEnvs(String props){
        String propValue=System.getenv(props);
        return propValue;
    }


    public static String getEsSearchUri(){
        String esApi=String.format("http://%s:9200/%s/search",getPropsFromEnvs(SUNBIRD_ES_IP).split(",")[0],"   rc/certificate/v1");
        logger.info("RegistryCredential:getEsSearchUri:es uri formed:"+esApi);
        return esApi;
    }
}
