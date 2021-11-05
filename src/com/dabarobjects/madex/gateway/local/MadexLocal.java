/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.local;

import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.madex.gateway.DelayedOutboundSMSPushPlug;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.phc.data.MadexConstants;
import com.dabarobjects.madex.gateway.services.AutoBackupDatabasePlug;
import com.dabarobjects.madex.gateway.services.phc.LocationUplinkSyncPlug;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 *
 * @author dabar
 */
public class MadexLocal {

    //public final static String WEB_PATH = "http://localhost:8080/report/gate";//http://nphcdamadex.net:8080/report/gate";

    /**
     * Testing Mode - data uplinks are turned off, default response subject is
     * set to MADEX-TEST against MADEX!
     *
     * @param args
     */
    public static void main(String[] args) {
        ApplicationContext contxt = new FileSystemXmlApplicationContext("madex-beans.xml");
        
        String mode = "test";
        if(args.length == 0){
            mode = "test";
        }
        if(args.length > 0){
            String param1 = args[0];
            if(param1.equalsIgnoreCase("-live")){
                mode = "live";
            }else{
                mode = "test";
            }
        }
        MadexConfBean mConf = (MadexConfBean) contxt.getBean(mode);
        
        System.out.println("Using Conf...." + mConf);
        MadexConstants.setResource(mConf.getEntityBeansPath());

        System.out.println("Opening MADEX Terminal Grid..."+ MadexConstants.EMS_RESOURCE);
        int nThreads = mConf.getThreads();
        if (nThreads == 0) {
            nThreads = 20;
        }
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        SpikeActivityPlugExecutorModel model =
                SpikeActivityPlugExecutorModel.getInstance();
        model.setSystemThreadService(executor);
       
        model.addEventPlugAtTop(new MadexProtocolGateway(contxt, mConf));
        model.addEventPlugAtBottom(new DelayedOutboundSMSPushPlug(contxt));
        
        
        String tmode = mConf.getMode();
        
        String backupTimeStr = mConf.getBackupTime();
        if(!backupTimeStr.equalsIgnoreCase("0") && !tmode.equalsIgnoreCase("TEST")){
            String[] timeParts = backupTimeStr.split(",");
             
            int backupHour = 22;
            int minuteToStart = 0;
            if(timeParts.length == 1){
                //just hours
                backupHour = Integer.parseInt(timeParts[0]);
                 
                
            }
            if(timeParts.length == 2){
                //hours,minute
                backupHour = Integer.parseInt(timeParts[0]);
                minuteToStart = Integer.parseInt(timeParts[1]);
            }
            model.addEventPlugAtBottom(new AutoBackupDatabasePlug(backupHour, minuteToStart));
        }
        
        model.addEventPlugAtBottom(new MadexReportCompilationService(mConf));
 
       model.addEventPlugAtBottom(new MadexReportUplinkService(mConf));
        


    }
}
