/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.services.phc;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.data.utils.CommonDateUtils;
import com.dabarobjects.data.utils.CommonRandomNoUtils;
import com.dabarobjects.madex.gateway.local.MadexLocal;
import com.dabarobjects.madex.phc.data.MadexFeedback;
import com.dabarobjects.madex.phc.data.auth.ListAllUnsynchedFeedbackOperation;
import com.dabarobjects.madex.phc.data.auth.ListMadexAdminUserOperation;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;

/**
 *
 * @author dabar
 */
public class UsersFeedbackUplinkPlug extends AbstractSpikeActivityPlug {

    private AbstractBridgeOperation executor;
    private String appUrl;
    private List<MadexFeedback> feedbacks;

    public UsersFeedbackUplinkPlug(AbstractBridgeOperation executor, String appUrl) {
        this.executor = executor;
        this.appUrl = appUrl;
        feedbacks = new ArrayList<MadexFeedback>();
    }
    private MadexFeedback mFeedback;

    public UsersFeedbackUplinkPlug(AbstractBridgeOperation executor, String appUrl, MadexFeedback mFeedback) {
        this.executor = executor;
        this.appUrl = appUrl;
        this.mFeedback = mFeedback;
    }

   

    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel,
            int row, int col) throws Exception {
        setStarted(true);

        if (mFeedback == null) {
            List<MadexFeedback> feedbacks1 = (List<MadexFeedback>) executor.read(new ListAllUnsynchedFeedbackOperation());
            feedbacks.addAll(feedbacks1);
        } else {
            feedbacks.add(mFeedback);
        }
        //List All Location Points

        for (MadexFeedback mAdmin : feedbacks) {


            System.out.println("Uplinking " + mAdmin);
            String clusterCode = mAdmin.getMadexLocationId();
            String subject = mAdmin.getFeedbackType();
            String feedbackDetails = mAdmin.getMessageContent();
            Date feedbackDate = mAdmin.getMessageDate();
            String feedbackid = CommonRandomNoUtils.generateKey(12);


            String feedbackDateStr = CommonDateUtils.formatDateTime(feedbackDate);

            if (clusterCode == null || clusterCode.isEmpty()) {
                mAdmin.setSyncOnline(Boolean.TRUE);
                boolean ok = executor.write(new DefaultUpdateDataOperation<MadexFeedback>(mAdmin));
                 
                continue;
            }
            if (feedbackDetails == null || feedbackDetails.isEmpty()) {
                mAdmin.setSyncOnline(Boolean.TRUE);
                boolean ok = executor.write(new DefaultUpdateDataOperation<MadexFeedback>(mAdmin));
                 
                continue;
            }
            String dataUrl = ""
                    + clusterCode
                    + ";"
                    + subject
                    + ";"
                    + feedbackDetails.replaceAll(";", "")
                    + ";"
                    + feedbackDateStr
                    + ";"
                    + feedbackid;
            String data = "up=feedback&cnt=" + dataUrl;
            String response = readWebActivity(appUrl, data);
            System.out.println(data + "=" + response + " OK Lenght: " + "OK".length() + ", Response Lenght: " + response.trim().length());
            
            if (response.trim().equalsIgnoreCase("OK")) {
                mAdmin.setSyncOnline(Boolean.TRUE);
                boolean ok = executor.write(new DefaultUpdateDataOperation<MadexFeedback>(mAdmin));
                System.out.println("Feedback Updated: " + ok);
            }else{
                System.out.println("Not Equal to OK");
            }
            //System.out.println("Location Uplink: " + madexLocation + ": " + response);
        }
        setComplete(true);

        removeFromEventView(1);
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
