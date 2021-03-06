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

package btrplace.model;

import btrplace.model.view.ModelView;

import java.util.*;

/**
 * Default implementation for {@link Model}.
 *
 * @author Fabien Hermenier
 */
public class DefaultModel implements Model, Cloneable {

    private Mapping cfg;

    private Map<String, ModelView> resources;

    private Attributes attrs;

    private int nextVM;

    private int nextNode;

    private Set<VM> usedVMIds;
    private Set<Node> usedNodeIds;

    /**
     * Make a new instance.
     */
    public DefaultModel() {
        usedNodeIds = new HashSet<>();
        usedVMIds = new HashSet<>();
        this.resources = new HashMap<>();
        attrs = new DefaultAttributes();
        cfg = new DefaultMapping();
        nextVM = 0;
        nextNode = 0;
    }

    @Override
    public ModelView getView(String id) {
        return this.resources.get(id);
    }

    @Override
    public boolean attach(ModelView v) {
        if (this.resources.containsKey(v.getIdentifier())) {
            return false;
        }
        this.resources.put(v.getIdentifier(), v);
        return true;
    }

    @Override
    public Collection<ModelView> getViews() {
        return this.resources.values();
    }

    @Override
    public Mapping getMapping() {
        return this.cfg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Model that = (Model) o;

        if (!cfg.equals(that.getMapping())) {
            return false;
        }

        if (!attrs.equals(that.getAttributes())) {
            return false;
        }
        Collection<ModelView> thatRrcs = that.getViews();
        return resources.size() == thatRrcs.size() && resources.values().containsAll(thatRrcs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cfg, resources, attrs);
    }

    @Override
    public boolean detach(ModelView v) {
        return resources.remove(v.getIdentifier()) != null;
    }

    @Override
    public void clearViews() {
        this.resources.clear();
    }

    @Override
    public Attributes getAttributes() {
        return attrs;
    }

    @Override
    public void setAttributes(Attributes a) {
        attrs = a;
    }

    @Override
    public Model clone() {
        DefaultModel m = new DefaultModel();
        MappingUtils.fill(cfg, m.cfg);
        for (ModelView rc : resources.values()) {
            m.attach(rc.clone());
        }
        m.setAttributes(this.getAttributes().clone());
        return m;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Mapping:\n");
        b.append(getMapping());
        b.append("\nAttributes:\n");
        b.append(getAttributes());
        b.append("\nViews:\n");
        for (Map.Entry<String, ModelView> entry : resources.entrySet()) {
            b.append(entry.getKey()).append(": ");
            b.append(entry.getValue()).append("\n");
        }
        return b.toString();
    }

    @Override
    public VM newVM() {
        if (usedVMIds.size() == Integer.MAX_VALUE) {
            //No more ids left
            return null;
        }
        //Find the first free id.
        VM v = new VM(nextVM++);
        while (!usedVMIds.add(v)) {
            v = new VM(nextVM++);

        }
        return v;
    }

    @Override
    public Node newNode() {
        if (usedNodeIds.size() == Integer.MAX_VALUE) {
            //No more ids left
            return null;
        }

        //Find the first free id.
        Node n = new Node(nextNode++);
        while (!usedNodeIds.add(n)) {
            n = new Node(nextNode++);

        }
        return n;
    }

    @Override
    public VM newVM(int id) {
        VM v = new VM(id);
        if (usedVMIds.contains(v)) {
            return null;
        }
        return v;
    }

    @Override
    public Node newNode(int id) {
        Node n = new Node(id);
        if (usedNodeIds.contains(n)) {
            return null;
        }
        return n;
    }

    @Override
    public Set<Node> getNodes() {
        return usedNodeIds;
    }

    @Override
    public Set<VM> getVMs() {
        return usedVMIds;
    }
}
