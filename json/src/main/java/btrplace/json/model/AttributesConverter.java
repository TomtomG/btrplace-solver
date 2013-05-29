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

package btrplace.json.model;

import btrplace.json.AbstractJSONObjectConverter;
import btrplace.model.*;
import net.minidev.json.JSONObject;


/**
 * Serialize/un-serialize attributes.
 * In practice, the JSON representation is a map where int are the keys.
 * For each of these keys, a map contains the key/values pair associated
 * to the element. A value is either a boolean ("true" or "false"), a number (integer or real), or a string.
 *
 * @author Fabien Hermenier
 */
public class AttributesConverter extends AbstractJSONObjectConverter<Attributes> {

    @Override
    public Attributes fromJSON(JSONObject o) {
        Attributes attrs = new DefaultAttributes();
        JSONObject vms = (JSONObject) o.get("vms");
        JSONObject nodes = (JSONObject) o.get("nodes");

        for (String el : vms.keySet()) {
            VM vm = getOrMakeVM(Integer.parseInt(el));
            JSONObject entries = (JSONObject) o.get(el);
            for (String key : entries.keySet()) {
                Object value = entries.get(key);
                if (value.getClass().equals(Boolean.class)) {
                    attrs.put(vm, key, (Boolean) value);
                } else if (value.getClass().equals(String.class)) {
                    attrs.put(vm, key, (String) value);
                } else if (value.getClass().equals(Double.class)) {
                    attrs.put(vm, key, (Double) value);
                } else if (value.getClass().equals(Integer.class)) {
                    attrs.put(vm, key, (Integer) value);
                } else {
                    throw new ClassCastException(value.toString() + " is not a basic type (" + value.getClass() + ")");
                }
            }
        }

        for (String el : vms.keySet()) {
            Node n = getOrMakeNode(Integer.parseInt(el));
            JSONObject entries = (JSONObject) o.get(el);
            for (String key : entries.keySet()) {
                Object value = entries.get(key);
                if (value.getClass().equals(Boolean.class)) {
                    attrs.put(n, key, (Boolean) value);
                } else if (value.getClass().equals(String.class)) {
                    attrs.put(n, key, (String) value);
                } else if (value.getClass().equals(Double.class)) {
                    attrs.put(n, key, (Double) value);
                } else if (value.getClass().equals(Integer.class)) {
                    attrs.put(n, key, (Integer) value);
                } else {
                    throw new ClassCastException(value.toString() + " is not a basic type (" + value.getClass() + ")");
                }
            }
        }
        return attrs;
    }

    @Override
    public JSONObject toJSON(Attributes attributes) {
        JSONObject res = new JSONObject();
        JSONObject vms = new JSONObject();
        JSONObject nodes = new JSONObject();
        for (Element e : attributes.getDefined()) {
            JSONObject el = new JSONObject();
            for (String k : attributes.getKeys(e)) {
                el.put(k, attributes.get(e, k));
            }
            if (e instanceof VM) {
                vms.put(e.toString(), el);
            } else {
                nodes.put(e.toString(), el);
            }
        }
        res.put("vms", vms);
        res.put("nodes", nodes);
        return res;
    }
}
