/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.madex.data.points.AddPointToPointOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.MadexAgent;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.madex.phc.data.clusters.VerifyAgentClusterOperation;
import com.dabarobjects.vts.data.points.agents.LoadAgentForPointOperation;
import java.util.Arrays;

/**
 *
 * @author dabar
 */
public class ClusterOfficeUpdateProtocolBinder implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "j";
    }

    @Override
    public ResponseBundle processPayloadData(String devicePointId, String pin, String[] dataArray, int arrayLength, 
    AbstractBridgeOperation dataAccess, MadexConfBean mConf,MadexSMSEntry smsEntry) {
        String deviceUniqueId = dataArray[0];
        String nameOfCluster = dataArray[1];
        String firstname = dataArray[2];
        String lastname = dataArray[3];
        String prMobileNo = dataArray[4];
        String prEmailNo = dataArray[5];
        String gpsPoint = dataArray[6];
        String npassword = dataArray[7];

        String[] gps = gpsPoint.split(",");
        String lat = "";
        String lg = "";
        if (gps.length == 2) {
            lat = gps[0];
            lg = gps[1];

        }
        System.out.println(Arrays.asList(dataArray));
        //First we verufy agent is previously registered
        ResponseBundle vBundle = (ResponseBundle) dataAccess.//
                read(new VerifyAgentClusterOperation(devicePointId, pin, true));
        System.out.println(vBundle);
        if (vBundle.getOperationSuccess()) {
            AddPointToPointOperation.DoesClusterExistsOperation clusterExists =
                    new AddPointToPointOperation.DoesClusterExistsOperation(deviceUniqueId, "CLUSTER");
            Boolean exists = (Boolean) dataAccess.read(clusterExists);
            MadexLocation clusterPointPoint;
            if (exists) {
                clusterPointPoint = (MadexLocation) dataAccess.//
                        read(new AddPointToPointOperation.//
                        LoadClusterRegistrationOperation(deviceUniqueId, "CLUSTER"));

                clusterPointPoint.setPointName(nameOfCluster);
                clusterPointPoint.setAgencyAddress(nameOfCluster);
                //clusterPointPoint.setLgaAxis(lgaPoint);
                clusterPointPoint.setGpsLatitude(lat);
                clusterPointPoint.setGpsLongitude(lg);

                LoadAgentForPointOperation loadAgent = //
                        new LoadAgentForPointOperation(clusterPointPoint.getId());
                MadexAgent mAgent = (MadexAgent) dataAccess.read(loadAgent);

                mAgent.setFirstName(firstname);
                mAgent.setLastName(lastname);
                mAgent.setAgentName(firstname +" " +lastname);
                mAgent.setAgentMobile(prMobileNo);
                mAgent.setAgentPersonalMobile(prMobileNo);
                mAgent.setEmailAddress(prEmailNo);
                mAgent.setLocalPassword(npassword);
                //Lets auto generate 4 digit lock PIN

                mAgent.setMobileDeviceNo(smsEntry.getSender());


                Boolean updated1 = dataAccess.write(new DefaultUpdateDataOperation<MadexLocation>(clusterPointPoint));
                Boolean updated2 = dataAccess.write(new DefaultUpdateDataOperation<MadexAgent>(mAgent));
                if (updated1 && updated2) {
                    ResponseBundle resp = new ResponseBundle(Boolean.TRUE, devicePointId, "Profile Updated Successfully At HQ Server");
                    resp.putParameter("act", "update");
                    
                    resp.putParameter("MSG", "Profile Updated Successfully At HQ Server");
                    resp.putParameter("clusterId", devicePointId);
                    resp.putParameter("deviceNo", smsEntry.getSender());
                    resp.putParameter("clusterPin", mAgent.getAgentPin());
                    return resp;
                } else {
                    ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, "Could Not Update Record On Server");
                    resp.putParameter("act", "update");
                    resp.putParameter("MSG", "Could Not Update Record On Server");
                    resp.putParameter("clusterId", devicePointId);
                    return resp;
                }
            }else{
                ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, "Could Not Update Record On Server. Unknown Device Id: ");
                    resp.putParameter("act", "update");
                    resp.putParameter("MSG", "Could Not Update Record On Server");
                    resp.putParameter("clusterId", devicePointId);
                    return resp;
            }
        } else {
            ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, "Wrong PIN Code. Please Verify Your PIN");
            resp.putParameter("clusterId", devicePointId);
            resp.putParameter("MSG", "Wrong PIN Code. Please Verify Your PIN");
            return resp;
        }
    }
}
