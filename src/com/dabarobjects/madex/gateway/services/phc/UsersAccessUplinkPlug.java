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
import com.dabarobjects.madex.phc.data.MadexAdminUser;
import com.dabarobjects.madex.phc.data.MadexAgent;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.madex.phc.data.auth.ListAllDataUsersOperation;
import com.dabarobjects.madex.phc.data.auth.ListMadexAdminUserOperation;
import com.dabarobjects.vts.data.points.agents.LoadAgentForPointOperation;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import javax.swing.ImageIcon;

/**
 *
 * @author dabar
 */
public class UsersAccessUplinkPlug extends AbstractSpikeActivityPlug {

    private AbstractBridgeOperation executor;
    private String appUrl;

    public UsersAccessUplinkPlug(AbstractBridgeOperation executor, String appUrl) {
        this.executor = executor;
        this.appUrl = appUrl;
    }

    

    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel,
            int row, int col) throws Exception {
        setStarted(true);


        //List All Location Pointse new
        List<MadexAdminUser> locations = (List<MadexAdminUser>) executor.read(new ListAllDataUsersOperation());
      
        for (MadexAdminUser mAdmin : locations) {

            String username = mAdmin.getUsername();
            String password = mAdmin.getPassword();
            Integer accessLevel = mAdmin.getAccessLevel();
            String pointCode = mAdmin.getPointCode();
            String emailAddress = mAdmin.getEmailAddress();
            String mobileNo = mAdmin.getMobileNoHome();

            String userInfo = mAdmin.getFirstname() + " " + mAdmin.getLastname();


            String dataUrl = ""
                    + username
                    + ";"
                    + password
                    + ";"
                    + accessLevel
                    + ";"
                    + pointCode
                    + ";"
                    + emailAddress
                    + ";"
                    + mobileNo + ";" + userInfo;
            String data = "up=user&cnt=" + dataUrl;
            String response = readWebActivity(appUrl, data);
            System.out.println("UserAccess Uplink: " + mAdmin + ": " + response);
        }
        setComplete(true);
        pausePlugSignal(120);
        refreshEventPlug();
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
