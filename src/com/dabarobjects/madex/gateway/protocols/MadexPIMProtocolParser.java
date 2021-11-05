/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols;

/**
 * The MADEX protocol defines the structure of the protocol
 * MADEX PIM protocol only
 * @author dabar
 */
public interface MadexPIMProtocolParser { 
    public String getHeader();
    public int getDataArrayLength();
    public String getDevicePointId();
    public String getPIN();
    public String[] getDataArray();
    public Boolean isProtocolCompliant();
}
