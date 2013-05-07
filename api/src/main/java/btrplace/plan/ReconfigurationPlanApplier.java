package btrplace.plan;

import btrplace.model.Model;
import btrplace.plan.event.EventCommittedListener;

/**
 * An object to simulate the application of
 * a plan. The result will be a new model.
 *
 * @author Fabien Hermenier
 */
public interface ReconfigurationPlanApplier {

    /**
     * Add a listener that will be notified upon event termination.
     *
     * @param l the listener to add
     */
    void addEventCommittedListener(EventCommittedListener l);

    /**
     * Remove a listener.
     *
     * @param l the listener
     * @return {@code true} iff the listener was removed
     */
    boolean removeEventCommittedListener(EventCommittedListener l);

    /**
     * Apply a plan.
     *
     * @param p the plan to apply
     * @return the resulting model if the application succeed. {@code null} otherwise
     */
    Model apply(ReconfigurationPlan p);

    /**
     * Textual representation of a plan.
     *
     * @param p the plan to stringify
     * @return the formatted string
     */
    String toString(ReconfigurationPlan p);
}
