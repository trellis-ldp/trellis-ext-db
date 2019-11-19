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

import org.trellisldp.ext.cassandra.query.CassandraQuery;

/**
 * A query for use by individual resources to retrieve their contents.
 */
abstract class ResourceQuery extends CassandraQuery {

    static final String MUTABLE_TABLENAME = "mutabledata";

    static final String MEMENTO_MUTABLE_TABLENAME = "mementodata";

    static final String IMMUTABLE_TABLENAME = "immutabledata";

    static final String BASIC_CONTAINMENT_TABLENAME = "basiccontainment";

    ResourceQuery() {
        super();
    }

    ResourceQuery(final CqlSession session, final String queryString, final ConsistencyLevel consistency) {
        super(session, queryString, consistency);
    }
}
