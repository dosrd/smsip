/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultSaveDataOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.MadexRequest;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import java.util.Date;

/**
 *
 * @author dabar
 */
public class OfficerRequestBindingProtocol implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "r";
    }

    @Override
    public ResponseBundle processPayloadData(
            String devicePointId,
            String pin,
            String[] dataArray,
            int arrayLength,
            AbstractBridgeOperation dataAccess,MadexConfBean mConf,
            MadexSMSEntry smsEntry) {

        String requestType = dataArray[0];
        String deviceId = dataArray[1];
        String objectData = dataArray[2];

        if (requestType.equalsIgnoreCase("password")) {
            MadexRequest request = new MadexRequest();
            request.setRequestType(MadexRequest.RequestType.FORGOT_PASSWORD);
            request.setSentDate(new Date());
            request.setSenderDevice(deviceId);
            request.setRequestLocationId(devicePointId);
            request.setRequestData(requestType);
            boolean saved = dataAccess.write(new DefaultSaveDataOperation<MadexRequest>(request));
            if (saved) {
                ResponseBundle rBundle = new ResponseBundle(Boolean.TRUE, "", 
                        "Request Acknowledged. Await Response Shortly");
                rBundle.putParameter("", "");
                return rBundle;
            }
        }


        ResponseBundle rBundle = new ResponseBundle(Boolean.FALSE, "", "");

        return rBundle;
    }
}
