/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.data.utils.CommonDataUtils;
import com.dabarobjects.madex.data.points.LoadPointByCodeOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.MadexAgent;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.vts.data.points.agents.LoadAgentForPointOperation;

/**
 * arr[0] = facility name arr[1] = firstname arr[2] = lastname arr[3] = mobile1
 * arr[4] = mobile2 arr[5] = community arr[6] = phc code
 *
 * @author dabar
 */
public class PHCOfficerInfoRegistrationBinder implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "h";
    }

    @Override
    public ResponseBundle processPayloadData(String devicePointId, String pin, String[] dataArray, int arrayLength, 
    AbstractBridgeOperation dataAccess, MadexConfBean mConf,MadexSMSEntry smsEntry) {

        String facilityName = dataArray[0];
        String firstname = dataArray[1];
        String lastname = dataArray[2];
        String mobile1 = dataArray[3];
        String mobile2 = dataArray[4];
        String commLga = dataArray[5];
        String phcCodeIndex = dataArray[6];

        
        String phcCode = devicePointId + "-PHC" + phcCodeIndex;

        Long parentLoc = (Long) dataAccess.read(new LoadPointByCodeOperation(devicePointId));
        if (parentLoc != null) {
            MadexAgent seniorME = (MadexAgent) dataAccess.read(new LoadAgentForPointOperation(parentLoc));
            if (seniorME != null) {
                if (!CommonDataUtils.containsGSM(mobile1)) {
                    ResponseBundle resp1 = new ResponseBundle(Boolean.FALSE, devicePointId,
                            "Provide Proper GSM Number For Officer");
                    resp1.putParameter("act", "updateOfficer");
                    resp1.putParameter("PRIVATE_MESSAGE", "The Facility Office Record Submitted For " + facilityName + " is not proper due to missing or improper Mobile Entry. Please resend");
                        resp1.putParameter("PRIVATE_MOBILE", seniorME.getAgentPersonalMobile());
                    return resp1;
                }

                
                MadexLocation phcCodeloc = (MadexLocation) dataAccess.read(new LoadPointByCodeOperation.//
                        UpdateLastReportDateForAgent(phcCode));


                if (phcCodeloc != null) {
                    MadexAgent agent = (MadexAgent) dataAccess.read(new LoadAgentForPointOperation(phcCodeloc.getId()));
                    System.out.println(phcCodeloc);
                    phcCodeloc.setPointName(facilityName);
                    phcCodeloc.setLgaAxis(commLga);
                    boolean ok1 = dataAccess.write(new DefaultUpdateDataOperation<MadexLocation>(phcCodeloc));

                    agent.setFirstName(firstname);
                    agent.setLastName(lastname);
                    agent.setAgentMobile(mobile2);
                    agent.setAgentName(firstname + " " + lastname);
                    agent.setAgentPersonalMobile(mobile1);
                    boolean ok2 = dataAccess.write(new DefaultUpdateDataOperation<MadexAgent>(agent));

                    if (ok1 && ok2) {
                        ResponseBundle resp1 = new ResponseBundle(Boolean.TRUE, phcCode,
                                "Facility Updated Successfully At Server");
                        resp1.putParameter("act", "updateOfficer");
                        resp1.putParameter("PRIVATE_MESSAGE", "You Have Been Successfully "
                                + "Registered With NPHCDA MADEX Submitting Your Facility "
                                + "Reports Monthly To " + seniorME.getAgentName());
                        resp1.putParameter("PRIVATE_MOBILE", mobile1);
                        return resp1;
                    } else {
                        ResponseBundle resp1 = new ResponseBundle(Boolean.FALSE, phcCode, "System Error Encountered Updating Facility");
                        resp1.putParameter("act", "updateOfficer");
                        return resp1;
                    }
                }
            }
        }


        ResponseBundle resp1 = new ResponseBundle(Boolean.FALSE, phcCode, "Could Not Update Facility At Server Due To Missing Cluster Information. Contact Support");
        resp1.putParameter("act", "updateOfficer");
        return resp1;
    }
}
