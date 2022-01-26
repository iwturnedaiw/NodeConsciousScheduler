/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.Objects;

/**
 *
 * @author sminami
 */
public class JobMatching {
    private final int victimJobId;
    private final int opponentJobId;

    public JobMatching(int victimJobId, int opponentJobId) {
        this.victimJobId = victimJobId;
        this.opponentJobId = opponentJobId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JobMatching) {
            JobMatching key = (JobMatching) obj;
            return this.victimJobId == key.victimJobId && this.opponentJobId == key.opponentJobId;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(victimJobId, opponentJobId);
    }

}