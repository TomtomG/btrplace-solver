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

package btrplace.json.plan;

import btrplace.json.JSONConverterException;
import btrplace.model.DefaultMapping;
import btrplace.model.DefaultModel;
import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.plan.DefaultReconfigurationPlan;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.event.Allocate;
import btrplace.plan.event.BootNode;
import btrplace.plan.event.BootVM;
import btrplace.plan.event.MigrateVM;
import btrplace.test.PremadeElements;
import net.minidev.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ReconfigurationPlanConverter}.
 *
 * @author Fabien Hermenier
 */
public class ReconfigurationPlanConverterTest implements PremadeElements {

    @Test
    public void testConversion() throws JSONConverterException {
        Mapping map = new DefaultMapping();
        map.addOnlineNode(n1);
        map.addOfflineNode(n2);
        map.addOnlineNode(n3);
        map.addReadyVM(vm1);
        map.addRunningVM(vm2, n1);
        map.addRunningVM(vm3, n1);
        map.addSleepingVM(vm4, n3);
        map.addRunningVM(vm5, n3);

        Model mo = new DefaultModel(map);

        ReconfigurationPlan plan = new DefaultReconfigurationPlan(mo);
        plan.add(new MigrateVM(vm2, n1, n3, 0, 1));
        plan.add(new BootVM(vm1, n3, 0, 1));
        plan.add(new BootNode(n2, 0, 5));
        plan.add(new Allocate(vm1, n3, "foo", 5, 2, 5));

        ReconfigurationPlanConverter rcp = new ReconfigurationPlanConverter();
        JSONObject o = rcp.toJSON(plan);

        System.out.println(rcp.fromJSON(o));
        Assert.assertEquals(rcp.fromJSON(o), plan);

    }
}
