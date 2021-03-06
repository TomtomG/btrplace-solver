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

package btrplace.json.model.constraint;

import btrplace.json.JSONConverterException;
import btrplace.model.constraint.CumulatedRunningCapacity;
import net.minidev.json.JSONObject;

/**
 * JSON Converter for the constraint {@link CumulatedRunningCapacityConverter}.
 *
 * @author Fabien Hermenier
 */
public class CumulatedRunningCapacityConverter extends SatConstraintConverter<CumulatedRunningCapacity> {

    @Override
    public Class<CumulatedRunningCapacity> getSupportedConstraint() {
        return CumulatedRunningCapacity.class;
    }

    @Override
    public String getJSONId() {
        return "cumulatedRunningCapacity";
    }


    @Override
    public CumulatedRunningCapacity fromJSON(JSONObject o) throws JSONConverterException {
        checkId(o);
        return new CumulatedRunningCapacity(requiredNodes(o, "nodes"),
                requiredInt(o, "amount"),
                requiredBoolean(o, "continuous"));
    }

    @Override
    public JSONObject toJSON(CumulatedRunningCapacity o) {
        JSONObject c = new JSONObject();
        c.put("id", getJSONId());
        c.put("nodes", nodesToJSON(o.getInvolvedNodes()));
        c.put("amount", o.getAmount());
        c.put("continuous", o.isContinuous());
        return c;
    }
}
