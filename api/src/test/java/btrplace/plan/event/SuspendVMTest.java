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

package btrplace.plan.event;

import btrplace.model.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link SuspendVM}.
 *
 * @author Fabien Hermenier
 */
public class SuspendVMTest {

    static Model mo = new DefaultModel();
    static List<Node> ns = Util.newNodes(mo, 10);
    static List<VM> vms = Util.newVMs(mo, 10);
    static SuspendVM a = new SuspendVM(vms.get(0), ns.get(0), ns.get(1), 3, 5);

    @Test
    public void testInstantiate() {

        Assert.assertEquals(vms.get(0), a.getVM());
        Assert.assertEquals(ns.get(0), a.getSourceNode());
        Assert.assertEquals(ns.get(1), a.getDestinationNode());
        Assert.assertEquals(3, a.getStart());
        Assert.assertEquals(5, a.getEnd());
        Assert.assertFalse(a.toString().contains("null"));
        Assert.assertEquals(a.getCurrentState(), VMStateTransition.VMState.running);
        Assert.assertEquals(a.getNextState(), VMStateTransition.VMState.sleeping);
    }

    @Test(dependsOnMethods = {"testInstantiate"})
    public void testApply() {
        Model m = new DefaultModel();
        Mapping map = m.getMapping();

        map.addOnlineNode(ns.get(0));
        map.addOnlineNode(ns.get(1));
        map.addRunningVM(vms.get(0), ns.get(0));

        SuspendVM a = new SuspendVM(vms.get(0), ns.get(0), ns.get(1), 3, 5);
        Assert.assertTrue(a.apply(m));
        Assert.assertEquals(map.getVMLocation(vms.get(0)), ns.get(1));
        Assert.assertTrue(map.getSleepingVMs().contains(vms.get(0)));

        Assert.assertFalse(a.apply(m));
        Assert.assertEquals(map.getVMLocation(vms.get(0)), ns.get(1));

        map.addRunningVM(vms.get(0), ns.get(1));
        Assert.assertTrue(new SuspendVM(vms.get(0), ns.get(1), ns.get(1), 3, 5).apply(m));

        Assert.assertFalse(new SuspendVM(vms.get(0), ns.get(1), ns.get(0), 3, 5).apply(m));

        map.addReadyVM(vms.get(0));
        Assert.assertFalse(new SuspendVM(vms.get(0), ns.get(1), ns.get(0), 3, 5).apply(m));

        map.addOfflineNode(ns.get(0));
        Assert.assertFalse(new SuspendVM(vms.get(0), ns.get(1), ns.get(0), 3, 5).apply(m));

        map.remove(ns.get(0));
        Assert.assertFalse(new SuspendVM(vms.get(0), ns.get(1), ns.get(0), 3, 5).apply(m));
    }

    @Test(dependsOnMethods = {"testInstantiate"})
    public void testEquals() {
        SuspendVM a = new SuspendVM(vms.get(0), ns.get(0), ns.get(1), 3, 5);
        SuspendVM b = new SuspendVM(vms.get(0), ns.get(0), ns.get(1), 3, 5);
        Assert.assertFalse(a.equals(new Object()));
        Assert.assertTrue(a.equals(a));
        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());

        Assert.assertNotSame(a, new SuspendVM(vms.get(0), ns.get(0), ns.get(1), 4, 5));
        Assert.assertNotSame(a, new SuspendVM(vms.get(0), ns.get(0), ns.get(1), 3, 4));
        Assert.assertNotSame(a, new SuspendVM(vms.get(1), ns.get(0), ns.get(1), 3, 5));
        Assert.assertNotSame(a, new SuspendVM(vms.get(0), ns.get(2), ns.get(1), 3, 5));
        Assert.assertNotSame(a, new SuspendVM(vms.get(0), ns.get(0), ns.get(2), 3, 5));
    }

    @Test
    public void testVisit() {
        ActionVisitor visitor = mock(ActionVisitor.class);
        a.visit(visitor);
        verify(visitor).visit(a);
    }
}
