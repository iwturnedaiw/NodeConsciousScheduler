/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sminami
 */
public interface EventHandler {
    ArrayList<Event> handle(Event ev);

}

class Submission implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
//        newEvents = 
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        evs = NodeConsciousScheduler.sim.getSche().scheduleJobsOnSubmission(ev);
        
        return evs;
    }
}

class Start implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        // Add the Job to Executing job List
        // Executing job list is declared in Simulator
//        evs = NodeConsciousScheduler.sim.getSche().schedule(ev);
        
        Job job = ev.getJob();
        job.setStartTime(ev.getOccurrenceTime());
        job.setPreviousMeasuredTime(ev.getOccurrenceTime());
        job.setWaitTime(job.getStartTime() - job.getSubmitTime());
        
        NodeConsciousScheduler.sim.getExecutingJobList().add(job);
        
        return evs;
    }
}

class End implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
//        newEvents = 
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        /* Fix the finish time */
        Job job = ev.getJob();
        int jobId = job.getJobId();
        int currentTime = ev.getOccurrenceTime();
        assert currentTime == job.getEndEventOccuranceTimeNow();
        int previousMeasuredTime = job.getPreviousMeasuredTime();
        int mostRecentRunningTime = currentTime - previousMeasuredTime;
        int OCStateLevel = job.getOCStateLevel();
        if (OCStateLevel == 1) {
            int runningTimeDed = job.getRunningTimeDed();
            job.setRunningTimeDed(runningTimeDed + mostRecentRunningTime);
        }
        else {
            int runningTimeOC = job.getRunningTimeOC();
            job.setRunningTimeOC(runningTimeOC + mostRecentRunningTime);
        }
        int runningTimeDed = job.getRunningTimeDed();
        int runningTimeOC = job.getRunningTimeOC();
        int runningTime = runningTimeDed + runningTimeOC;
        int finishedTime = runningTime + job.getStartTime();
        job.setFinishedTime(finishedTime);
        
        // Output the result
        NodeConsciousScheduler.sim.outputResult(job);

        // Erase the job from executing job list
        NodeConsciousScheduler.sim.getExecutingJobList().remove(job);
        // Add the job completed List
        NodeConsciousScheduler.sim.getCompletedJobList().add(job);

        // Resource refill        
        freeResources(job);

        // Again call scheduling
        evs = NodeConsciousScheduler.sim.getSche().scheduleJobsOnEnd(ev);

        
        return evs;
    }

    private void freeResources(Job job) {
        int jobId = job.getJobId();
        ArrayList<UsingNode> usingNodesList = job.getUsingNodesList();

        ArrayList<NodeInfo> AllNodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo();
        for (int i = 0; i < usingNodesList.size(); ++i) {
            UsingNode usingNode = usingNodesList.get(i);
            int nodeNo = usingNode.getNodeNum();
            NodeInfo nodeInfo = AllNodeInfo.get(nodeNo);
            int numFreeCores = nodeInfo.getNumFreeCores();
            int numOccupiedCores = nodeInfo.getNumOccupiedCores();
            assert nodeInfo.getExecutingJobIds().contains(jobId);

            int numUsingCores = usingNode.getNumUsingCores();
            
            /* Number of free/occupied Cores*/
            numFreeCores += numUsingCores;
            assert numFreeCores <= nodeInfo.getNumCores();
            assert numFreeCores >= -(NodeConsciousScheduler.M-1)*nodeInfo.getNumCores();
            nodeInfo.setNumFreeCores(numFreeCores);
            numOccupiedCores -= numUsingCores;
            nodeInfo.setNumOccupiedCores(numOccupiedCores);
            
            /* Each core */
            ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < nodeInfo.getNumCores(); ++j) {
                CoreInfo eachCore = occupiedCores.get(j);
                ArrayList<Integer> jobList = eachCore.getJobList();
                for (int k = 0; k < jobList.size(); ++k) {
                    if (jobList.get(k) == jobId) {
                        jobList.remove(k);
                    }
                }
            }
            
            nodeInfo.getExecutingJobIds().remove(jobId);            
            // TODO:
            // Want to free usingNode
        }

    }
}

class DeleteFromBeginning implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
//        newEvents = 
        
        
        
        NodeConsciousScheduler.sim.getEvq().deleteEventFromBeginning(ev);
        
        return new ArrayList<Event>();
    }
}

class DeleteFromEnd implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
//        newEvents = 
        
        
        
        NodeConsciousScheduler.sim.getEvq().deleteEventFromEnd(ev);
        
        return new ArrayList<Event>();
    }
}
