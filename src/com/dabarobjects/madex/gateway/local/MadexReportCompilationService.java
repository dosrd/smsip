/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.local;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.utils.CommonDateUtils;
import com.dabarobjects.data.utils.date.MonthlyReportTime;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.services.phc.SumParentPointsForMonthPlug;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;

/**
 * Keeps a cycle of 3 months for each report compilation. Any report that does not arrive in 3 months since last month is left behind
 * @author dabar
 */
public class MadexReportCompilationService extends AbstractSpikeActivityPlug {

    private MadexConfBean mConf;
    public MadexReportCompilationService(MadexConfBean mConf) {
        this.mConf = mConf;
    }

    
    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel, int row, int col) throws Exception {
        setStarted(true);
        System.out.println("Running Compilation Engine...at: " + new Date());
        List<MonthlyReportTime> periods = MonthlyReportTime.//
                listTimesBtwIncl(MonthlyReportTime.lastNMonth(3), MonthlyReportTime.lastMonth());
        for (MonthlyReportTime monthlyReportTime : periods) {
            SumParentPointsForMonthPlug sumPoints = new SumParentPointsForMonthPlug(monthlyReportTime, mConf);
            addNewEventView(sumPoints);
        }
        Date nextHour = CommonDateUtils.nextFullHour(1);
        System.out.println("Suspended Compilation Engine Till: " + nextHour);
        pauseTill(nextHour);
        refreshEventPlug();
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
