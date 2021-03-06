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

import btrplace.model.Element;
import btrplace.model.Model;

/**
 * A event to apply on a model to modify it.
 *
 * @author Fabien Hermenier
 * @see {@link Action} for a time-bounded event.
 */
public interface Event<E extends Element> {

    /**
     * Apply the event on a given model.
     *
     * @param m the model to modify
     * @return {@code true} iff the modification succeeded
     */
    boolean apply(Model m);


    /**
     * Notify a visitor to visit the action.
     *
     * @param v the visitor to notify
     */
    Object visit(ActionVisitor v);
}
