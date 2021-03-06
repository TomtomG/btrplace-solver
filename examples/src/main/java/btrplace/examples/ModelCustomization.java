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

package btrplace.examples;

import btrplace.model.*;
import btrplace.model.constraint.*;
import btrplace.model.view.ShareableResource;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.event.MigrateVM;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import btrplace.solver.choco.durationEvaluator.DurationEvaluators;
import btrplace.solver.choco.durationEvaluator.LinearToAResourceActionDuration;

import java.util.*;

/**
 * Tutorial about the basic tuning of a model.
 *
 * @author Fabien Hermenier
 * @see <a href="https://github.com/fhermeni/btrplace-solver/wiki/Customizing-a-model">btrplace website</a>.
 */
public class ModelCustomization implements Example {

    private List<VM> vms = new ArrayList<>();

    private Model makeModel() {
        Model mo = new DefaultModel();
        vms = new ArrayList<>();
        List<Node> ns = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            vms.add(mo.newVM());
        }
        ns.add(mo.newNode());
        ns.add(mo.newNode());
        Mapping map = mo.getMapping();

        map.addOnlineNode(ns.get(0));
        map.addOnlineNode(ns.get(1));

        ShareableResource rcMem = new ShareableResource("mem", 32, 1); //32GB by default for the nodes

        for (int i = 0; i < 10; i++) {
            rcMem.setConsumption(vms.get(i), i % 3 + 1);
            //vm0: 1, vm1:2, vm2:3, vm3:1, vm4:2, vm5:3, vm6:1, ...
        }

        map.addRunningVM(vms.get(0), ns.get(0));
        map.addRunningVM(vms.get(1), ns.get(0));
        map.addRunningVM(vms.get(2), ns.get(0));
        map.addRunningVM(vms.get(3), ns.get(0));
        map.addRunningVM(vms.get(4), ns.get(0));
        map.addRunningVM(vms.get(5), ns.get(0));
        map.addRunningVM(vms.get(6), ns.get(1));
        map.addRunningVM(vms.get(7), ns.get(1));
        map.addRunningVM(vms.get(8), ns.get(1));
        map.addReadyVM(vms.get(9));

        mo.attach(rcMem);
        return mo;
    }

    private List<SatConstraint> makeConstraints(Model model) {
        List<SatConstraint> cstrs = new ArrayList<>();

        //No more than 5 VMs per node
        cstrs.add(new SingleRunningCapacity(model.getMapping().getAllNodes(), 5));

        //vm1 and vm10 on the same node
        cstrs.add(new Gather(Arrays.asList(vms.get(0), vms.get(9))));

        //(vm1, vm2, vm4) and (vm5, vm6, vm8) must not share node
        Collection<VM> g1 = Arrays.asList(vms.get(0), vms.get(1), vms.get(3));
        Collection<VM> g2 = Arrays.asList(vms.get(4), vms.get(5), vms.get(7));

        cstrs.add(new Split(Arrays.asList(g1, g2)));

        //vm10 must be running
        cstrs.add(new Running(Collections.singleton(vms.get(9))));
        return cstrs;
    }

    @Override
    public boolean run() {

        Model model = makeModel();
        List<SatConstraint> cstrs = makeConstraints(model);

        //Set attributes for the VMs
        Attributes attrs = model.getAttributes();
        for (VM vm : model.getMapping().getAllVMs()) {
            attrs.put(vm, "template", vm.id() % 2 == 0 ? "small" : "large");
            attrs.put(vm, "clone", true);
            attrs.put(vm, "forge", vm.id() % 2 == 0 ? 2 : 7);
            //forge == 2 && template == small  for vm0, vm2, vm4, vm6, vm8
            //forge == 7 && template == large for vm1, vm3, vm5, vm7, vm9
        }

        //Change the duration evaluator for MigrateVM action
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        DurationEvaluators dev = cra.getDurationEvaluators();
        dev.register(MigrateVM.class, new LinearToAResourceActionDuration<VM>("mem", 2, 3));

        //Relocate VM4:
        //  using a migration: 7 sec. (mem=2)
        //  using a re-instantiation: 3 sec. (forge:2 + boot:1)
        //Relocate VM5:
        //  using a migration: 5 sec. (mem=1)
        //  using a re-instantiation: 8 sec. (forge:7 + boot:1)

        try {
            cra.doOptimize(true);
            ReconfigurationPlan plan = cra.solve(model, cstrs);
            System.out.println(plan);
        } catch (SolverException ex) {
            System.err.println(ex.getMessage());
            return false;
        }
        return true;
    }
}
