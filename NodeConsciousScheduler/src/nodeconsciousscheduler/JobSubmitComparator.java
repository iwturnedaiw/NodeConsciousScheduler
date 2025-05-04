/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.Comparator;

/**
 *
 * @author sminami
 */
public class JobSubmitComparator implements Comparator<Job> {
    @Override
    public int compare(Job obj1, Job obj2) {
        if (obj1.getSubmitTime() < obj2.getSubmitTime()) {
            return -1;
        }
        if (obj1.getSubmitTime() > obj2.getSubmitTime()) {
            return 1;
        }
        return 0;
    }
}
