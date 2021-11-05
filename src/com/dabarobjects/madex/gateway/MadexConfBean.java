/*
 * To change this template\nchoose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway;

import java.io.Serializable;

/**
 * com.dabarobjects.madex.gateway.MadexConfBean
 * @author dabar
 */
public class MadexConfBean implements Serializable {

    private String mode; //TEST\nLIVE
    private int threads; //default 40
    private String entityBeansPath; //phc-madex-dataconfig 
    private String defMessHeader; //MADEX
    private int reportCompileCycleinMonths; //3
    private int reportSyncCycleinMonths;//3
    private String backupTime;//22,00
    private String reportSynchPath;//http://

    public String getDefMessHeader() {
        return defMessHeader;
    }

    public void setDefMessHeader(String defMessHeader) {
        this.defMessHeader = defMessHeader;
    }

    

    public String getEntityBeansPath() {
        return entityBeansPath;
    }

    public void setEntityBeansPath(String entityBeansPath) {
        this.entityBeansPath = entityBeansPath;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getReportCompileCycleinMonths() {
        return reportCompileCycleinMonths;
    }

    public void setReportCompileCycleinMonths(int reportCompileCycleinMonths) {
        this.reportCompileCycleinMonths = reportCompileCycleinMonths;
    }

    public int getReportSyncCycleinMonths() {
        return reportSyncCycleinMonths;
    }

    public void setReportSyncCycleinMonths(int reportSyncCycleinMonths) {
        this.reportSyncCycleinMonths = reportSyncCycleinMonths;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public String getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(String backupTime) {
        this.backupTime = backupTime;
    }

    public String getReportSynchPath() {
        return reportSynchPath;
    }

    public void setReportSynchPath(String reportSynchPath) {
        this.reportSynchPath = reportSynchPath;
    }

    @Override
    public String toString() {
        return "MadexConfBean{" + "\nmode=" + mode + "\nthreads=" + threads + "\nentityBeansPath=" + entityBeansPath + "\ndefMessHeader=" + defMessHeader + "\nreportCompileCycleinMonths=" + reportCompileCycleinMonths + "\nreportSyncCycleinMonths=" + reportSyncCycleinMonths + "\nbackupTime=" + backupTime + "\nreportSynchPath=" + reportSynchPath + "\n}";
    }
    
    
    
}
