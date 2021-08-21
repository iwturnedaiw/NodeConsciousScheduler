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
public class CoreOCState {
    private boolean endingJobFlag;
    private int multiplicity;    

    CoreOCState() {};
    CoreOCState(boolean existEndingJob, int multiplicity) {
        this.endingJobFlag = existEndingJob;
        this.multiplicity = multiplicity;
    }

    public boolean isEndingJobFlag() {
        return endingJobFlag;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public void setEndingJobFlag(boolean endingJobFlag) {
        this.endingJobFlag = endingJobFlag;
    }

    public void setMultiplicity(int multiplicity) {
        this.multiplicity = multiplicity;
    }


    
}

