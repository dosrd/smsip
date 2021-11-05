/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.madex.MadexPacket;
import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultSaveDataOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.madex.phc.data.MadexOutbound;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.spikeservice.SpikeService;
import com.dabarobjects.spikeservice.connect.SpikeResponseMap;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author dabar
 */
public class MadexBackgroundSMSResponseService extends AbstractSpikeActivityPlug {

    private final String randId;
    private final String sender;
    private AbstractBridgeOperation access;
    private MadexSMSEntry message;
    private ApplicationContext springCtx;
    private ResponseBundle responseBundle;
    private MadexConfBean mConf;

    public MadexBackgroundSMSResponseService(
            String randId,
            String sender,
            AbstractBridgeOperation access,
            MadexSMSEntry message,
            ApplicationContext contxt,
            ResponseBundle responseBundle,MadexConfBean mConf) {
        super(true, randId);
        this.randId = randId;
        this.sender = sender;
        this.access = access;
        this.message = message;
        this.springCtx = contxt;
        this.responseBundle = responseBundle;
        this.mConf = mConf;
    }

    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel, int row, int col) throws Exception {
        setStarted(true);
        setMessage("Pushing SMS Response...");
        SpikeService con = (SpikeService) springCtx.getBean("smsspike");
        SpikeResponseMap lresp = con.loginAccount();
        System.out.println("Login Response: " + lresp);
        String privateMessage = (String) responseBundle.getParameter("PRIVATE_MESSAGE");
        String privateMobileNo = (String) responseBundle.getParameter("PRIVATE_MOBILE");
        String responseMessageBatch = responseBundle.convertForTransport();


        
        
        
        
        MadexPacket mPacket = new MadexPacket(responseMessageBatch);
        List<String> packets = mPacket.getParts();
        //Collections.reverse(packets);
        for (String responseMessage : packets) {
            MadexOutbound outbound1 = new MadexOutbound();
            outbound1.setSender("MADEX!");  
            outbound1.setScheduleDate(new Date());
            outbound1.setMessage(responseMessage);
            outbound1.setDestination(sender);
            outbound1.setMessageId(randId);  

            SpikeResponseMap resp = con.sendBasicMessageNow(responseMessage + "ID:" + randId + ";", sender, "MADEX!");
            System.out.println("Sending SMS Response: " + responseMessage + ", " + resp);
            outbound1.setSent(resp.isSuccess());

            if (resp.isSuccess()) {
                message.setFeedbackSMSSent(Boolean.TRUE);
                outbound1.setSentDate(new Date());
                outbound1.setSent(Boolean.TRUE);
                access.write(new DefaultSaveDataOperation<MadexOutbound>(outbound1));
            } else {
                outbound1.setSent(Boolean.FALSE);
                message.setFeedbackSMSSent(Boolean.FALSE);
                access.write(new DefaultSaveDataOperation<MadexOutbound>(outbound1));
                addNewEventView(new DelayedOutboundSMSPushPlug(springCtx));
            }
            
            
        }


 

        String defHeader = "MADEX-TEST";
        if(mConf != null){
            defHeader = mConf.getDefMessHeader();
        }
        if (privateMobileNo != null && privateMessage != null) {
            SpikeResponseMap resp1 = con.sendBasicMessageNow(privateMessage, privateMobileNo, defHeader);
            System.out.println("Private Response: " + resp1);
            MadexOutbound outbound2 = new MadexOutbound();
            outbound2.setSender(defHeader);
            outbound2.setScheduleDate(new Date());
            outbound2.setMessage(privateMessage);
            outbound2.setDestination(privateMobileNo);
            outbound2.setMessageId(randId);
            outbound2.setSent(resp1.isSuccess());
            if (resp1.isSuccess()) {
                message.setFeedbackSMSSent(Boolean.TRUE);
                outbound2.setSentDate(new Date());
                access.write(new DefaultSaveDataOperation<MadexOutbound>(outbound2));
            } else {
                outbound2.setSent(Boolean.FALSE);
                access.write(new DefaultSaveDataOperation<MadexOutbound>(outbound2));
                addNewEventView(new DelayedOutboundSMSPushPlug(springCtx));
            }
            
        }

        access.write(new DefaultUpdateDataOperation<MadexSMSEntry>(message));
        setComplete(true);
        removeFromEventView(1);
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
