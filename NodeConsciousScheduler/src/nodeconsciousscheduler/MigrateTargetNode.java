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
public class MigrateTargetNode {
    int nodeId;
    ArrayList<CoreInfo> migrateTargetCores;
    // -> ArrayList<MultiplicityInfo> migrateTargetCores
    // MultiplicityInfo
    //   int coreId
    //   int OCStateLevel
    //   Set<Integer> jobIds
    //   and has comparator
    
    MigrateTargetNode(int nodeId) {
        this.nodeId = nodeId;
        this.migrateTargetCores = new ArrayList<CoreInfo>();
    }

    public int getNodeId() {
        return nodeId;
    }

    public ArrayList<CoreInfo> getMigrateTargetCores() {
        return migrateTargetCores;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public void setMigrateTargetCores(ArrayList<CoreInfo> migrateTargetCores) {
        this.migrateTargetCores = migrateTargetCores;
    }    
}
