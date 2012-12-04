/*
 * Copyright (c) 2012 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 *
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

import btrplace.model.*;
import btrplace.model.constraint.Ban;
import btrplace.model.constraint.Running;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import junit.framework.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Unit tests for {@link CBan}.
 *
 * @author Fabien Hermenier
 */
public class CBanTest {

    @Test
    public void testBasic() throws SolverException {
        ChocoLogging.setVerbosity(Verbosity.FINEST);
        UUID[] nodes = new UUID[5];
        UUID[] vms = new UUID[5];
        Mapping m = new DefaultMapping();
        Set<UUID> sVMs = new HashSet<UUID>();
        Set<UUID> sNodes = new HashSet<UUID>();
        for (int i = 0; i < vms.length; i++) {
            nodes[i] = UUID.randomUUID();
            vms[i] = UUID.randomUUID();
            m.addOnlineNode(nodes[i]);
            m.addRunningVM(vms[i], nodes[i]);
            if (i % 2 == 0) {
                sVMs.add(vms[i]);
                sNodes.add(nodes[i]);
            }
        }

        Model mo = new DefaultModel(m);
        Ban b = new Ban(sVMs, sNodes);
        Collection<SatConstraint> s = new HashSet<SatConstraint>();
        s.add(b);
        s.add(new Running(m.getAllVMs()));

        DefaultChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        cra.labelVariables(true);
        cra.setTimeLimit(-1);
        ReconfigurationPlan p = cra.solve(mo, s);
        Assert.assertEquals(SatConstraint.Sat.SATISFIED, b.isSatisfied(p.getResult()));
        System.out.println(p);
        Assert.assertEquals(3, p.size());
    }
}
