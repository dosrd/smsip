/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.utils.date.MonthlyReportTime;
import com.dabarobjects.madex.data.points.LoadPointByCodeOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.madex.phc.data.MadexReport;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.madex.phc.data.clusters.VerifyAgentClusterOperation;
import com.dabarobjects.vts.data.points.results.ListResultsForPointOperation;
import java.util.List;

/**
 *
 * @author dabar
 */
public class FindReportForMonthBinder implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "q";
    }

    @Override
    public ResponseBundle processPayloadData(String devicePointId, String pin, String[] dataArray, int arrayLength, 
    AbstractBridgeOperation dataAccess, MadexConfBean mConf,MadexSMSEntry smsEntry) {
        ResponseBundle vBundle = (ResponseBundle) dataAccess.read(new VerifyAgentClusterOperation(devicePointId, pin, true));
        if (vBundle.getOperationSuccess()) {
            String phcIndexStr = dataArray[0];
            String monthIndexStr = dataArray[1];
            String yearIndexStr = dataArray[2];

            String phcCode = devicePointId + "-PHC" + (Integer.parseInt(phcIndexStr)+1);
            MadexLocation pointPhc = (MadexLocation) dataAccess.read(new LoadPointByCodeOperation.//
                    LoadPointObject(phcCode));
            if (pointPhc != null) {
                int yearIndex = Integer.parseInt(yearIndexStr);
                int monthIndex = Integer.parseInt(monthIndexStr);
                
                MonthlyReportTime mReportTime = new MonthlyReportTime(monthIndex, yearIndex);
                List<MadexReport> reports = (List<MadexReport>) dataAccess.read(new ListResultsForPointOperation(phcCode,
                        mReportTime));
                StringBuilder buf = new StringBuilder();
                if (!reports.isEmpty()) {
                    for (MadexReport madexReport : reports) {
                        buf.append(madexReport.getSubmittedData()).append(",");
                    }
                    buf.deleteCharAt(buf.length() - 1);
                    ResponseBundle resp = new ResponseBundle(Boolean.TRUE, "", "");
                    resp.putParameter("act", "rQuery");
                    resp.putParameter("r", buf.toString());
                    resp.putParameter("y", yearIndexStr);
                    resp.putParameter("m", monthIndexStr);
                    resp.putParameter("p", phcIndexStr);
                    return resp;
                } else {
                    ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, //
                            "No Results Have Been Submitted For The Period " + mReportTime.toString() + " under Facility: " + pointPhc.getPointName());
                    resp.putParameter("act", "rQuery");
                    resp.putParameter("clusterId", devicePointId);
                    resp.putParameter("clusterPin", pin);
                    return resp;
                }

            } else {
                ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, //
                        "Unable To Find Any Registered Facility On Server Related To The Query Sent. Please Verify That You Are Properly Registered");
                resp.putParameter("act", "rQuery");
                resp.putParameter("clusterId", devicePointId);
                resp.putParameter("clusterPin", pin);
                return resp;
            }

        }
        ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, //
                "You Are Not Able To Post Queries Because of an illegal PIN");
        resp.putParameter("act", "rQuery");
        resp.putParameter("clusterId", devicePointId);
        resp.putParameter("clusterPin", pin);
        return resp;
    }
}
