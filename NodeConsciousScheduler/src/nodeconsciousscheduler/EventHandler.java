/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static nodeconsciousscheduler.IntActivate.printThrowDeactivateEvent;

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
        job.setPreviousMigratingTime(ev.getOccurrenceTime());
        
        NodeConsciousScheduler.sim.getExecutingJobList().add(job);
        
        // TODO
        // Update activated cores table if it is not int.
        boolean interactiveJob = job.isInteracitveJob();
        if (interactiveJob) {
            int currentTime = ev.getOccurrenceTime();
            int prologTime = job.getPrologTime();
            int activationTime = currentTime + prologTime;
            printThrowActivateEvent(currentTime, activationTime, job, EventType.INT_ACTIVATE, 1);
            evs.add(new Event(EventType.INT_ACTIVATE, activationTime, job));
        }
        
        return evs;
    }
    private void printThrowActivateEvent(int currentTime, int activationTime, Job job, EventType evt, int tabno) {
        for (int i = 0; i < tabno; ++i) System.out.print("\t");
        System.out.println("debug) Throw " + evt + " event: jobId " + job.getJobId() + ", newAcTime: " + activationTime + " at " + currentTime);
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
        double accumulatedCpuTime = job.getAccumulatedCpuTime();
        accumulatedCpuTime += (double)mostRecentRunningTime / OCStateLevel;
        job.setAccumulatedCpuTime(accumulatedCpuTime);
        
        boolean interactiveJob = job.isInteracitveJob();
        if (!interactiveJob) {
            if (OCStateLevel == 1) {
                int runningTimeDed = job.getRunningTimeDed();
                job.setRunningTimeDed(runningTimeDed + mostRecentRunningTime);
            } else {
                int runningTimeOC = job.getRunningTimeOC();
                job.setRunningTimeOC(runningTimeOC + mostRecentRunningTime);
            }
        }
        int runningTimeDed = job.getRunningTimeDed();
        int runningTimeOC = job.getRunningTimeOC();
        int runningTime = runningTimeDed + runningTimeOC;
        int finishedTime = runningTime + job.getStartTime();
        if (interactiveJob)
            finishedTime = job.getStartTime() + runningTime + job.getPrologTime() + job.getSumIdleTime() + job.getEpilogTIme();
        job.setFinishedTime(finishedTime);

            
        assert currentTime == finishedTime;
        
        // Output the result
        NodeConsciousScheduler.sim.outputResult(job);
        NodeConsciousScheduler.sim.outputResultForVis(job);

        // Erase the job from executing job list
        NodeConsciousScheduler.sim.getExecutingJobList().remove(job);
        // Add the job completed List
        NodeConsciousScheduler.sim.getCompletedJobList().add(job);

        // Resource refill        
        NodeConsciousScheduler.sim.freeResources(job);

        // Again call scheduling
        evs = NodeConsciousScheduler.sim.getSche().scheduleJobsOnEnd(ev);

        
        return evs;
    }


}

class IntActivate implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        // TODO
        // Get job Info
        // Timing coexisting jobs
        // Activation the job and coexisting jobs
        
        /* Fix the finish time */
        int currentTime = ev.getOccurrenceTime();
        Job job = ev.getJob();
        int jobId = job.getJobId();
        boolean interactiveJob = job.isInteracitveJob();
        boolean activationState = job.isActivationState();
        assert !activationState;
        
        int OCStateLevel = job.getOCStateLevel();
        Set<Integer> coexistingJobs = job.getCoexistingJobs();
        System.out.println("\tActivate jobId: " + jobId + ", victim jobId: " + coexistingJobs);
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = Scheduler.getJobByJobId(coexistingJobId);           
            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();
            if (!intJobFlag) {
                Scheduler.measureCurrentExecutingTime(currentTime, coexistingJob);
            } else if (actStateFlag) {
                Scheduler.measureCurrentExecutingTimeForActivation(currentTime, coexistingJob, coexistingJob.getApparentOCStateLevel());
            }
            coexistingJob.setPreviousMeasuredTime(currentTime);
        }
        job.setActivationState(!activationState);
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = Scheduler.getJobByJobId(coexistingJobId);           
            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();

            int coexistingApparentOCStateLevel = coexistingJob.getApparentOCStateLevel();
            int newCoexistingApparnteOCStateLevel = Scheduler.calculateNewOCStateLevelForExecutingJob(coexistingJob, true);
            int coexistingOCStateLevel = coexistingJob.getOCStateLevel();
            assert coexistingApparentOCStateLevel <= newCoexistingApparnteOCStateLevel;
            assert coexistingApparentOCStateLevel <= coexistingOCStateLevel;
            coexistingJob.setApparentOCStateLevel(newCoexistingApparnteOCStateLevel);
            
            if (!intJobFlag) {
                int oldTrueEndTime = coexistingJob.getEndEventOccuranceTimeNow();
                int trueEndTime = Scheduler.calculateNewActualEndTime(currentTime, coexistingJob);
                if (oldTrueEndTime != trueEndTime) {
                    Scheduler.printThrowENDEvent(currentTime, trueEndTime, coexistingJob, EventType.END);
                    evs.add(new Event(EventType.END, trueEndTime, coexistingJob));
                    coexistingJob.setEndEventOccuranceTimeNow(trueEndTime);
                    Scheduler.printThrowENDEvent(currentTime, trueEndTime, coexistingJob, EventType.DELETE_FROM_END);
                    evs.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
                }
            } else if (actStateFlag){
                int oldDeactivateTime = coexistingJob.getCurrentDeactiveTime();
                int deactivateTime = Scheduler.calculateNewActualEndTimeForActivation(currentTime, coexistingJob);
                if (oldDeactivateTime != deactivateTime) {
                    printThrowDeactivateEvent(currentTime, deactivateTime, coexistingJob, EventType.DELETE_DEACTIVE, 1);
                    evs.add(new Event(EventType.DELETE_DEACTIVE, currentTime, coexistingJob, oldDeactivateTime));
                    coexistingJob.setCurrentDeactiveTime(deactivateTime);
                    printThrowDeactivateEvent(currentTime, deactivateTime, coexistingJob, EventType.INT_DEACTIVATE, 1);
                    evs.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, coexistingJob));
                }
            }
        }        
        
        int apparentOCStateLevel = job.getApparentOCStateLevel();
        int newApparentOCStateLevel = Scheduler.calculateNewOCStateLevelForExecutingJob(job, true);
        // TODO: acc_int
        // calc coming int_deactivate
        // throw
        assert apparentOCStateLevel <= newApparentOCStateLevel;
        assert newApparentOCStateLevel <= OCStateLevel;
        job.setApparentOCStateLevel(newApparentOCStateLevel);
        int deactivateTime = Scheduler.calculateNewActualEndTimeForActivation(currentTime, job);
        

        job.setPreviousMeasuredTime(currentTime);
        job.setCurrentDeactiveTime(deactivateTime);        
        printThrowDeactivateEvent(currentTime, deactivateTime, job, EventType.INT_DEACTIVATE, 1);
        evs.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, job));
        
        return evs;
    }
    ArrayList<Integer> calculateAssignNodesNo (ArrayList<UsingNode> usingNodeList) {
        ArrayList<Integer> assignNodesNo = new ArrayList();
        for (int i = 0; i < usingNodeList.size(); ++i) {
            int nodeId = usingNodeList.get(i).getNodeNum();
            assignNodesNo.add(nodeId);
        }
        return assignNodesNo;
    }
    static void printThrowDeactivateEvent(int currentTime, int deactivationTime, Job job, EventType evt, int tabno) {
        for (int i = 0; i < tabno; ++i) System.out.print("\t");
        System.out.println("debug) Throw " + evt + " event: jobId " + job.getJobId() + ", newDeacTime: " + deactivationTime + " at " + currentTime);
    }
}

class IntDeactivate implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        // TODO
        Job job = ev.getJob();
        int jobId = job.getJobId();
        int currentTime = ev.getOccurrenceTime();
        
        boolean interactiveJob = job.isInteracitveJob();
        assert interactiveJob;
        boolean activationState = job.isActivationState();
        assert activationState;

        Set<Integer> coexistingJobs = job.getCoexistingJobs();
        System.out.println("\tDeactivate jobId: " + jobId + ", victim jobId: " + coexistingJobs);
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = Scheduler.getJobByJobId(coexistingJobId);

            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();
            if (!intJobFlag) {            
                Scheduler.measureCurrentExecutingTime(currentTime, coexistingJob);
            } else if (actStateFlag) {
                Scheduler.measureCurrentExecutingTimeForActivation(currentTime, coexistingJob, currentTime);
            }
            coexistingJob.setPreviousMeasuredTime(currentTime);
        }
        job.setActivationState(!activationState);
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = Scheduler.getJobByJobId(coexistingJobId);
            int coexistingApparentOCStateLevel = coexistingJob.getApparentOCStateLevel();
            int coexistingOCStateLevel = coexistingJob.getOCStateLevel();

            int newCoexistingApparentOCStateLevel = Scheduler.calculateNewOCStateLevelForExecutingJob(coexistingJob, true);
            assert newCoexistingApparentOCStateLevel <= coexistingApparentOCStateLevel;
            assert coexistingApparentOCStateLevel <= coexistingOCStateLevel;
            coexistingJob.setApparentOCStateLevel(newCoexistingApparentOCStateLevel);
            
            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();
            
            if (!intJobFlag) {
                int oldTrueEndTime = coexistingJob.getEndEventOccuranceTimeNow();
                int trueEndTime = Scheduler.calculateNewActualEndTime(currentTime, coexistingJob);
                if (oldTrueEndTime != trueEndTime) {
                    Scheduler.printThrowENDEvent(currentTime, trueEndTime, coexistingJob, EventType.END);
                    evs.add(new Event(EventType.END, trueEndTime, coexistingJob));
                    coexistingJob.setEndEventOccuranceTimeNow(trueEndTime);
                    Scheduler.printThrowENDEvent(currentTime, trueEndTime, coexistingJob, EventType.DELETE_FROM_END);
                    evs.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
                }
            } else if (actStateFlag) {
                int oldDeactivateTime = coexistingJob.getCurrentDeactiveTime();
                int deactivateTime = Scheduler.calculateNewActualEndTimeForActivation(currentTime, coexistingJob);
                if (oldDeactivateTime != currentTime && oldDeactivateTime != deactivateTime) {
                    printThrowDeactivateEvent(currentTime, deactivateTime, coexistingJob, EventType.DELETE_DEACTIVE, 1);
                    evs.add(new Event(EventType.DELETE_DEACTIVE, currentTime, coexistingJob, oldDeactivateTime));
                    coexistingJob.setCurrentDeactiveTime(deactivateTime);
                    printThrowDeactivateEvent(currentTime, deactivateTime, coexistingJob, EventType.INT_DEACTIVATE, 1);
                    evs.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, coexistingJob));
                }
            }
            // TODO: treatment of interactive jobs
        }
        job.setActivationState(activationState);

        Scheduler.measureCurrentExecutingTimeForActivation(currentTime, job, job.getOccupiedTimeInTimeSlices());
        double currentAccumulatedComputeQuantityForLatestActivation = job.getCurrentAccumulatedComputeQuantityForLatestActivation();
        // Avoiding Precision problem: + 1e-8
        assert job.getCurrentRequiredActivationTime() <= currentAccumulatedComputeQuantityForLatestActivation + 1e-8;
        currentAccumulatedComputeQuantityForLatestActivation = 0.0;
        job.setCurrentAccumulatedComputeQuantityForLatestActivation(currentAccumulatedComputeQuantityForLatestActivation);
        job.setPreviousMeasuredTime(currentTime);

        job.setActivationState(!activationState);

        int OCStateLevel = job.getOCStateLevel();
        int apparentOCStateLevel = job.getApparentOCStateLevel();
        int newApparentOCStateLevel = Scheduler.calculateNewOCStateLevelForExecutingJob(job, true);
        assert newApparentOCStateLevel <= apparentOCStateLevel;
        assert newApparentOCStateLevel <= OCStateLevel;
        job.setApparentOCStateLevel(newApparentOCStateLevel);
        
        int currentActivationIndex = job.getCurrentActivationIndex();
        ArrayList<Integer> activationTimes = job.getActivationTimes();
        if (currentActivationIndex == activationTimes.size()-1) {
            int epilogTime = job.getEpilogTIme();
            int endTime = epilogTime + currentTime;
            job.setEndEventOccuranceTimeNow(endTime);
            Scheduler.printThrowENDEvent(currentTime, endTime, job, EventType.END);
            evs.add(new Event(EventType.END, endTime, job));
        } else {
            int idleTime = job.getIdleTimes().get(currentActivationIndex);
            int activateTime = currentTime + idleTime;

            printThrowActivateEvent(currentTime,activateTime, job, EventType.INT_ACTIVATE, 1);
            evs.add(new Event(EventType.INT_ACTIVATE, activateTime, job));

            ++currentActivationIndex;
            job.setCurrentActivationIndex(currentActivationIndex);            
        }


        return evs;
    }
    private void printThrowActivateEvent(int currentTime, int activationTime, Job job, EventType evt, int tabno) {
        for (int i = 0; i < tabno; ++i) System.out.print("\t");
        System.out.println("debug) Throw " + evt + " event: jobId " + job.getJobId() + ", newAcTime: " + activationTime + " at " + currentTime);
    }
}

class DeleteDeactivate implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );

        NodeConsciousScheduler.sim.getEvq().deleteDeactiveEvent(ev);

        return new ArrayList<Event>();
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
