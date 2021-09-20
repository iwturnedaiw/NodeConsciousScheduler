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
class TimeSlicesAndNodeInfoConsistency {
    protected boolean consistency;
    protected boolean sameEndEventFlag;

    public TimeSlicesAndNodeInfoConsistency(boolean consistency, boolean sameEndEventFlag) {
        this.consistency = consistency;
        this.sameEndEventFlag = sameEndEventFlag;
    }
    
    public boolean isConsistency() {
        return consistency;
    }

    public boolean isSameEndEventFlag() {
        return sameEndEventFlag;
    }

    public void setConsistency(boolean consistency) {
        this.consistency = consistency;
    }

    public void setSameEndEventFlag(boolean sameEndEventFlag) {
        this.sameEndEventFlag = sameEndEventFlag;
    }
    
}
