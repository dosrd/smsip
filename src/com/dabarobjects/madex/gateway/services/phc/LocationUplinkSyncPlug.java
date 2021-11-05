/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.services.phc;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.madex.data.points.ListAllLocationsOperation;
import com.dabarobjects.madex.data.points.LoadParentForPointOperation;
import com.dabarobjects.madex.gateway.local.MadexLocal;
import com.dabarobjects.madex.phc.data.MadexAgent;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.vts.data.points.agents.LoadAgentForPointOperation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

/**
 *
 * @author dabar
 */
public class LocationUplinkSyncPlug extends AbstractSpikeActivityPlug {

    private AbstractBridgeOperation executor;
    private String appUrl;
    private List<MadexLocation> locations;

    public LocationUplinkSyncPlug(AbstractBridgeOperation executor, String appUrl) {
        this.executor = executor;
        this.appUrl = appUrl;
        locations = new ArrayList<MadexLocation>();

    }
    private MadexLocation defLocation;

    public LocationUplinkSyncPlug(AbstractBridgeOperation executor, String appUrl, MadexLocation defLocation) {
        this.executor = executor;
        this.appUrl = appUrl;
        this.defLocation = defLocation;
    }

   

    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel,
            int row, int col) throws Exception {
        setStarted(true);
        if (defLocation == null) {
            List<MadexLocation> ls = (List<MadexLocation>) executor.read(new ListAllLocationsOperation());
            locations.addAll(ls);
        } else {
            locations.add(defLocation);
        }


        //List All Location Points

        for (MadexLocation madexLocation : locations) {
            System.out.println("Uplinking Loc: " + madexLocation);
            MadexAgent ma = (MadexAgent) executor.//
                    read(new LoadAgentForPointOperation(madexLocation.getId()));
            //pointCode,pointPin,pointName,pointAddress,pointOfficerName,pointOfficerMobile,pointLevel,pointGpsLong,pointGPSLat
            MadexLocation parent = (MadexLocation) executor.//
                    read(new LoadParentForPointOperation(madexLocation.getId()));
            String parentPointCode = "";
            if (parent != null) {
                parentPointCode = parent.getPointCode();
            }
            String agentPin = "0000";
            String agentName = "NPHCDA";
            String agentMobile = "080";
            String pass = "";

            if (ma != null) {
                agentPin = ma.getAgentPin();
                agentName = ma.getAgentName();
                agentMobile = ma.getAgentMobile();
                pass = ma.getLocalPassword();
            }

            String dataUrl = ""
                    + madexLocation.getPointCode()
                    + ";"
                    + agentPin
                    + ";"
                    + madexLocation.getPointName()
                    + ";"
                    + madexLocation.getLgaAxis()
                    + ";"
                    + agentName
                    + ";"
                    + agentMobile
                    + ";"
                    + madexLocation.getPointDepth()
                    + ";"
                    + madexLocation.getGpsLongitude()
                    + ";"
                    + madexLocation.getGpsLatitude()
                    + ";"
                    + parentPointCode
                    + ";"
                    + pass
                    + ";"
                    + madexLocation.getPointType();
            String data = "up=points&cnt=" + dataUrl;
            String response = readWebActivity(appUrl, data);
            System.out.println("Location Uplink: " + madexLocation + ": " + response);
        }
        setComplete(true);
        removeFromEventView(2);
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
