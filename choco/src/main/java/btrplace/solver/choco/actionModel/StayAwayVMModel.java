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

package btrplace.solver.choco.actionModel;

import btrplace.model.VM;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.Slice;
import choco.kernel.solver.variables.integer.IntDomainVar;


/**
 * A fake action model that indicates the VM is ready or sleeping and does
 * not go in the running state.
 *
 * @author Fabien Hermenier
 */
public class StayAwayVMModel implements VMActionModel {

    private VM vm;

    private IntDomainVar zero;

    /**
     * Make a new model.
     *
     * @param rp the RP to use as a basis.
     * @param e  the VM managed by the action
     */
    public StayAwayVMModel(ReconfigurationProblem rp, VM e) {
        vm = e;
        zero = rp.getSolver().makeConstantIntVar(0);
    }

    @Override
    public boolean insertActions(ReconfigurationPlan plan) {
        return true;
    }

    @Override
    public VM getVM() {
        return vm;
    }

    @Override
    public IntDomainVar getStart() {
        return zero;
    }

    @Override
    public IntDomainVar getEnd() {
        return zero;
    }

    @Override
    public IntDomainVar getDuration() {
        return zero;
    }

    @Override
    public Slice getCSlice() {
        return null;
    }

    @Override
    public Slice getDSlice() {
        return null;
    }

    @Override
    public IntDomainVar getState() {
        return zero;
    }

    @Override
    public void visit(ActionModelVisitor v) {
        v.visit(this);
    }

}
