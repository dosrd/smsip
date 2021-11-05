/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.protocols.bindings.phc;

import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.gateway.MadexGatewayProcessingUnit;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;

/**
 *
 * @author dabar
 */
public class UpdateNewPINMadexProtocol implements MadexGatewayProcessingUnit{

    @Override
    public String getHeaderBinder() {
        return "b";
    }

    @Override
    public ResponseBundle processPayloadData(String devicePointId, String pin, String[] dataArray, 
    int arrayLength, AbstractBridgeOperation dataAccess,MadexConfBean mConf,MadexSMSEntry smsEntry) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
