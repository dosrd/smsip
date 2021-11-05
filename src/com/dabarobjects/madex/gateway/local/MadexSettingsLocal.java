/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.local;

/**
 *
 * @author dabar
 */
public class MadexSettingsLocal {
    private String defaultResponseSubject;
    private String errorDumpFile;
    private int locationSyncCycle;
    private int dataSyncCycle;
    private int aggregationSpan;

    public MadexSettingsLocal() {
    }

    public int getAggregationSpan() {
        return aggregationSpan;
    }

    public void setAggregationSpan(int aggregationSpan) {
        this.aggregationSpan = aggregationSpan;
    }

    public int getDataSyncCycle() {
        return dataSyncCycle;
    }

    public void setDataSyncCycle(int dataSyncCycle) {
        this.dataSyncCycle = dataSyncCycle;
    }

    public String getDefaultResponseSubject() {
        return defaultResponseSubject;
    }

    public void setDefaultResponseSubject(String defaultResponseSubject) {
        this.defaultResponseSubject = defaultResponseSubject;
    }

    public String getErrorDumpFile() {
        return errorDumpFile;
    }

    public void setErrorDumpFile(String errorDumpFile) {
        this.errorDumpFile = errorDumpFile;
    }

    public int getLocationSyncCycle() {
        return locationSyncCycle;
    }

    public void setLocationSyncCycle(int locationSyncCycle) {
        this.locationSyncCycle = locationSyncCycle;
    }
    
    
    
    
    
}
