/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.madex.phc.data.MadexOutbound;
import com.dabarobjects.madex.phc.data.outbound.ListAllUnsentOutboundOperation;
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
public class DelayedOutboundSMSPushPlug extends AbstractSpikeActivityPlug {

    private ApplicationContext contxt;
    private AbstractBridgeOperation access;

    public DelayedOutboundSMSPushPlug(ApplicationContext contxt) {
        this.contxt = contxt;
    }

    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel, int row, int col) throws Exception {
        setStarted(true);
        SpikeService con = (SpikeService) contxt.getBean("smsspike");
        this.access = (AbstractBridgeOperation) contxt.getBean("databridge");
        SpikeResponseMap login = con.loginAccount();
        System.out.println("login: " + login);
        if (login.isSuccess()) {
            System.out.println("Checking For Delayed Outbound Message..." + new Date());
            List<MadexOutbound> unsents = (List<MadexOutbound>) //
                    access.read(new ListAllUnsentOutboundOperation());
            if (con.serviceReachable()) {
                for (MadexOutbound madexOutbound : unsents) {

                    if(madexOutbound.getSent()){
                        continue;
                    }
                    String header = madexOutbound.getSender();
                    String destination = madexOutbound.getDestination();
                    String outMessage = madexOutbound.getMessage();
                    SpikeResponseMap resp = con.sendBasicMessageNow(outMessage, destination, header);
                    System.out.println("Sending Delayed SMS Response: " + outMessage + ", " + resp);
                    
                    if (resp.isSuccess()) {
                        madexOutbound.setSent(Boolean.TRUE);
                        madexOutbound.setSentDate(new Date());
                    }
                    System.out.println("Updating Outbound " + madexOutbound + ", Sent: " + madexOutbound.getSender());
                    access.write(new DefaultUpdateDataOperation<MadexOutbound>(madexOutbound));
                }
                unsents.clear();
                unsents = null;
                setComplete(true);
                removeFromEventView(1);
                return;
            }else{
                con.logoutAccount();
            }

        } else {
            System.out.println("Could Not Event Access The Web... " + new Date());
        }

        pausePlugSignal(20);
        refreshEventPlug();
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
