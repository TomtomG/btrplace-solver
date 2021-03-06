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

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Online;
import btrplace.model.constraint.SatConstraint;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.actionModel.ActionModel;
import choco.kernel.solver.ContradictionException;

import java.util.Collections;
import java.util.Set;


/**
 * Choco implementation of {@link btrplace.model.constraint.Online}.
 *
 * @author Fabien Hermenier
 */
public class COnline implements ChocoSatConstraint {

    private Online cstr;

    /**
     * Make a new constraint.
     *
     * @param o the {@link SatConstraint} to rely on
     */
    public COnline(Online o) {
        this.cstr = o;
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SolverException {
        for (Node nId : cstr.getInvolvedNodes()) {
            ActionModel m = rp.getNodeAction(nId);
            try {
                m.getState().setVal(1);
            } catch (ContradictionException e) {
                rp.getLogger().error("Unable to force node '{}' at being online: {}", nId, e.getMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<VM> getMisPlacedVMs(Model m) {
        return Collections.emptySet();
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
            return Online.class;
        }

        @Override
        public COnline build(SatConstraint cstr) {
            return new COnline((Online) cstr);
        }
    }
}
