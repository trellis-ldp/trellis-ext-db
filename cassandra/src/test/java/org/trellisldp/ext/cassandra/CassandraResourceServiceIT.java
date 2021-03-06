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

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.api.Metadata.builder;

import java.time.Instant;
import java.util.Set;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.test.ResourceServiceTests;
import org.trellisldp.vocabulary.Trellis;

@EnabledIfSystemProperty(named = "trellis.test.cassandra", matches = "true")
class CassandraResourceServiceIT extends CassandraServiceIT implements ResourceServiceTests {

    @Test
    void basicActions() {
        final IRI id = createIRI("http://example.com/id/foo");
        final IRI container = createIRI("http://example.com/id");
        final IRI ixnModel = createIRI("http://example.com/ixnModel");
        @SuppressWarnings("resource")
        final Dataset quads = rdfFactory.createDataset();
        final Quad quad = rdfFactory.createQuad(id, ixnModel, id, Trellis.PreferUserManaged);
        quads.add(quad);

        // build container
        Metadata meta = builder(container).interactionModel(ixnModel).container(null).build();
        connection.resourceService.create(meta, null).toCompletableFuture().join();

        // build resource
        meta = builder(id).interactionModel(ixnModel).container(container).build();
        connection.resourceService.create(meta, quads).toCompletableFuture().join();

        final Resource resource = connection.resourceService.get(id).toCompletableFuture().join();
        assertEquals(id, resource.getIdentifier());
        assertEquals(ixnModel, resource.getInteractionModel());
        assertEquals(container,
                        resource.getContainer().orElseThrow(() -> new AssertionError("Failed to find any container!")));

        final Set<Quad> allQuads = resource.stream().collect(toSet());
        assertTrue(allQuads.contains(quad));

        // touch container
        final Instant modified = connection.resourceService.get(container).toCompletableFuture().join().getModified();
        waitTwoSeconds();
        connection.resourceService.touch(container).toCompletableFuture().join();
        final Instant newModified = connection.resourceService.get(container)
            .toCompletableFuture().join().getModified();
        assertTrue(modified.compareTo(newModified) < 0);
    }

    @Test
    void testNoArgCtor() {
        assertDoesNotThrow(() -> new CassandraResourceService());
    }

    @Override
    public ResourceService getResourceService() {
        return connection.resourceService;
    }
}
