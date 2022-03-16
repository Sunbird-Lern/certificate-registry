package org.sunbird;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class will help in getting the env values for calling the cert service
 * @author anmolgupta
 */
public class CertVars {

    public static final String CERT_SERVICE_BASE_URL= "cert_service_base_url";
    private static Logger logger= LoggerFactory.getLogger(CertVars.class);
    private  static final String SERVICE_BASE_URL=getPropsFromEnvs(CERT_SERVICE_BASE_URL);
    private static final String DOWNLOAD_URI= "/v1/user/certs/download";
    private static final String GENERATE_URI="/v1/certs/generate";
    private static final String VERIFY_URI="/v1/certs/verify";
    public static final String SUNBIRD_ES_IP = "sunbird_es_host";


    public static String getVerifyUri() { return VERIFY_URI; }
    public static String getGenerateUri() {
        return GENERATE_URI;
    }

    public static String getSERVICE_BASE_URL() {
        if(StringUtils.isBlank(SERVICE_BASE_URL)){
            logger.error("CertVars:getPropsFromEnvs:no suitable host found for downloadUri");
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
        String esApi=String.format("http://%s:9200/%s/_search",getPropsFromEnvs(SUNBIRD_ES_IP).split(",")[0],JsonKeys.CERT_ALIAS);
        logger.info("CertVars:getRCSearchUri:es uri formed:"+esApi);
        return esApi;
    }
}
