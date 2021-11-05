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
import com.dabarobjects.madex.phc.data.MadexFeedback;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.madex.phc.data.clusters.VerifyAgentClusterOperation;
import java.util.Date;

/**
 * Definition of Binding deviceRecId;phc;month;year;subject;{message};
 *
 * @author dabar
 */
public class SubmitComplaintBinding implements MadexGatewayProcessingUnit {

    @Override
    public String getHeaderBinder() {
        return "g";
    }

    @Override
    public ResponseBundle processPayloadData(String devicePointId, String pin, String[] dataArray,
            int arrayLength, AbstractBridgeOperation dataAccess, MadexConfBean mConf,MadexSMSEntry smsEntry) {

        ResponseBundle vBundle = (ResponseBundle) dataAccess.read(new VerifyAgentClusterOperation(devicePointId, pin, true));
        if (vBundle.getOperationSuccess()) {
            String localIndex = dataArray[0];
            String relatedPhc = dataArray[1];
            String feedbackSubjectIndex = dataArray[2]; 
            String message = dataArray[3];

             
            //String phcCode = clusterCode + "-PHC" + i;
            //This is a critical operation, index translation
            Integer phcNoIndex = Integer.parseInt(relatedPhc) + 1;

            MadexFeedback feedback = new MadexFeedback();
            feedback.setMessageDate(new Date());
            feedback.setRelatedFeedbackDate(new Date());
            feedback.setMadexLocationId(devicePointId);
            feedback.setFeedbackType(feedbackSubjectIndex);
            feedback.setMessageContent(message);
            feedback.setRelatedLocationId("" + phcNoIndex); 
            feedback.setMobileCode(smsEntry.getSender());

            Boolean saved = dataAccess.write(new DefaultSaveDataOperation<MadexFeedback>(feedback));
            if (saved) {
                ResponseBundle resp = new ResponseBundle(Boolean.TRUE, devicePointId, "Created Feedback");
                resp.putParameter("act", "feedback");
                resp.putParameter("localRecId", localIndex); 
                resp.putParameter("deviceNo", smsEntry.getSender());
                return resp;
            }else{
                ResponseBundle resp = new ResponseBundle(Boolean.FALSE, devicePointId, "Database Error On Server!");
                resp.putParameter("act", "feedback");
                resp.putParameter("localRecId", localIndex); 
                resp.putParameter("deviceNo", smsEntry.getSender());
                return resp;
            }



        } else {
            return vBundle;
        }



    }
}
