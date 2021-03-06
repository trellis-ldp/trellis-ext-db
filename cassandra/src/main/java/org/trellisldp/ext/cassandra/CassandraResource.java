/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
 *
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
package org.trellisldp.ext.cassandra;

import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.Trellis;

class CassandraResource implements Resource {

    public static final String CONFIG_CASSANDRA_LDP_TYPE = "trellis.cassandra.ldp-type";
    private static final boolean INCLUDE_LDP_TYPE = getConfig()
        .getOptionalValue(CONFIG_CASSANDRA_LDP_TYPE, Boolean.class).orElse(Boolean.TRUE);
    private static final RDF rdf = RDFFactory.getInstance();

    private final Metadata metadata;
    private final Dataset dataset;
    private final Instant modified;

    public CassandraResource(final Metadata metadata, final Instant modified, final Dataset dataset) {
        this.metadata = metadata;
        this.dataset = dataset;
        this.modified = modified;
    }

    @Override
    public IRI getIdentifier() {
        return metadata.getIdentifier();
    }

    @Override
    public Optional<IRI> getContainer() {
        return metadata.getContainer();
    }

    @Override
    public IRI getInteractionModel() {
        return metadata.getInteractionModel();
    }

    @Override
    public Instant getModified() {
        return modified;
    }

    @Override
    public Set<IRI> getMetadataGraphNames() {
        return metadata.getMetadataGraphNames();
    }

    @Override
    public Optional<BinaryMetadata> getBinaryMetadata() {
        return metadata.getBinary();
    }

    @Override
    public Dataset dataset() {
        return dataset;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<Quad> stream() {
        return Stream.concat(getServerManagedQuads(), (Stream<Quad>) dataset.stream());
    }

    private Stream<Quad> getServerManagedQuads() {
        if (INCLUDE_LDP_TYPE) {
            return Stream.of(rdf.createQuad(Trellis.PreferServerManaged, getIdentifier(), type, getInteractionModel()));
        }
        return Stream.empty();
    }
}
