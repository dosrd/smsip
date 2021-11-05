/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.madex.phc.data.clusters.ConfirmRegisterClusterAgentOperation;

/**
 * header,arr_length,point_id,generated_pin,password,gps_long,gps_lat,name
 *
 * @author dabar
 */
public class RegistrationConfirmationBinding implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "a";
    }

    @Override
    public ResponseBundle processPayloadData(String devicePointId, String pin, String[] dataArray,
            int arrayLength, AbstractBridgeOperation dataAccess, MadexConfBean mConf,MadexSMSEntry smsEntry) {
        //check if point exists
        //confirm an agent is registered with the pin sent
        //unload data array

        String password = dataArray[0];
        String gpsPoint = dataArray[1];
        String lat = "";
        String lg = "";
        if (gpsPoint != null) {
            System.out.println("GPS: " + gpsPoint);
            String[] gps = gpsPoint.split(",");

            if (gps.length == 2) {
                lat = gps[0];
                lg = gps[1];

            }
        }


        ConfirmRegisterClusterAgentOperation register = new ConfirmRegisterClusterAgentOperation(devicePointId, pin, password, lg, lat);

        ResponseBundle resp = (ResponseBundle) dataAccess.read(register);
        resp.putParameter("action", "completion");
        resp.putParameter("gsmNo", smsEntry.getSender());
        return resp;
    }
}
