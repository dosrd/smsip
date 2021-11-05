/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;

/**
 * a registered header used to identify a madex protocol element
 * @author dabar
 */
public interface MadexGatewayProcessingUnit {
    //must be a single length value
    public String getHeaderBinder();
    public ResponseBundle processPayloadData(String devicePointId, 
            String pin, String[] dataArray, int arrayLength, AbstractBridgeOperation dataAccess,MadexConfBean mConf,MadexSMSEntry smsEntry);
    
}
