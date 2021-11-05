/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.services.phc;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.utils.CommonDateUtils;
import com.dabarobjects.data.utils.date.MonthlyReportTime;
import com.dabarobjects.madex.data.points.ListAllLocationsOperation;
import com.dabarobjects.madex.data.points.ListPointsForPointOperation;
import com.dabarobjects.madex.gateway.local.MadexLocal;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.madex.phc.data.MadexReport;
import com.dabarobjects.vts.data.points.results.ListResultsForPointOperation;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

/**
 * pick a point,pick a date, pick their reports and up them in comma delimited
 * fashion
 *
 * @author dabar
 */
public class ReportUplinkSyncPlug extends AbstractSpikeActivityPlug {

    private AbstractBridgeOperation executor;
    private MonthlyReportTime month;
    private String appUrl;
    private List<MadexLocation> locations;

    public ReportUplinkSyncPlug(AbstractBridgeOperation executor, MonthlyReportTime month, String webPath, List<MadexLocation> locations, String type) {
        super(true, "UPLINK-" + month.getReportMonth() + "" + month.getReportYear() + ":" +type);
        this.executor = executor;
        this.locations = locations;
        this.month = month;
        this.appUrl = webPath;
    }
     public ReportUplinkSyncPlug(AbstractBridgeOperation executor, MonthlyReportTime month, String webPath, MadexLocation location, String type) {
        super(true, "UPLINK-" + month.getReportMonth() + "" + month.getReportYear() + ":" +type);
        this.executor = executor;
        this.locations = new ArrayList<MadexLocation>();
        this.locations.add(location);
        this.month = month;
        this.appUrl = webPath;
    }

     
     
    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel, int row, int col) throws Exception {
        setStarted(true); 
        System.out.println("Executing for period..." + month);
        //List all points 
       
        //pick all months in the current year
        //iterate over each point
        for (MadexLocation madexLocation : locations) {
            System.out.println("Syncing Report For " + madexLocation);
            String pointCode = madexLocation.getPointCode();

            ListResultsForPointOperation 
                    listReportsOPs =
                    new 
                    ListResultsForPointOperation(pointCode, month.getDatePoint(), false);
            List<MadexReport> mreportList =
                    (List<MadexReport>) executor.read(listReportsOPs);
            if(mreportList.isEmpty())
                continue;
            String dataPart = ""
                    + month.getReportMonth()
                    + ";"
                    + month.getReportYear()
                    + ";"
                    + pointCode
                    + ";";
            StringBuilder buf = new StringBuilder();
            buf.append(dataPart);
            for (MadexReport madexReport : mreportList) {
               buf.append(madexReport.getSubmittedData()).append(";");
            }
            buf.deleteCharAt(buf.length() - 1);
            String dataPath = buf.toString();
            
            String data = "up=reports&cnt=" + dataPath;
            String resp = readWebActivity(appUrl, data);
            //System.out.println("Report Uplink For " + madexLocation + 
            //" for period: " + month + "; Response: " + resp);
            if(resp.trim().equalsIgnoreCase("OK")){
                System.out.println("Data Sync: " + month + "; Point: " + madexLocation + " ; Successful");
            }else{
                System.out.println("Data Sync: " + month + "; Point: " + madexLocation + " ; >>>Not Successful");
            }
            
        }
        setComplete(true);
        removeFromEventView(1);
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
