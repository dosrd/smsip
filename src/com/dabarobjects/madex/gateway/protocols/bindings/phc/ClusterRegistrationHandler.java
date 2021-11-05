/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultSaveDataOperation;
import com.dabarobjects.data.utils.CommonRandomNoUtils;
import com.dabarobjects.madex.phc.data.MadexAgent;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.madex.phc.data.clusters.ListClustersForStateOperation;
import com.dabarobjects.madex.data.points.AddPointToPointOperation;
import com.dabarobjects.madex.data.points.CreateRootPointOperation;
import com.dabarobjects.madex.data.points.LoadPointByCodeOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.*;
import com.dabarobjects.vts.data.points.agents.LoadAgentForPointOperation;
import java.util.Arrays;
import java.util.Date;

/**
 *
 * @author dabar
 */
public class ClusterRegistrationHandler implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "d";
    }
    public static final String[] STATES = {"Abia", "Adamawa", "Akwa Ibom",
        "Anambra", "Bauchi", "Bayelsa", "Benue", "Borno", "Cross River",
        "Delta", "Ebonyi", "Edo", "Ekiti", "Enugu", "Gombe", "Imo",
        "Jigawa", "Kaduna", "Kano", "Katsina", "Kebbi", "Kogi", "Kwara",
        "Lagos", "Nasarawa", "Niger", "Ogun", "Ondo", "Osun", "Oyo",
        "Plateau", "Rivers", "Sokoto", "Taraba", "Yobe", "Zamfara", "FCT"};
    public static final String[] STATES_ZONES = {"Abia", "SE", "Adamawa",
        "NE", "Akwa Ibom", "SS", "Anambra", "SE", "Bauchi", "NE",
        "Bayelsa", "SS", "Benue", "NC", "Borno", "NE", "Cross River", "SS",
        "Delta", "SS", "Ebonyi", "SE", "Edo", "SS", "Ekiti", "SW", "Enugu",
        "SE", "Gombe", "NE", "Imo", "SE", "Jigawa", "NW", "Kaduna", "NW",
        "Kano", "NW", "Katsina", "NW", "Kebbi", "NW", "Kogi", "NC",
        "Kwara", "NC", "Lagos", "SW", "Nasarawa", "NC", "Niger", "NC",
        "Ogun", "SW", "Ondo", "SW", "Osun", "SW", "Oyo", "SW", "Plateau",
        "NC", "Rivers", "SS", "Sokoto", "NW", "Taraba", "NE", "Yobe", "NE",
        "Zamfara", "NW", "FCT", "NC"};

    public static final String getZone(String state) {
        for (int i = 0; i < STATES_ZONES.length; i += 2) {
            String dataStr = STATES_ZONES[i];
            if (dataStr.equalsIgnoreCase(state)) {
                String sz = STATES_ZONES[(i + 1)];

                return sz;
            }
        }
        return "NC";
    }
    public static final String[] ZONES = {"SW", "NW", "SE", "NE", "NC", "SS"};

    public static int getZoneIndex(String inZone) {
        for (int i = 0; i < ZONES.length; i++) {
            String zz = ZONES[i];

            if (inZone.equalsIgnoreCase(zz)) {

                return i;
            }
        }
        return 0;
    }
    public static final String[] MONTHS = {"January", "February", "March",
        "April", "May", "June", "July", "August", "September", "October",
        "November", "December"};
    public static String[] YEARS_ARR = {"2010", "2011", "2012", "2013",
        "2014", "2015", "2016", "2017", "2018", "2019", "2020"};
    public static String[] PHCS = {"PHC1", "PHC2", "PHC3", "PHC4"};

    @Override
    public ResponseBundle processPayloadData(String devicePointId, String pin, String[] dataArray, int arrayLength, 
    AbstractBridgeOperation dataAccess,MadexConfBean mConf, MadexSMSEntry smsEntry) {
        System.out.println(Arrays.asList(dataArray));

        String clusterStateIndex = dataArray[0];
        String zoneIndex = dataArray[1];
        String deviceId = dataArray[2];
        String gpsPoint = dataArray[3];
        String officerFirstname = dataArray[4];
        String officerLastname = dataArray[5];
        String nameOfHospital = dataArray[6];
        String prEmailNo = dataArray[7];
        String prMobileNo = dataArray[8];
        String lgaPoint = dataArray[9];

        String[] gps = gpsPoint.split(",");
        String lat = "";
        String lg = "";
        if (gps.length == 2) {
            lat = gps[0];
            lg = gps[1];

        }

        String zone = ZONES[Integer.parseInt(zoneIndex)];
        //We will translate the index sent from the mobile device by 1
        //We have reserved code or id to value=0
        int stateIndexTranslate = Integer.parseInt(clusterStateIndex) + 1;
        String state = STATES[Integer.parseInt(clusterStateIndex)];
        String stateCode = "" + stateIndexTranslate;

        //Create the cluster where all the reporting takes place. this is where the device ID is kept
        //Most of the action takes place at this level and therefore we put more functions


        String clusterCode = "";

        String pointRegister = deviceId;

        AddPointToPointOperation.DoesClusterExistsOperation clusterExists =
                new AddPointToPointOperation.DoesClusterExistsOperation(pointRegister, "CLUSTER");
        
        Long activatedDevices = (Long) dataAccess.read(new AddPointToPointOperation.CountAllAvailableDevicesOperation());
        if(activatedDevices > 300){
            ResponseBundle resp = new ResponseBundle(Boolean.TRUE, devicePointId, "Maximum Number Of Devices Reached. Contact Your Agency To Expand Capacity");
            resp.putParameter("act", "alert");
            resp.putParameter("clusterId", clusterCode);
            resp.putParameter("deviceNo", smsEntry.getSender()); 
            resp.putParameter("PRIVATE_MESSAGE", "Maximum Number Of Devices Reached. Contact Your Agency To Expand Capacity");
            resp.putParameter("PRIVATE_MOBILE", prMobileNo);
            return resp;
        }
        Boolean exists = (Boolean) dataAccess.read(clusterExists);
        MadexLocation clusterPointPoint;
        if (exists) {
            clusterPointPoint = (MadexLocation) dataAccess.read(new AddPointToPointOperation.//
                    LoadClusterRegistrationOperation(pointRegister, "CLUSTER"));
            clusterCode = clusterPointPoint.getPointCode();

            LoadAgentForPointOperation loadAgent = new LoadAgentForPointOperation(clusterPointPoint.getId());
            MadexAgent mAgent = (MadexAgent) dataAccess.read(loadAgent);

            ResponseBundle resp = new ResponseBundle(Boolean.TRUE, devicePointId, "Please This Registration Already Exists, To Connect Back With Your Former Account, Use The Restore Account Option.");
            resp.putParameter("act", "alert");
            resp.putParameter("clusterId", clusterCode);
            resp.putParameter("deviceNo", smsEntry.getSender());
            resp.putParameter("clusterPin", mAgent.getAgentPin());
            resp.putParameter("PRIVATE_MESSAGE", "Please This Registration Already Exists, To Connect Back With Your Former Account, Use The Restore Account Option.");
            resp.putParameter("PRIVATE_MOBILE", prMobileNo);
            return resp;


        } else {
            Long rootId = (Long) dataAccess.read(new CreateRootPointOperation("Nigeria"));
            try {
                //Create Zones
                MadexLocation zonePoint = new MadexLocation();
                zonePoint.setClosed(Boolean.FALSE);
                zonePoint.setPointName(MADEXParameters.getZoneNames(zone));
                zonePoint.setPointCode(zone);
                zonePoint.setPointId(zone);
                zonePoint.setPointRegistration(zone);
                zonePoint.setPointType("ZONE");
                zonePoint.setGpsLatitude(lat);

                zonePoint.setGpsLongitude(lg);
                zonePoint.setPointZone(zone);
                Long zonePointId = (Long) dataAccess.read(new AddPointToPointOperation(rootId, zonePoint));

                //Create the state
                MadexLocation statePoint = new MadexLocation();
                statePoint.setClosed(Boolean.FALSE);
                statePoint.setPointName(state);
                statePoint.setPointCode("" + stateIndexTranslate);
                statePoint.setPointId("" + stateIndexTranslate);
                statePoint.setPointRegistration("" + stateIndexTranslate);
                statePoint.setPointType("STATE");
                statePoint.setGpsLatitude(lat);

                statePoint.setGpsLongitude(lg);
                statePoint.setPointZone(zone);
                Long statePointId = (Long) dataAccess.read(new AddPointToPointOperation(zonePointId, statePoint));


                //Create cluster
                clusterPointPoint = new MadexLocation();
                Long countCluster = (Long) dataAccess.read(new ListClustersForStateOperation.//
                        CountClusterForStateOperation(stateCode));

                long clusterCountLg = countCluster + 1;
                clusterCode = zone + "" + stateCode + "CL" + (clusterCountLg);
                System.out.println("Generated ...." + clusterCode);
                Boolean usedAlready = (Boolean) dataAccess.read(new LoadPointByCodeOperation.IsPointObjectAvailableOperation(clusterCode));
                System.out.println("Code status: " + usedAlready);
                int rounds = 100;

                while (usedAlready) {
                    rounds--;
                    clusterCountLg = clusterCountLg + 1;
                    clusterCode = zone + "" + stateCode + "CL" + (clusterCountLg);
                    usedAlready = (Boolean) dataAccess.read(new LoadPointByCodeOperation.IsPointObjectAvailableOperation(clusterCode));
                    if (rounds < 1) {
                        //a small check to help prevent everlasting loop
                        break;
                    }
                }

                if (usedAlready) {
                    ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, "Could Not Allocate A Cluster Code To Your Cluster Due To Conflicts on Server Side");
                    resp.putParameter("act", "register");
                    resp.putParameter("clusterId", clusterCode);
                    resp.putParameter("clusterPin", "");

                    return resp;
                }

                clusterPointPoint.setClosed(Boolean.FALSE);
                clusterPointPoint.setPointCode(clusterCode);
                clusterPointPoint.setPointId(clusterCode);
                clusterPointPoint.setPointRegistration(deviceId);
                clusterPointPoint.setPointZone(zone);
                clusterPointPoint.setPointType("CLUSTER");
                clusterPointPoint.setPointName(nameOfHospital);
                clusterPointPoint.setAgencyAddress(nameOfHospital);
                clusterPointPoint.setLgaAxis(lgaPoint);
                clusterPointPoint.setGpsLatitude(lat);
                clusterPointPoint.setGpsLongitude(lg);
                clusterPointPoint.setTransStateIndex(Integer.parseInt(clusterStateIndex));
                clusterPointPoint.setTransZoneIndex(Integer.parseInt(zoneIndex));

                MadexDevices deviceInfo = new MadexDevices();
                deviceInfo.setDeviceId(deviceId);
                deviceInfo.setRegisterDate(new Date());
                deviceInfo.setAssociatedNumber(smsEntry.getSender());
                deviceInfo.setAssociatedPoint(clusterCode);
                dataAccess.write(new DefaultSaveDataOperation<MadexDevices>(deviceInfo));


                MadexAgent mAgent = new MadexAgent();
                mAgent.setFirstName(officerFirstname);
                mAgent.setLastName(officerLastname);
                mAgent.setAgentName(officerFirstname + " " + officerLastname);
                mAgent.setAgentMobile(prMobileNo);
                mAgent.setAgentPersonalMobile(prMobileNo);
                mAgent.setEmailAddress(prEmailNo);
                //Lets auto generate 4 digit lock PIN
                String pin4 = CommonRandomNoUtils.generateKey(4);
                mAgent.setLockedIn(Boolean.TRUE);
                mAgent.setLockedDate(new Date());
                mAgent.setAgentPin(pin4);
                mAgent.setAgentId(CommonRandomNoUtils.generateKey(7));
                mAgent.setMobileDeviceNo(smsEntry.getSender());
                mAgent.setStaffId(devicePointId);
                mAgent.setRegisterDate(new Date());
                mAgent.setLocalPassword("");
                mAgent.setPointId(clusterCode);
                mAgent.setPointName(nameOfHospital);




                Long clusterId = (Long) dataAccess.read(new AddPointToPointOperation(statePointId,
                        clusterPointPoint, mAgent));
                //Create the 4 clusters 

                for (int i = 1; i <= 4; i++) {

                    MadexAgent mAgentg = new MadexAgent();
                    mAgentg.setFirstName(officerFirstname);
                    mAgentg.setLastName(officerLastname);
                    mAgentg.setAgentName(officerFirstname + " " + officerLastname);
                    //mAgentg.setAgentMobile(prMobileNo);
                    mAgentg.setAgentPersonalMobile(prMobileNo);
                    //mAgentg.setEmailAddress(prEmailNo);
                    //Lets auto generate 4 digit lock PIN
                    String pin4g = CommonRandomNoUtils.generateKey(4);
                    mAgentg.setLockedIn(Boolean.TRUE);
                    mAgentg.setLockedDate(new Date());
                    mAgentg.setAgentPin(pin4g);
                    mAgentg.setAgentId(CommonRandomNoUtils.generateKey(7));
                    //mAgentg.setMobileDeviceNo(smsEntry.getSender());
                    mAgentg.setStaffId(devicePointId);
                    mAgentg.setRegisterDate(new Date());
                    mAgentg.setLocalPassword("");


                    String phcCode = clusterCode + "-PHC" + i;
                    MadexLocation phcPoint = new MadexLocation();
                    phcPoint.setClosed(Boolean.FALSE);
                    phcPoint.setPointCode(phcCode);
                    phcPoint.setPointId(phcCode);
                    phcPoint.setLgaAxis(lgaPoint);
                    phcPoint.setPointZone(zone);
                    phcPoint.setPointName("PHC Facility " + i);
                    phcPoint.setPointRegistration(phcCode);
                    phcPoint.setPointType("PHC");
                    phcPoint.setGpsLatitude(lat);
                    phcPoint.setGpsLongitude(lg);
                    Long phcPointId = (Long) dataAccess.//
                            read(new AddPointToPointOperation(clusterId, phcPoint, mAgentg));
                }
                ResponseBundle resp = new ResponseBundle(Boolean.TRUE, devicePointId, "Created Cluster");
                resp.putParameter("act", "register");
                resp.putParameter("clusterId", clusterCode);
                resp.putParameter("clusterPin", pin4);
                resp.putParameter("deviceNo", smsEntry.getSender());
                resp.putParameter("PRIVATE_MESSAGE", "You Have Been Successfully Registered With NPHCDA MADEX. You Cluster Code is " + clusterCode + " and your PIN is " + pin4);
                resp.putParameter("PRIVATE_MOBILE", prMobileNo);


                return resp;
            } catch (Exception e) {
                ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, e.getMessage());
                resp.putParameter("act", "register");
                resp.putParameter("clusterId", clusterCode);
                resp.putParameter("clusterPin", "");

                return resp;
            }
        }








        //Long statePointId = (Long) dataAccess.read(new AddPointToPointOperation(rootId, statePoint));


    }
}
