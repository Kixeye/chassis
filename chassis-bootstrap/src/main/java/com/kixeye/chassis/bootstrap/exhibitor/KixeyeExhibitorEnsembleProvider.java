package com.kixeye.chassis.bootstrap.exhibitor;

import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.exhibitor.ExhibitorEnsembleProvider;
import org.apache.curator.ensemble.exhibitor.ExhibitorRestClient;
import org.apache.curator.ensemble.exhibitor.Exhibitors;

/**
 * Assumes a static list Exhibitor urls as a source for zookeeper connections strings and
 * continues to poll that list. This alters functionality of the superclass in that it only uses the given
 * Exhibitor urls for the initial list of zookeepers and then polls those zookeepers for changes
 * to the Ensemble.
 *
 * This class is useful when the static Exhibitor url list given to it points to a VIP which maintains the pool
 * of Exhibitors.
 *
 * @author dturner@kixeye.com
 */
public class KixeyeExhibitorEnsembleProvider extends ExhibitorEnsembleProvider{

    private Exhibitors initialExhibitors;

    /**
     * @param exhibitors  the current set of exhibitor instances (can be changed later via {@link #setExhibitors(org.apache.curator.ensemble.exhibitor.Exhibitors)})
     * @param restClient  the rest client to use (use {@link org.apache.curator.ensemble.exhibitor.DefaultExhibitorRestClient} for most cases)
     * @param restUriPath the path of the REST call used to get the server set. Usually: <code>/exhibitor/v1/cluster/list</code>
     * @param pollingMs   how ofter to poll the exhibitors for the list
     * @param retryPolicy retry policy to use when connecting to the exhibitors
     */
    public KixeyeExhibitorEnsembleProvider(Exhibitors exhibitors, ExhibitorRestClient restClient, String restUriPath, int pollingMs, RetryPolicy retryPolicy) {
        super(exhibitors, restClient, restUriPath, pollingMs, retryPolicy);
        this.initialExhibitors = exhibitors;
    }

    @Override
    protected synchronized void poll() {
        //always reset the Exhibitors list to the initial list because it is super.poll() changes this list at the end
        setExhibitors(initialExhibitors);
        super.poll();
    }
}
