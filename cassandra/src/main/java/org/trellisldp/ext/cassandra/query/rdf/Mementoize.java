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

import static java.time.temporal.ChronoUnit.SECONDS;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.trellisldp.ext.cassandra.MutableWriteConsistency;

/**
 * A query that records a version of a resource as a Memento.
 */
public class Mementoize extends ResourceQuery {

    /**
     * Create a query that generates a memento.
     * @param session the cassandra session
     * @param consistency the consistency level
     */
    @Inject
    public Mementoize(final CqlSession session, @MutableWriteConsistency final ConsistencyLevel consistency) {
        super(session, "INSERT INTO " + MEMENTO_MUTABLE_TABLENAME
                        + " (interactionModel, mimeType, container, quads, modified, binaryIdentifier, "
                        + "created, identifier, mementomodified)" + " VALUES (?,?,?,?,?,?,?,?,?);", consistency);
    }

    /**
     * Store a Memento. Note that the value for {@code modified} is truncated to seconds because Memento requires HTTP
     * time management.
     *
     * @param ixnModel an {@link IRI} for the interaction model for this resource
     * @param mimeType if this resource has a binary, the mimeType therefor
     * @param container if this resource has a container, the {@link IRI} therefor
     * @param data RDF for this resource
     * @param modified the time at which this resource was last modified
     * @param binaryIdentifier if this resource has a binary, the identifier therefor
     * @param creation a time-based (version 1) UUID for the moment this resource is created
     * @param id an {@link IRI} that identifies this resource
     * @return whether and when it has been inserted
     */
    public CompletionStage<Void> execute(final IRI ixnModel, final String mimeType, final IRI container,
            final Dataset data, final Instant modified, final IRI binaryIdentifier, final UUID creation, final IRI id) {
        final Instant mementoModified = modified.truncatedTo(SECONDS);
        final BoundStatement statement = preparedStatement().bind(ixnModel, mimeType, container, data, modified,
                        binaryIdentifier, creation, id, mementoModified);
        return executeWrite(statement);
    }
}
