/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.data.operations.data.WritingDataOperation;
import com.dabarobjects.data.utils.CommonDateUtils;
import com.dabarobjects.data.utils.date.MonthlyReportTime;
import com.dabarobjects.madex.phc.data.*;
import com.dabarobjects.madex.phc.data.clusters.VerifyAgentClusterOperation;
import com.dabarobjects.madex.data.points.LoadPointByCodeOperation;
import com.dabarobjects.madex.gateway.MadexBackgroundService;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.gateway.services.phc.SumParentPointsForMonthPlug;
import com.dabarobjects.vts.data.points.results.ListResultsForPointOperation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Year will be referenced from year 2010 meaning 2012 will be 2 saving us 3
 * extra chars Month will start from 0 to 11, jan to dec
 * "header,arr_length,point_id,generated_pin,report_month,report_year,phc_no,R1,...,Rn";
 *
 * @author dabar
 */
public class PHCMadexReportDataGoeProtocol implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "e";
    }

    /**
     * Method needs to still be rewritten. if the total array size sent is
     * larger than the pre-configured size in DATA_TRANSMISSION_KEYS, it will
     * crash
     *
     * @param devicePointId
     * @param pin
     * @param pimData
     * @param arrayLength
     * @param dataAccess
     * @return
     */
    @Override
    public ResponseBundle processPayloadData(String devicePointId,
            String pin, String[] pimData, int arrayLength,
            AbstractBridgeOperation dataAccess, MadexConfBean mConf,MadexSMSEntry smsEntry) {
        try {
            //Since we will be submitting per PHC, during initial registration the same agent will be replicated for each of the phc or a flag to support parent should be added

            ResponseBundle vBundle = (ResponseBundle) dataAccess.read(new VerifyAgentClusterOperation(devicePointId, pin, true));
            System.out.println(vBundle);
            if (vBundle.getOperationSuccess()) {
                //Long pointId = (Long) dataAccess.read(new LoadPointByCodeOperation.LoadPointIdObjectId(devicePointId));
                String monthIndexStr = pimData[0];
                String yearIndexStr = pimData[1];
                String phcId = pimData[2];
                String gpsPoint = pimData[3];
                String[] gps = gpsPoint.split(",");
                String lat = "";
                String lg = "";
                if (gps.length == 2) {
                    lat = gps[0];
                    lg = gps[1]; 
                }

                //Adding geo location at PHC ID level: phcId
                Integer month = Integer.parseInt(monthIndexStr);
                Integer yearIndex = Integer.parseInt(yearIndexStr);
                Integer yearEpoch = 2010;
                Integer fullyear = yearEpoch + yearIndex;
                Date reportDate = CommonDateUtils.getLastDateForMonth(CommonDateUtils.constructDate(fullyear, month, 1));

                //This is a critical operation, index translation
                Integer phcNoIndex = Integer.parseInt(phcId) + 1;

                String phcCode = devicePointId + "-PHC" + phcNoIndex;
                //Long pointPhcId = (Long) dataAccess.read(new LoadPointByCodeOperation.LoadPointIdObjectId(phcCode));
                System.out.println("Reporting Month Entry: " + reportDate + " PHC: " + phcCode + ", " + devicePointId + " " + Arrays.asList(pimData));
                List<MadexReport> reportForLocForDateList = new ArrayList<MadexReport>();

                ListResultsForPointOperation listReportsOPs =
                        new ListResultsForPointOperation(phcCode, reportDate, true);
                reportForLocForDateList.addAll((List<MadexReport>) dataAccess.read(listReportsOPs));

                List<WritingDataOperation> dataWrites = new ArrayList<WritingDataOperation>();

                int i = 4;
                for (MadexReport listRes : reportForLocForDateList) {
                    //System.out.println("Order: " + listRes.getOrdinal());
                    String value = pimData[i];
                    int ordinal = i - 4;
                    try {
                        listRes.setImpliedScore(Long.parseLong(value));
                        listRes.setSubmittedData(Long.parseLong(value));
                    } catch (NumberFormatException numberFormatException) {
                        numberFormatException.printStackTrace();
                        listRes.setImpliedScore(0l);
                        listRes.setSubmittedData(0l);
                    }
                    listRes.setReportingDate(reportDate);
                    listRes.setOrdinal(ordinal);
                    listRes.setLastUpdateTime(new Date());
                    listRes.setGpsLatitude(lat);
                    listRes.setGpsLongitude(lg);
                    //dataAccess.write(new DefaultUpdateDataOperation<MadexReport>(listRes));

                    dataWrites.add(new DefaultUpdateDataOperation<MadexReport>(listRes));
                    i++;
                }




                dataAccess.setBatchMode(true);
                for (WritingDataOperation writingDataOperation : dataWrites) {
                    dataAccess.write(writingDataOperation);
                }
                boolean ok = dataAccess.completeBatch();
                if (ok) {

                    //load all the PHCs under the cluster point here and add them together to form cluster addition. a background thread will sum for the states
                    // MadexLocation loc = (MadexLocation) dataAccess.read(new LoadPointByCodeOperation.//
                    //  UpdateLastReportDateForAgent(devicePointId));
                    MadexLocation phcCodeloc = (MadexLocation) dataAccess.read(new LoadPointByCodeOperation.//
                            UpdateLastReportDateForAgent(phcCode));

                    dataAccess.read(new LoadPointByCodeOperation.//
                            UpdateLastReportDateForAgent(devicePointId));

                    MonthlyReportTime mrt = new MonthlyReportTime(month, fullyear);
                    

                    //MadexEntryApplication.LIVE_REPORT.pushLiveData(phcCodeloc);
                    ResponseBundle resp = new ResponseBundle(Boolean.TRUE, devicePointId, "Report Submitted And Processed Successfully");
                    resp.putParameter("act", "reports");
                    resp.putParameter("rPHC", phcId);
                    resp.putParameter("rYear", yearIndexStr);
                    resp.putParameter("rMonth", monthIndexStr);
                    resp.putParameter("PRIVATE_MESSAGE", "Congratulations, Your Report For " + mrt.toString() + " has been processed successfully for Facility: " + phcCodeloc.getPointName());
                    resp.putParameter("PRIVATE_MOBILE", vBundle.getParameter("agent_mobile"));
                    return resp;
                } else {
                    System.out.println("Could Not Save Report...");
                    MadexLocation phcCodeloc = (MadexLocation) dataAccess.read(new LoadPointByCodeOperation.//
                            UpdateLastReportDateForAgent(phcCode));
                    //dataAccess.write(new LoadPointByCodeOperation.UpdateLastReportDateForAgentOperation(devicePointId));

                    MonthlyReportTime mrt = new MonthlyReportTime(month, fullyear);
                    ResponseBundle vBundle1 = new ResponseBundle(Boolean.FALSE, devicePointId, "Unexpected Server Error Occurred. Please Try Again");
                    vBundle1.putParameter("act", "reports");
                    vBundle1.putParameter("PRIVATE_MESSAGE", "Unfortunately, Your Report For " + mrt.toString() + " for Facility: " + phcCodeloc.getPointName() + " could not be processed. Please Try Again Later");
                    vBundle1.putParameter("PRIVATE_MOBILE", vBundle.getParameter("agent_mobile"));
                    return vBundle1;
                }


            } else {
                vBundle.putParameter("act", "reports");
                return vBundle;
            }


        } catch (Exception numberFormatException) {
            numberFormatException.printStackTrace();
        }
        ResponseBundle vBundle1 = new ResponseBundle(Boolean.FALSE, devicePointId, "Unexpected Server Error Occurred. Please Try Again");
        vBundle1.putParameter("act", "reports");
        return vBundle1;

    }
}
