/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.local;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.utils.CommonDateUtils;
import com.dabarobjects.data.utils.date.MonthlyReportTime;
import com.dabarobjects.madex.data.points.*;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.phc.data.MadexConstants;
import com.dabarobjects.madex.gateway.services.phc.*;
import com.dabarobjects.madex.phc.data.MadexLocation;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;

/**
 * Keeps a cycle of 3 months for each report compilation. Any report that does
 * not arrive in 3 months since last month is left behind
 *
 * @author dabar
 */
public class MadexReportUplinkService extends AbstractSpikeActivityPlug {

    private MadexConfBean mConf;

    public MadexReportUplinkService(MadexConfBean mConf) {
        this.mConf = mConf;
    }

    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel, int row, int col) throws Exception {
        setStarted(true);
 
        com.dabarobjects.data.operations.Global.log(this, "Executing Data Sync Services...at: " + CommonDateUtils.formatDateNTime(new Date()));
        List<MonthlyReportTime> periods = MonthlyReportTime.//
                listTimesBtwIncl(MonthlyReportTime.lastNMonth(3), MonthlyReportTime.lastMonth());
        AbstractBridgeOperation executor = new AbstractBridgeOperation(mConf.getEntityBeansPath());
        String webUrl = mConf.getReportSynchPath();
        addNewEventView(new LocationUplinkSyncPlug(executor, webUrl));
        addNewEventView(new UsersAccessUplinkPlug(executor, webUrl));
        addNewEventView(new UsersFeedbackUplinkPlug(executor, webUrl));
        
        /**
        for (MonthlyReportTime monthlyReportTime : periods) {
            List<MadexLocation> govLocations = new ArrayList<MadexLocation>();
            MadexLocation nation = (MadexLocation) 
                    executor.//
                    read(new LoadRootPointOperation());

            List<MadexLocation> zoneLocations = (List<MadexLocation>) //
                    executor.//
                    read(new ListZonePointsOperation());
            List<MadexLocation> stateLocations = (List<MadexLocation>) //
                    executor.//
                    read(new ListStatePointsOperation());
            
            
            govLocations.add(nation);
            govLocations.addAll(zoneLocations);
            govLocations.addAll(stateLocations);

            ReportUplinkSyncPlug rSync1 = new ReportUplinkSyncPlug(executor, monthlyReportTime, webUrl, govLocations,"TOP");
            
            List<MadexLocation> clusterLocations = (List<MadexLocation>) //
                    executor.//
                    read(new ListClusterPointsOperation());
            
            ReportUplinkSyncPlug rSync2 = new ReportUplinkSyncPlug(executor, monthlyReportTime, webUrl, clusterLocations,"CLUSTER");
            
            List<MadexLocation> phcLocations = (List<MadexLocation>) //
                    executor.//
                    read(new ListPHCsPointsOperation());
            ReportUplinkSyncPlug rSync3 = new ReportUplinkSyncPlug(executor, monthlyReportTime, webUrl, phcLocations,"PHC");
            

            addNewEventView(rSync1);
            addNewEventView(rSync2);
            addNewEventView(rSync3);
        }
        **/
        System.out.println("Suspending Data Sync Services Till: ");
        
        pausePlugSignal(60);
        refreshEventPlug();
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
