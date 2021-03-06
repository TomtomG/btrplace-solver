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

import btrplace.model.Model;
import btrplace.model.VM;
import btrplace.model.constraint.Preserve;
import btrplace.model.view.ShareableResource;
import btrplace.plan.event.Allocate;
import btrplace.plan.event.AllocateEvent;


/**
 * Checker for the {@link btrplace.model.constraint.Preserve} constraint
 *
 * @author Fabien Hermenier
 * @see btrplace.model.constraint.Preserve
 */
public class PreserveChecker extends AllowAllConstraintChecker<Preserve> {

    private int amount;

    private String id;

    /**
     * Make a new checker.
     *
     * @param p the associated constraint
     */
    public PreserveChecker(Preserve p) {
        super(p);
        id = p.getResource();
        amount = p.getAmount();
    }

    @Override
    public boolean consume(AllocateEvent a) {
        if (getVMs().contains(a.getVM()) && a.getResourceId().equals(id)) {
            return a.getAmount() >= amount;
        }
        return true;
    }

    @Override
    public boolean start(Allocate a) {
        if (a.getResourceId().equals(id) && getVMs().contains(a.getVM())) {
            return a.getAmount() >= amount;
        }
        return true;
    }

    @Override
    public boolean startsWith(Model mo) {
        return true;
    }

    @Override
    public boolean endsWith(Model mo) {
        ShareableResource r = (ShareableResource) mo.getView(ShareableResource.VIEW_ID_BASE + id);
        if (r == null) {
            return false;
        }
        for (VM vmId : getVMs()) {
            if (mo.getMapping().getRunningVMs().contains(vmId)) {
                int v = r.getConsumption(vmId);
                if (v < amount) {
                    return false;
                }
            }
        }
        return true;
    }
}
