/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway;

import com.dabarobjects.data.utils.CommonRandomNoUtils;
import java.io.Serializable;

/**
 *
 * @author dabar
 */
public final class MadexProcessingResponse implements Serializable{
    private final Boolean successful;
    private final String processCompletionId;

    public final static MadexProcessingResponse OK = new MadexProcessingResponse(Boolean.TRUE);
    public final static MadexProcessingResponse ERROR = new MadexProcessingResponse(Boolean.FALSE);

    private String remarks;
    public MadexProcessingResponse(Boolean successful) {
        this.successful = successful;
        this.processCompletionId = CommonRandomNoUtils.generateKey(6);
        remarks = "";
    }
    
    public MadexProcessingResponse(Boolean successful,  String remarks) {
        this.successful = successful;
        this.processCompletionId = CommonRandomNoUtils.generateKey(6);
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getProcessCompletionId() {
        return processCompletionId;
    }

    public Boolean getSuccessful() {
        return successful;
    }
    
}
