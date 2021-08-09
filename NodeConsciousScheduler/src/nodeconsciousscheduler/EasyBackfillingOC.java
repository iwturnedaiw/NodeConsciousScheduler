/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author sminami
 */
public class EasyBackfillingOC extends EasyBackfilling {
    
    @Override
    protected ArrayList<VacantNode> canExecutableNodesAt(int currentTime, LinkedList<TimeSlice> timeSlices, Job job) {
        /* Return variable
           This have the node no. with # of free core.
        */
        ArrayList<VacantNode> nodes = new ArrayList<VacantNode>();
        
        /* Working Variable */
        ArrayList<VacantNode> vacantNodes = new ArrayList<VacantNode>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodes.add(new VacantNode(i, NodeConsciousScheduler.numCores));

        /* This is used for counting executable nodes */
        ArrayList<Integer> vacantNodeCount = new ArrayList<Integer>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodeCount.add(0);
        
        /* Calculate ppn */
        /* TODO: The case requiredCores ist not dividable  */
        int requiredCoresPerNode = job.getRequiredCores()/job.getRequiredNodes();
        if (job.getRequiredCores()%job.getRequiredNodes() != 0) ++requiredCoresPerNode;

        int jobId = job.getJobId();
        int startTime = currentTime;
        int expectedEndTime = startTime + job.getRequiredTime();
        int alongTimeSlices = 0;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);

            if ( (ts.getStartTime() <= startTime && startTime < ts.getEndTime()) || 
                 (ts.getStartTime() < expectedEndTime && expectedEndTime <= ts.getEndTime()) ||
                 (startTime <= ts.getStartTime() && ts.getEndTime() <= expectedEndTime) ) {
                //ts.printTsInfo();
                ++alongTimeSlices;
                for (int j = 0; j < ts.getNumNode(); ++j) {
                    int freeCores = ts.getAvailableCores().get(j);
                    VacantNode node = vacantNodes.get(j);
                    
                    assert node.getNodeNo() == j;

                    freeCores = min(freeCores, node.getFreeCores());
                    node.setFreeCores(freeCores);

                    int numCore = ts.getPpn();
//                    if (freeCores >= requiredCoresPerNode ) {
                    if (freeCores - requiredCoresPerNode >= -(NodeConsciousScheduler.M-1)*numCore) {
                        int cnt = vacantNodeCount.get(j);
                        vacantNodeCount.set(j, ++cnt);
                    }
                }
            }
        }

        if (alongTimeSlices == 0) return nodes;

        /*
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            VacantNode node = vacantNodes.get(i);
            int freeCores = node.getFreeCores();
            node.setFreeCores(freeCores/alongTimeSlices);
        }
        */

        /* If cnt == alongTimeSlices, the job is executable on the nodes along the timeSlices */        
        for (int i = 0; i < vacantNodeCount.size(); ++i) {
            int cnt = vacantNodeCount.get(i);
            if (cnt == alongTimeSlices) {
                VacantNode node = vacantNodes.get(i);
                assert node.getNodeNo() == i;
                nodes.add(node);
            }
        }
        
        return nodes;
    }
}
