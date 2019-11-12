/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.ext.cassandra.query.rdf;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.ext.cassandra.MutableReadConsistency;

/**
 * A query to retrieve a list of the Mementos of a resource.
 */
public class Mementos extends ResourceQuery {

    /**
     * Create a query that retrieves a list of mementos.
     * @param session the cassandra session
     * @param consistency the consistency level
     */
    @Inject
    public Mementos(final CqlSession session, @MutableReadConsistency final ConsistencyLevel consistency) {
        super(session, "SELECT modified FROM " + MEMENTO_MUTABLE_TABLENAME + " WHERE identifier = :identifier",
                        consistency);
    }

    /**
     * @param id the {@link IRI} of the resource the Mementos of which are to be cataloged
     * @return A {@link AsyncResultSet} with the modified-dates of any Mementos for this resource.
     *         There will be at least one (the most recent one).
     */
    public CompletionStage<AsyncResultSet> execute(final IRI id) {
        return executeRead(preparedStatement().bind().set("identifier", id, IRI.class));
    }
}