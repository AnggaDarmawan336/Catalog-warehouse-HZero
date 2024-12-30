package com.hand.demo.infra.util;

import org.hzero.boot.interfaces.sdk.dto.RequestPayloadDTO;
import org.hzero.boot.interfaces.sdk.dto.ResponsePayloadDTO;
import org.hzero.boot.interfaces.sdk.invoke.InterfaceInvokeSdk;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Utils
 */
@Component
public class Utils {
    private Utils() {}

    @Autowired
    private InterfaceInvokeSdk interfaceInvokeSdk;

    public ResponsePayloadDTO invokeInterface(String namespace,
                                              String serverCode,
                                              String interfaceCode,
                                              String jsonString,  MediaType applicationJson){

        RequestPayloadDTO requestPayLoadDTO = new RequestPayloadDTO();
        requestPayLoadDTO.setPayload(jsonString);
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("Authorization", "Bearer" + TokenUtils.getToken());
        Map<String, String> pathParamMap = new HashMap<>();
        pathParamMap.put("organizationId", BaseConstants.DEFAULT_TENANT_ID.toString());
        requestPayLoadDTO.setPathVariableMap(pathParamMap);
        requestPayLoadDTO.setHeaderParamMap(paramMap);
        requestPayLoadDTO.setMediaType("application/json");

        return interfaceInvokeSdk.invoke(namespace,
                serverCode,
                interfaceCode,
                requestPayLoadDTO
        );
    }
}
