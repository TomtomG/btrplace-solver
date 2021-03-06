/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.solver.choco.constraint;

import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.constraint.Split;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.Slice;
import btrplace.solver.choco.actionModel.VMActionModel;
import btrplace.solver.choco.chocoUtil.Disjoint;
import btrplace.solver.choco.chocoUtil.Precedences;
import choco.cp.solver.CPSolver;
import choco.kernel.solver.variables.integer.IntDomainVar;
import gnu.trove.TIntArrayList;

import java.util.*;

/**
 * Choco implementation of the {@link btrplace.model.constraint.Split} constraint.
 *
 * @author Fabien Hermenier
 */
public class CSplit implements ChocoSatConstraint {

    private Split cstr;

    /**
     * Make a new constraint.
     *
     * @param s the constraint to rely on
     */
    public CSplit(Split s) {
        this.cstr = s;
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SolverException {
        List<List<IntDomainVar>> groups = new ArrayList<>();
        List<List<VM>> vmGroups = new ArrayList<>();
        for (Collection<VM> grp : cstr.getSets()) {
            List<IntDomainVar> l = new ArrayList<>();
            List<VM> vl = new ArrayList<>();
            for (VM vm : grp) {
                if (rp.getFutureRunningVMs().contains(vm)) {
                    Slice s = rp.getVMAction(vm).getDSlice();
                    l.add(s.getHoster());
                    vl.add(vm);
                }
            }
            if (!l.isEmpty()) {
                groups.add(l);
                vmGroups.add(vl);
            }
        }
        CPSolver s = rp.getSolver();
        int nbNodes = rp.getNodes().length;
        IntDomainVar[][] vars = new IntDomainVar[groups.size()][];
        for (int i = 0; i < groups.size(); i++) {
            for (int j = 0; j < i; j++) {
                IntDomainVar[] gI = vars[i];
                IntDomainVar[] gJ = vars[j];

                if (gI == null) {
                    gI = groups.get(i).toArray(new IntDomainVar[groups.get(i).size()]);
                    vars[i] = gI;
                }

                if (gJ == null) {
                    gJ = groups.get(j).toArray(new IntDomainVar[groups.get(j).size()]);
                    vars[j] = gJ;
                }

                s.post(new Disjoint(s.getEnvironment(), gI, gJ, nbNodes));
            }
        }
        if (cstr.isContinuous()) {
            if (!cstr.isSatisfied(rp.getSourceModel())) {
                rp.getLogger().error("The constraint '{}' must be already satisfied to provide a continuous restriction", cstr);
                return false;
            } else {
                Mapping map = rp.getSourceModel().getMapping();
                //Each VM on a group, can not go to a node until all the VMs from the other groups have leaved
                //So, for each group of VM, we create a list containing the c^end and the c^host variable of all
                //the VMs in the other groups

                TIntArrayList[] otherPositions = new TIntArrayList[vmGroups.size()];
                List<IntDomainVar>[] otherEnds = new List[vmGroups.size()];
                for (int i = 0; i < vmGroups.size(); i++) {
                    otherPositions[i] = new TIntArrayList();
                    otherEnds[i] = new ArrayList<>();
                }

                //Fulfill the others stuff.
                for (int i = 0; i < vmGroups.size(); i++) {
                    List<VM> grp = vmGroups.get(i);
                    for (VM vm : grp) {
                        if (map.getRunningVMs().contains(vm)) {
                            int myPos = rp.getNode(map.getVMLocation(vm));
                            IntDomainVar myEnd = rp.getVMAction(vm).getCSlice().getEnd();

                            for (int j = 0; j < vmGroups.size(); j++) {
                                if (i != j) {
                                    otherPositions[j].add(myPos);
                                    otherEnds[j].add(myEnd);
                                }
                            }
                        }
                    }
                }
                int[][] otherPos = new int[groups.size()][];
                IntDomainVar[][] otherEds = new IntDomainVar[groups.size()][];
                for (int i = 0; i < vmGroups.size(); i++) {
                    otherPos[i] = otherPositions[i].toNativeArray();
                    otherEds[i] = otherEnds[i].toArray(new IntDomainVar[otherEnds[i].size()]);
                }

                //Now, we just have to put way too many precedences constraint, one per VM.
                for (int i = 0; i < vmGroups.size(); i++) {
                    List<VM> grp = vmGroups.get(i);
                    for (VM vm : grp) {
                        if (rp.getFutureRunningVMs().contains(vm)) {
                            VMActionModel a = rp.getVMAction(vm);
                            IntDomainVar myPos = a.getDSlice().getHoster();
                            IntDomainVar myStart = a.getDSlice().getStart();
                            s.post(new Precedences(s.getEnvironment(), myPos, myStart, otherPos[i], otherEds[i]));
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Set<VM> getMisPlacedVMs(Model m) {
        Mapping map = m.getMapping();
        List<Collection<VM>> groups = new ArrayList<>(cstr.getSets());
        //Bad contains the VMs on nodes that host VMs from different groups.
        Set<VM> bad = new HashSet<>();
        for (Collection<VM> grp : groups) {
            for (VM vm : grp) {
                if (map.getRunningVMs().contains(vm)) {
                    Node n = map.getVMLocation(vm);
                    Set<VM> allOnN = map.getRunningVMs(n);
                    for (VM vmOnN : allOnN) {
                        if (inOtherGroup(groups, grp, vmOnN)) {
                            //The VM belong to another group
                            bad.add(vm);
                            bad.add(vmOnN);
                        }
                    }
                }
            }
        }
        return bad;
    }

    private boolean inOtherGroup(List<Collection<VM>> groups, Collection<VM> grp, VM vmOnN) {
        for (Collection<VM> s : groups) {
            if (s.contains(vmOnN) && !grp.contains(vmOnN)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public String toString() {
        return cstr.toString();
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoSatConstraintBuilder {
        @Override
        public Class<? extends SatConstraint> getKey() {
            return Split.class;
        }

        @Override
        public CSplit build(SatConstraint cstr) {
            return new CSplit((Split) cstr);
        }
    }
}
