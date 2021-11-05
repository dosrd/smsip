/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultSaveDataOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.madex.data.points.LoadPointByCodeOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.MadexAgent;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.madex.phc.data.MadexRequest;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.vts.data.points.agents.LoadAgentForPointOperation;
import java.util.Date;

/**
 *
 * @author dabar
 */
public class OfficerRequestBindingProtocol2 implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "s";
    }

    @Override
    public ResponseBundle processPayloadData(
            String pointId,
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
            request.setRequestLocationId(pointId);
            request.setRequestData(requestType);
            boolean saved = dataAccess.write(new DefaultSaveDataOperation<MadexRequest>(request));

            Boolean isAvailable = (Boolean) dataAccess.//
                    read(new LoadPointByCodeOperation.//
                    IsPointObjectAvailableOperation(pointId));
            if (isAvailable) {
                MadexLocation location = (MadexLocation) dataAccess.//
                        read(new LoadPointByCodeOperation.LoadPointObject(pointId));

                MadexAgent agent = (MadexAgent) dataAccess.read(new LoadAgentForPointOperation(location.getId()));
                String agentPin = agent.getAgentPin();
                if (pin.equals(agentPin)) {
                    agent.setLocalPassword(objectData);
                    saved = saved && dataAccess.write(new DefaultUpdateDataOperation<MadexAgent>(agent));

                    if (saved) {
                        ResponseBundle rBundle = new ResponseBundle(Boolean.TRUE, pointId,
                                "Request Acknowledged. Await Response Shortly On Your Personal Mobile");
                        rBundle.addTrigger(getClass());
                        rBundle.putParameter("PRIVATE_MESSAGE", "Your App Access Password is " + objectData);
                        rBundle.putParameter("PRIVATE_MOBILE", agent.getAgentPersonalMobile());
                        return rBundle;
                    } else {
                        ResponseBundle rBundle = new ResponseBundle(Boolean.FALSE, pointId,
                                "Server Could Not Grant This Request Due To General Errors. Please Try Again");
                        rBundle.addTrigger(getClass());
                        return rBundle;
                    }

                } else {
                    ResponseBundle rBundle = new ResponseBundle(Boolean.FALSE, pointId,
                            "Wrong Access PIN"); 
                    rBundle.addTrigger(getClass());
                    return rBundle;
                }

            } else {
                ResponseBundle rBundle = new ResponseBundle(Boolean.FALSE, pointId,
                        "No Cluster or Location Associated With The Cluster Code");
                rBundle.addTrigger(getClass());
                return rBundle;
            }


        }


        ResponseBundle rBundle = new ResponseBundle(Boolean.FALSE, "", "Unknown Problems Occurred At Server");
        rBundle.addTrigger(getClass());
        return rBundle;
    }
}
