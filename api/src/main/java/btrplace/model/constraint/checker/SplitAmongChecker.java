package btrplace.model.constraint.checker;

import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.constraint.SplitAmong;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Checker for the {@link btrplace.model.constraint.SplitAmong} constraint
 *
 * @author Fabien Hermenier
 * @see btrplace.model.constraint.SplitAmong
 */
public class SplitAmongChecker extends AllowAllConstraintChecker<SplitAmong> {

    private Collection<Set<UUID>> vGrps;

    /**
     * Make a new checker.
     *
     * @param s the associated constraint
     */
    public SplitAmongChecker(SplitAmong s) {
        super(s);
        vGrps = s.getGroupsOfVMs();
    }

    @Override
    public boolean endsWith(Model i) {
        Mapping m = i.getMapping();
        //The pgroups that are used:
        Set<Set<UUID>> pUsed = new HashSet<>();
        for (Set<UUID> vgrp : vGrps) {
            Set<UUID> choosedGroup = null;

            //Check every running VM in a single vgroup are running in the same pgroup
            for (UUID vmId : vgrp) {
                if (m.getRunningVMs().contains(vmId)) {
                    if (choosedGroup == null) {
                        choosedGroup = getConstraint().getAssociatedPGroup(m.getVMLocation(vmId));
                        if (choosedGroup == null) {
                            //Te VM is running but on an unknown group. It is an error
                            return false;
                        } else if (!pUsed.add(choosedGroup)) {
                            //The pgroup has already been used for another set of VMs.
                            return false;
                        }
                    } else if (!choosedGroup.contains(m.getVMLocation(vmId))) {
                        //The VM is not in the group with the other
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
