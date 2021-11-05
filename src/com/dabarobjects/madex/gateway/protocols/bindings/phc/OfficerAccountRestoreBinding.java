/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultSaveDataOperation;
import com.dabarobjects.madex.data.points.LoadPointByCodeOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.*;
import com.dabarobjects.madex.phc.data.clusters.UpdateClusterDeviceAndAgentOperation;
import com.dabarobjects.vts.data.points.agents.LoadAgentForPointOperation;
import java.util.Arrays;
import java.util.Date;

/**
 * A request does not need automatic verification A request is an object that
 * signifies a change in the system configuration of a madex location
 * request_type, A change of Device Request - means that a user damages or
 * misplaced a previous device and was given a new device to continue to send
 * reports, in such case a change device request is sent which will request 1.
 * New Device ID 2. Cluster or Location ID 3. PIN rtype,nDevice,nCluster,nPIN
 * Change of Officer Information Request 1. New Details (Personal Mobile, Email,
 * Name etc) 2. Cluster 3. PIN 4. Password rtype,nDevice,nCluster,nPIN Change of
 * Password 1. Cluster ID 2. PIN 3. Old password 4. New Password
 *
 * Switching Device To Another Cluster is a matter of deleting the application
 * data and restarting the app
 *
 * @author dabar
 */
public class OfficerAccountRestoreBinding implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "f";
    }

    @Override
    public ResponseBundle processPayloadData(String pointId,
            String pin, String[] dataArray,
            int arrayLength, AbstractBridgeOperation dataAccess,MadexConfBean mConf,
            MadexSMSEntry smsEntry) {

        System.out.println(Arrays.asList(dataArray));
        String requestTypeCode = dataArray[0];
        String newDeviceId = dataArray[1];
        String gpsInfo = dataArray[2];
        String password = dataArray[3];
        
        if(password == null){
            password = "";
        }

        //sim card number may have been changed

        //first we load the cluster code
        //second we load the associated agent, and compare passwords
        Boolean isAvailable = (Boolean) dataAccess.//
                read(new LoadPointByCodeOperation.//
                IsPointObjectAvailableOperation(pointId));
        if (isAvailable) {
            MadexLocation location = (MadexLocation) dataAccess.//
                    read(new LoadPointByCodeOperation.LoadPointObject(pointId));

            MadexAgent agent = (MadexAgent) dataAccess.read(new LoadAgentForPointOperation(location.getId()));
            String agentPin = agent.getAgentPin();
            if (pin.equals(agentPin)) {
                System.out.println("Local Stored Password: " + agent.getLocalPassword() + ", Lenght: " + agent.getLocalPassword().length());
                
                System.out.println("Sent Stored Password: " + password + ", Lenght: " + password.length());
                if (agent.getLocalPassword().equals(password)) {
                    location.setPointRegistration(newDeviceId);
                    agent.setMobileDeviceNo(smsEntry.getSender());
                    UpdateClusterDeviceAndAgentOperation updateDate = //
                            new UpdateClusterDeviceAndAgentOperation(location.getId(), newDeviceId, smsEntry.getSender(), gpsInfo);
                    Boolean added = dataAccess.write(updateDate);
                    if (added) {
                        MadexDevices deviceInfo = new MadexDevices();
                        deviceInfo.setDeviceId(newDeviceId);
                        deviceInfo.setRegisterDate(new Date());
                        deviceInfo.setAssociatedNumber(smsEntry.getSender());
                        deviceInfo.setAssociatedPoint(pointId);
                        dataAccess.write(new DefaultSaveDataOperation<MadexDevices>(deviceInfo));

                        ResponseBundle resp = new ResponseBundle(Boolean.TRUE, pointId, "Restoration Granted");
                        resp.putParameter("act", "restore");
                        resp.putParameter("1", location.getTransStateIndex());
                        resp.putParameter("2", location.getTransZoneIndex());
                        resp.putParameter("3", agent.getFirstName());
                        resp.putParameter("4", agent.getLastName());
                        resp.putParameter("5", location.getPointName());
                        resp.putParameter("6", agent.getEmailAddress());
                        resp.putParameter("7", location.getLgaAxis());
                        resp.putParameter("8", agent.getAgentPersonalMobile());
                        resp.putParameter("9", smsEntry.getSender());
                        resp.putParameter("10", pointId);
                        resp.putParameter("11", pin);
                        resp.putParameter("12", agent.getLocalPassword());
                        resp.putParameter("PRIVATE_MESSAGE", "Your Restoration Request has Been Granted. Please Restart The Application After a few minutes");
                        resp.putParameter("PRIVATE_MOBILE", agent.getAgentPersonalMobile());
                        return resp;


                    } else {
                        ResponseBundle resp = new ResponseBundle(Boolean.FALSE, pointId, "Database Error From Server!");
                        resp.putParameter("act", "restore");
                        resp.putParameter("pointId", pointId);
                        resp.putParameter("deviceNo", smsEntry.getSender());
                        return resp;
                    }
                } else {
                    ResponseBundle resp = new ResponseBundle(Boolean.FALSE, pointId, "The Password You Specified is Incorrect!");
                    resp.putParameter("act", "restore");
                    resp.putParameter("pointId", pointId);
                    resp.putParameter("deviceNo", smsEntry.getSender());
                    return resp;
                }



            } else {
                ResponseBundle resp = new ResponseBundle(Boolean.FALSE, pointId, "Wrong Cluster PIN provided");
                resp.putParameter("act", "restore");
                resp.putParameter("pointId", pointId);
                resp.putParameter("deviceNo", smsEntry.getSender());
                return resp;
            }

        } else {
            ResponseBundle resp = new ResponseBundle(Boolean.FALSE, pointId, "The specified Cluster Code does not match any Registration");
            resp.putParameter("act", "restore");
            resp.putParameter("pointId", pointId);
            resp.putParameter("deviceNo", smsEntry.getSender());
            return resp;
        }



    }
}
