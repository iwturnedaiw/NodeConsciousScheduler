package nodeconsciousscheduler;


import java.util.HashSet;
import java.util.Set;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author sminami
 */
public class NewAndDeletedCoexistingJobs {
    Set<Integer> newCoexistingJobsOnTheCore;
    Set<Integer> deletedCoexistingJobsFromTheCore;

    public NewAndDeletedCoexistingJobs() {
        this.newCoexistingJobsOnTheCore = new HashSet<Integer>();
        this.deletedCoexistingJobsFromTheCore = new HashSet<Integer>();
    }
    
    public NewAndDeletedCoexistingJobs(HashSet<Integer> newCoexistingJobsOnTheCore, HashSet<Integer> deletedCoexistingJobsOnTheCore) {
        this.newCoexistingJobsOnTheCore = newCoexistingJobsOnTheCore;
        this.deletedCoexistingJobsFromTheCore = deletedCoexistingJobsOnTheCore;
    }

    public Set<Integer> getNewCoexistingJobsOnTheCore() {
        return newCoexistingJobsOnTheCore;
    }

    public Set<Integer> getDeletedCoexistingJobsFromTheCore() {
        return deletedCoexistingJobsFromTheCore;
    }

    
    
    public void setNewCoexistingJobsOnTheCore(Set<Integer> newCoexistingJobsOnTheCore) {
        this.newCoexistingJobsOnTheCore = newCoexistingJobsOnTheCore;
    }

    public void setDeletedCoexistingJobsFromTheCore(Set<Integer> deletedCoexistingJobsFromTheCore) {
        this.deletedCoexistingJobsFromTheCore = deletedCoexistingJobsFromTheCore;
    }
    
    
    
}
