/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols;

/**
 *
 * @author dabar
 */
public class BasicHashtagProtocolParser implements MadexPIMProtocolParser{
    private String payloadData;
    private boolean protocolCompliant;
    private String headerId;
    private String message;
    private String senderNo;

    public BasicHashtagProtocolParser(String payloadData, String senderNo) {
        this.payloadData = payloadData;
        protocolCompliant  = false;
        this.senderNo = senderNo;
        headerId = "";
        message = "";
        processProtocol();
        
    }
    
    private void processProtocol(){
        if(payloadData.isEmpty()){
            protocolCompliant = false;
        }
        if(payloadData.startsWith("#")){
            int indexOfSpace = payloadData.indexOf(" ");
            if(indexOfSpace == -1){
                message = "";
                headerId = payloadData.replaceFirst("#", "");
                protocolCompliant = true;
                return;
            }
            headerId = payloadData.substring(1, indexOfSpace);
            if(headerId.isEmpty() || headerId.trim().isEmpty()){
                protocolCompliant = false;
                return;
            }
            if(headerId.length() > 12){
                protocolCompliant = false;
                return;
            }
            protocolCompliant = true;
            int startIndex = indexOfSpace + 1;
            
            message = payloadData.substring(startIndex);
            
            
        }else{
            protocolCompliant = false;
        }
    }

    @Override
    public String getHeader() {
        return headerId;
    }

    @Override
    public int getDataArrayLength() {
        return 1;
    }

    @Override
    public String getDevicePointId() {
        return senderNo;
    }

    @Override
    public String getPIN() {
        return "";
    }

    @Override
    public String[] getDataArray() {
        return new String[]{message};
    }

    @Override
    public Boolean isProtocolCompliant() {
        return protocolCompliant;
    }
    
}
