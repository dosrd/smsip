/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.services;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.utils.CommonDateUtils;
import com.dabarobjects.data.utils.CommonFileUtils;
import com.dabarobjects.madex.gateway.local.MadexReportUplinkService;
import java.io.File;
import java.util.Date;
import javax.swing.ImageIcon;

/**
 *
 * @author dabar
 */
public class AutoBackupDatabasePlug extends AbstractSpikeActivityPlug {
    private int backupHour;
    private int backupMinute;

    public AutoBackupDatabasePlug(int backupHour, int backupMinute) {
        this.backupHour = backupHour;
        this.backupMinute = backupMinute;
    }
    

    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel, int row, int col) throws Exception {
        setStarted(true);
        if(backupHour == 0){
            backupHour = 22;
        }
        if(backupMinute == 0){
            backupMinute = 0;
        }
        Date timeOfBackup = CommonDateUtils.getNextAvailableTodayAt(backupHour, backupMinute);
        System.out.println("Pausing Data Backup Till " + timeOfBackup);
        pauseTill(timeOfBackup);
        System.out.println("Running Data Backup");
        String backupName = CommonDateUtils.getHHDDMMyyyy();
        File saveOut = new File(System.getProperty("user.home") + System.getProperty("file.separator")
                + ".madex" + System.getProperty("file.separator") + "backup" + System.getProperty("file.separator"));

        File zipOut = new File(System.getProperty("user.home") + System.getProperty("file.separator")
                + ".madex" + System.getProperty("file.separator") + "backup" + System.getProperty("file.separator")
                + "MADEX_" + backupName + "_DB_MySql.zip");
        if (!saveOut.exists()) {
            saveOut.mkdirs();
        }
        com.dabarobjects.data.operations.Global.log(this, "Backing Up Your MySQL Instance...");

        com.dabarobjects.data.operations.Global.log(this, "");
        String exec = "cmd.exe /c mysqldump.exe -u "
                + "madex"
                + " -p"
                + "$mAd3x810"
                + " "
                + "madex"
                + " > "
                + saveOut.getAbsolutePath() + System.getProperty("file.separator") + "MADEX_"+backupName+ "_DB_MySql.sql";
        com.dabarobjects.data.operations.Global.log(this, "Backing Up..." + exec);
        Process p = Runtime.getRuntime().exec(exec);

        com.dabarobjects.data.operations.Global.log(this, "Waiting For Process To Be Complete");
        int i = p.waitFor();
        com.dabarobjects.data.operations.Global.log(this, "Exit Value: " + i);
        com.dabarobjects.data.operations.Global.log(this, "Zipping File");
        boolean localBackupOk = CommonFileUtils.zipAllH2DBFiles(saveOut, zipOut.getAbsolutePath());
 
        if(localBackupOk){
            System.out.println("Successfully Backedup Data");
        }
        pausePlugSignal(10); 
        refreshEventPlug();
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
