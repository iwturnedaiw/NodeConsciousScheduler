/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

/**
 *
 * @author sminami
 */
class MaxAndMinMultiplicityCores {
    private CoreInfo maxCoreInfo;
    private CoreInfo minCoreInfo;

    MaxAndMinMultiplicityCores() {} 
    MaxAndMinMultiplicityCores(CoreInfo maxCoreInfo, CoreInfo minCoreInfo) {
        this.maxCoreInfo = maxCoreInfo;
        this.minCoreInfo = minCoreInfo;
    }
    public CoreInfo getMaxCoreInfo() {
        return maxCoreInfo;
    }

    public CoreInfo getMinCoreInfo() {
        return minCoreInfo;
    }

    public void setMaxCoreInfo(CoreInfo maxCoreInfo) {
        this.maxCoreInfo = maxCoreInfo;
    }

    public void setMinCoreInfo(CoreInfo minCoreInfo) {
        this.minCoreInfo = minCoreInfo;
    }
    
    
}
