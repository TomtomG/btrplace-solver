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

package btrplace.solver.choco;


/**
 * Allow to retrieve and release ints that
 * are the basic element identifiers in btrplace.
 *
 * @author Fabien Hermenier
 */
public interface ElementPool {

    /**
     * Get a new int.
     *
     * @return the int if possible, {@code null} if the pool is empty
     */
    int request();

    /**
     * Book a given int.
     *
     * @param u the int to book
     * @return {@code true} iff the int has been booked.
     */
    boolean book(int u);

    /**
     * Release a int that will be available again.
     *
     * @return {@code true}
     */
    boolean release(int u);

    /**
     * Check whether a int is used or not.
     *
     * @param u the int to check
     * @return {@code true} iff the int is in used
     */
    boolean inUse(int u);
}