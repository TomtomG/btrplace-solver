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

package btrplace.model.constraint.checker;

import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Among;
import btrplace.plan.event.RunningVMPlacement;

import java.util.Collection;

/**
 * Checker for the {@link Among} constraint
 *
 * @author Fabien Hermenier
 * @see Among
 */
public class AmongChecker extends AllowAllConstraintChecker<Among> {

    /**
     * Current group (for the continuous restriction). {@code null} if no group has been selected.
     */
    private Collection<Node> choosedGroup = null;

    /**
     * Make a new checker.
     *
     * @param a the associated constraint
     */
    public AmongChecker(Among a) {
        super(a);
    }

    @Override
    public boolean startsWith(Model mo) {
        if (getConstraint().isContinuous()) {
            Mapping map = mo.getMapping();
            for (VM vm : getVMs()) {
                if (map.getRunningVMs().contains(vm)) {
                    Collection<Node> nodes = getConstraint().getAssociatedPGroup((map.getVMLocation(vm)));
                    if (nodes == null) {
                        return false;
                    } else if (choosedGroup == null) {
                        choosedGroup = nodes;
                    } else if (!choosedGroup.equals(nodes)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean startRunningVMPlacement(RunningVMPlacement a) {
        if (getConstraint().isContinuous() && getVMs().contains(a.getVM())) {
            if (choosedGroup == null) {
                choosedGroup = getConstraint().getAssociatedPGroup(a.getDestinationNode());
                if (choosedGroup == null) {
                    //disallowed group
                    return false;
                }
            } else {
                if (!choosedGroup.contains(a.getDestinationNode())) {
                    //Not the right group
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean endsWith(Model i) {
        Mapping map = i.getMapping();
        Collection<Node> grp = null;
        for (VM vm : getVMs()) {
            if (map.getRunningVMs().contains(vm)) {
                Collection<Node> nodes = getConstraint().getAssociatedPGroup((map.getVMLocation(vm)));
                if (nodes == null) {
                    return false;
                } else if (grp == null) {
                    grp = nodes;
                } else if (!grp.equals(nodes)) {
                    return false;
                }
            }
        }
        return true;
    }
}
