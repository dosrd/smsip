/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols;

/**
 * to send out message to device
 * we will have 
 * @author dabar
 */
public class DefaultMadexPIMProtocolParser implements MadexPIMProtocolParser {
   
    public static final String RESET_PIN = "header,arr_length,point_id,generated_pin,old_pin,password,gps_long,gps_lat,name";
    public static final String SUBMIT_REPORT = "header,arr_length,point_id,generated_pin,longitude,latitude,report_month,report_year,R1,...,Rn";
    
    private String payloadData;

    public DefaultMadexPIMProtocolParser(String payloadData) {
        this.payloadData = payloadData;
        protocolCompliant = false;
        processProtocol();
    }
    private boolean protocolCompliant;
    private void processProtocol(){
        try {
            if(payloadData == null || payloadData.isEmpty()){
                protocolCompliant = false;
                return;
            }
            //we need to get out the header first
            //we split by comma which is madex standard separator
            String[] arrs = payloadData.split(";");
            if(arrs.length < 4){
                protocolCompliant = false;
                return;
                
            } 
            //we retrieve the header
            header = arrs[0];
            dataArrayLength = Integer.parseInt(arrs[1]);
            devicePointId = arrs[2];
            pin = arrs[3];
            
            
            bodyData = new String[dataArrayLength];
            for (int i = 4; i < arrs.length; i++) {
                String unit = arrs[i];
                bodyData[i-4] = unit;
            }
            protocolCompliant = true;
        } catch (Exception e) {
            protocolCompliant = false;
        }
        
    }
    
    public Boolean isProtocolCompliant(){
        return protocolCompliant;
    }
    private String header;
    private int dataArrayLength;
    private String[] bodyData;
    private String devicePointId;
    private String pin;

    @Override
    public String getHeader() {
        return header;
    }

    @Override
    public int getDataArrayLength() {
        return dataArrayLength;
    }

    @Override
    public String[] getDataArray() {
        return bodyData;
    }

    @Override
    public String getDevicePointId() {
        return devicePointId;
    }

    @Override
    public String getPIN() {
        return pin;
    }
    
    
}
