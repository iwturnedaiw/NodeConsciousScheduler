/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;

/**
 *
 * @author sminami
 */
public interface EventHandler {
    ArrayList<Event> handle(Event ev);

}

class Submission implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType());
//        newEvents = 
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        evs = NodeConsciousScheduler.sim.getSche().scheduleJobsOnSubmission(ev);
        
        return evs;
    }
}

class Start implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType());
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        // Add the Job to Executing job List
        // Executing job list is declared in Simulator
//        evs = NodeConsciousScheduler.sim.getSche().schedule(ev);
        
        Job job = ev.getJob();
        job.setWaitTime(job.getStartTime() - job.getSubmitTime());
        
        NodeConsciousScheduler.sim.getExecutingJobList().add(job);
        
        return evs;
    }
}

class End implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType());
//        newEvents = 
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        // Erase the job from executing job list
        // Resource refill
        // Again call scheduling
        // Add the job completed List

        Job job = ev.getJob();
        int runningTime = job.getActualExecuteTime();
        job.setRunningTimeDed(runningTime);
        
        int finishedTime = runningTime + job.getStartTime();
        job.setFinishedTime(finishedTime);

        
        NodeConsciousScheduler.sim.outputResult(job);
        NodeConsciousScheduler.sim.getExecutingJobList().remove(job);
        NodeConsciousScheduler.sim.getCompletedJobList().add(job);
        freeResources(job);
        
        evs = NodeConsciousScheduler.sim.getSche().scheduleJobsOnEnd(ev);
        
        return evs;
    }

    private void freeResources(Job job) {
        int jobId = job.getJobId();
        ArrayList<UsingNodes> usingNodesList = job.getUsingNodesList();

        ArrayList<NodeInfo> AllNodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo();
        for (int i = 0; i < usingNodesList.size(); ++i) {
            UsingNodes usingNode = usingNodesList.get(i);
            int nodeNo = usingNode.getNodeNum();
            NodeInfo nodeInfo = AllNodeInfo.get(nodeNo);
            int numFreeCores = nodeInfo.getNumFreeCores();
            int numOccupiedCores = nodeInfo.getNumOccupiedCores();

            int numUsingCores = usingNode.getNumUsingCores();
            
            /* Number of free/occupied Cores*/
            numFreeCores += numUsingCores;
            nodeInfo.setNumFreeCores(numFreeCores);
            numOccupiedCores -= numUsingCores;
            nodeInfo.setNumOccupiedCores(numOccupiedCores);
            
            /* Each core */
            ArrayList<Integer> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < nodeInfo.getNumCores(); ++j) {
                if (occupiedCores.get(j) == jobId) {
                    occupiedCores.set(j, Constants.UNUSED);
                }
            }
            // TODO:
            // Want to free usingNode
        }

    }
}
