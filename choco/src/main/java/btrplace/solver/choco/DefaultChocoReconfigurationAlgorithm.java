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

package btrplace.solver.choco;

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.*;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.ReconfigurationPlanChecker;
import btrplace.plan.ReconfigurationPlanCheckerException;
import btrplace.solver.SolverException;
import btrplace.solver.choco.constraint.ChocoSatConstraint;
import btrplace.solver.choco.constraint.ChocoSatConstraintBuilder;
import btrplace.solver.choco.constraint.SatConstraintMapper;
import btrplace.solver.choco.durationEvaluator.DurationEvaluators;
import btrplace.solver.choco.objective.ReconfigurationObjective;
import btrplace.solver.choco.objective.minMTTR.MinMTTR;
import btrplace.solver.choco.view.ModelViewMapper;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.Solution;
import choco.kernel.solver.search.measure.IMeasures;

import java.util.*;

/**
 * Default implementation of {@link ChocoReconfigurationAlgorithm}.
 *
 * @author Fabien Hermenier
 */
public class DefaultChocoReconfigurationAlgorithm implements ChocoReconfigurationAlgorithm {

    private ModelViewMapper viewMapper;

    private SatConstraintMapper cstrMapper;

    private boolean optimize = false;

    /**
     * No time limit by default.
     */
    private int timeLimit = 0;

    private boolean repair = false;

    private boolean useLabels = false;

    private ReconfigurationProblem rp;

    private Collection<SatConstraint> cstrs;

    private DurationEvaluators durationEvaluators;

    private ReconfigurationObjective obj;

    private int maxEnd = DefaultReconfigurationProblem.DEFAULT_MAX_TIME;

    private long coreRPDuration;

    private long speRPDuration;

    /**
     * Make a new algorithm.
     */
    public DefaultChocoReconfigurationAlgorithm() {

        cstrMapper = new SatConstraintMapper();
        durationEvaluators = new DurationEvaluators();
        viewMapper = new ModelViewMapper();

        //Default objective
        obj = new MinMTTR();
    }

    @Override
    public void doOptimize(boolean b) {
        this.optimize = b;
    }

    @Override
    public boolean doOptimize() {
        return this.optimize;
    }

    @Override
    public void setTimeLimit(int t) {
        timeLimit = t;
    }

    @Override
    public int getTimeLimit() {
        return timeLimit;
    }

    @Override
    public void doRepair(boolean b) {
        repair = b;
    }

    @Override
    public boolean doRepair() {
        return repair;
    }

    @Override
    public void labelVariables(boolean b) {
        useLabels = b;
    }

    @Override
    public boolean areVariablesLabelled() {
        return useLabels;
    }

    private void checkUnkownVMsInMapping(Model m, Collection<VM> vms) throws SolverException {
        if (!m.getMapping().getAllVMs().containsAll(vms)) {
            Set<VM> unknown = new HashSet<>(vms);
            unknown.removeAll(m.getMapping().getAllVMs());
            throw new SolverException(m, "Unknown VMs: " + unknown);
        }
    }

    /**
     * Check for the existence of nodes in a model
     *
     * @param mo the model to check
     * @param ns the nodes to check
     * @throws SolverException if at least one of the given nodes is not in the RP.
     */
    private void checkNodesExistence(Model mo, Collection<Node> ns) throws SolverException {
        for (Node node : ns) {
            if (!mo.getMapping().getAllNodes().contains(node)) {
                throw new SolverException(mo, "Unknown node '" + node + "'");
            }
        }
    }

    @Override
    public ReconfigurationPlan solve(Model i, Collection<SatConstraint> cstrs) throws SolverException {
        rp = null;
        this.cstrs = cstrs;
        coreRPDuration = -System.currentTimeMillis();
        //Build the RP. As VM state management is not possible
        //We extract VM-state related constraints first.
        //For other constraint, we just create the right choco constraint
        Set<VM> toRun = new HashSet<>();
        Set<VM> toForge = new HashSet<>();
        Set<VM> toKill = new HashSet<>();
        Set<VM> toSleep = new HashSet<>();

        List<ChocoSatConstraint> cConstraints = new ArrayList<>();
        for (SatConstraint cstr : cstrs) {
            checkNodesExistence(i, cstr.getInvolvedNodes());

            //We cannot check for VMs that are going to the ready state
            //as they are not forced to be a part of the initial model
            //(when they will be forged)
            if (!(cstrs instanceof Ready)) {
                checkUnkownVMsInMapping(i, cstr.getInvolvedVMs());
            }

            if (cstr instanceof Running) {
                toRun.addAll(cstr.getInvolvedVMs());
            } else if (cstr instanceof Sleeping) {
                toSleep.addAll(cstr.getInvolvedVMs());
            } else if (cstr instanceof Ready) {
                checkUnkownVMsInMapping(i, cstr.getInvolvedVMs());
                toForge.addAll(cstr.getInvolvedVMs());
            } else if (cstr instanceof Killed) {
                checkUnkownVMsInMapping(i, cstr.getInvolvedVMs());
                toKill.addAll(cstr.getInvolvedVMs());
            }

            ChocoSatConstraintBuilder ccstrb = cstrMapper.getBuilder(cstr.getClass());
            if (ccstrb == null) {
                throw new SolverException(i, "Unable to map constraint '" + cstr.getClass().getSimpleName() + "'");
            }
            ChocoSatConstraint ccstr = ccstrb.build(cstr);
            if (ccstr == null) {
                throw new SolverException(i, "Error while mapping the constraint '"
                        + cstr.getClass().getSimpleName() + "'");
            }
            cConstraints.add(ccstr);
        }

        //Make the core-RP
        DefaultReconfigurationProblemBuilder rpb = new DefaultReconfigurationProblemBuilder(i)
                .setNextVMsStates(toForge, toRun, toSleep, toKill)
                .setViewMapper(viewMapper)
                .setDurationEvaluatators(durationEvaluators);
        if (repair) {
            Set<VM> toManage = new HashSet<>();
            for (ChocoSatConstraint cstr : cConstraints) {
                toManage.addAll(cstr.getMisPlacedVMs(i));
            }
            toManage.addAll(obj.getMisPlacedVMs(i));
            rpb.setManageableVMs(toManage);
        }
        if (useLabels) {
            rpb.labelVariables();
        }
        rp = rpb.build();

        //Set the maximum duration
        try {
            rp.getEnd().setSup(maxEnd);
        } catch (ContradictionException e) {
            rp.getLogger().error("Unable to restrict the maximum plan duration to {}", maxEnd);
            return null;
        }
        coreRPDuration += System.currentTimeMillis();

        //Customize with the constraints
        speRPDuration = -System.currentTimeMillis();
        for (ChocoSatConstraint ccstr : cConstraints) {
            if (!ccstr.inject(rp)) {
                return null;
            }
        }

        //The objective
        obj.inject(rp);
        speRPDuration += System.currentTimeMillis();
        rp.getLogger().debug("{} ms to build the core-RP + {} ms to tune it", coreRPDuration, speRPDuration);

        rp.getLogger().debug("{} nodes; {} VMs; {} constraints", rp.getNodes().length, rp.getVMs().length, cstrs.size());
        rp.getLogger().debug("optimize: {}; timeLimit: {}; manageableVMs: {}", optimize, getTimeLimit(), rp.getManageableVMs().size());

        ReconfigurationPlan p = rp.solve(timeLimit, optimize);
        if (p == null) {
            return null;
        }
        checkSatisfaction2(p, cstrs);
        return p;
    }

    private void checkSatisfaction2(ReconfigurationPlan p, Collection<SatConstraint> cstrs) throws SolverException {
        ReconfigurationPlanChecker chk = new ReconfigurationPlanChecker();
        for (SatConstraint c : cstrs) {
            chk.addChecker(c.getChecker());
        }
        try {
            chk.check(p);
        } catch (ReconfigurationPlanCheckerException ex) {
            throw new SolverException(p.getOrigin(), ex.getMessage(), ex);
        }
    }

    @Override
    public SatConstraintMapper getSatConstraintMapper() {
        return cstrMapper;
    }

    @Override
    public DurationEvaluators getDurationEvaluators() {
        return durationEvaluators;
    }

    @Override
    public ReconfigurationObjective getObjective() {
        return obj;
    }

    @Override
    public void setObjective(ReconfigurationObjective o) {
        obj = o;
    }

    @Override
    public SolvingStatistics getSolvingStatistics() {
        if (rp == null) {
            return new SolvingStatistics(0, 0, 0, optimize, getTimeLimit(), 0, 0, 0, 0, false, 0, 0);
        }
        SolvingStatistics st = new SolvingStatistics(
                rp.getNodes().length,
                rp.getVMs().length,
                cstrs.size(),
                optimize,
                getTimeLimit(),
                rp.getManageableVMs().size(),
                rp.getSolver().getTimeCount(),
                rp.getSolver().getNodeCount(),
                rp.getSolver().getBackTrackCount(),
                rp.getSolver().isEncounteredLimit(),
                coreRPDuration,
                speRPDuration);

        for (Solution s : rp.getSolver().getSearchStrategy().getStoredSolutions()) {
            IMeasures m = s.getMeasures();
            SolutionStatistics sol;
            if (m.getObjectiveValue() != null) {
                sol = new SolutionStatistics(m.getNodeCount(),
                        m.getBackTrackCount(),
                        m.getTimeCount(),
                        m.getObjectiveValue().intValue());
            } else {
                sol = new SolutionStatistics(m.getNodeCount(),
                        m.getBackTrackCount(),
                        m.getTimeCount());
            }
            st.addSolution(sol);

        }
        return st;
    }

    @Override
    public void setMaxEnd(int end) {
        this.maxEnd = end;
    }

    @Override
    public int getMaxEnd() {
        return this.maxEnd;
    }

    @Override
    public ModelViewMapper getViewMapper() {
        return viewMapper;
    }

    @Override
    public void setViewMapper(ModelViewMapper m) {
        viewMapper = m;
    }

    @Override
    public void setVerbosity(int lvl) {
        if (lvl <= 0) {
            ChocoLogging.setVerbosity(Verbosity.SILENT);
            labelVariables(false);
        } else {
            labelVariables(true);
            ChocoLogging.setVerbosity(Verbosity.SOLUTION);
            if (lvl == 2) {
                ChocoLogging.setVerbosity(Verbosity.SEARCH);
                ChocoLogging.setLoggingMaxDepth(Integer.MAX_VALUE);
            } else if (lvl > 2) {
                ChocoLogging.setVerbosity(Verbosity.FINEST);
            }
        }
    }
}
