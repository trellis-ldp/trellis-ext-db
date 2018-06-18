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
package org.trellisldp.ext.db;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.vocabulary.RDF.type;

import com.google.common.io.Resources;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.text.RandomStringGenerator;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.Binary;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.id.UUIDGenerator;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.Trellis;

/**
 * ResourceService tests.
 */
public class DBResourceTest {

    private static final RDF rdf = getInstance();

    private static final IdentifierService idService = new UUIDGenerator();

    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);

    private static EmbeddedPostgres pg = null;

    private static ResourceService svc = null;

    static {
        try {
            pg = EmbeddedPostgres.builder()
                .setDataDirectory("./build/pgdata-" + new RandomStringGenerator.Builder()
                .withinRange('a', 'z').build().generate(10)).start();
            // SET UP DATABASE
            Jdbi.create(pg.getPostgresDatabase()).useHandle(handle ->
                handle.execute(Resources.toString(getResource("create.pgsql"), UTF_8)));

            svc = new DBResourceService(pg.getPostgresDatabase(), idService,
                new NoopMementoService(), new NoopEventService());

        } catch (final IOException ex) {

        }
    }

    @Test
    public void getRoot() {
        assertTrue(DBResource.findResource(pg.getPostgresDatabase(), root).isPresent());
    }

    @Test
    public void testReinit() {
        final ResourceService svc2 = new DBResourceService(pg.getPostgresDatabase(), idService,
                new NoopMementoService(), new NoopEventService());
        assertNotNull(svc2);
    }

    @Test
    public void getNonExistent() {
        assertFalse(DBResource.findResource(pg.getPostgresDatabase(), rdf.createIRI(TRELLIS_DATA_PREFIX + "other"))
                .isPresent());
    }

    @Test
    public void getServerQuads() {
        DBResource.findResource(pg.getPostgresDatabase(), root).ifPresent(res -> {
            final Graph serverManaged = rdf.createGraph();
            res.stream(Trellis.PreferServerManaged).forEach(serverManaged::add);
            assertTrue(serverManaged.contains(root, type, LDP.BasicContainer));
        });
    }

    @Test
    public void getMembershipQuads() {
        DBResource.findResource(pg.getPostgresDatabase(), root).ifPresent(res ->
            assertEquals(0L, res.stream(LDP.PreferMembership).count()));
    }

    @Test
    public void testScan() {
        assertTrue(svc.scan().anyMatch(triple ->
                    triple.getSubject().equals(root) && triple.getPredicate().equals(type) &&
                    triple.getObject().equals(LDP.BasicContainer)));
    }

    @Test
    public void testCompact() {
        assertThrows(UnsupportedOperationException.class, () -> svc.compact(root, now(), now()));
    }

    @Test
    public void testPurge() {
        assertThrows(UnsupportedOperationException.class, () -> svc.purge(root));
    }

    @Test
    public void getBinary() throws Exception {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "binary");
        final IRI binaryIri = rdf.createIRI("http://example.com/resource");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        final Binary binary = new Binary(binaryIri, now(), "text/plain", 10L);
        assertTrue(svc.create(identifier, new SimpleSession(Trellis.AnonymousAgent), LDP.NonRDFSource, dataset, root,
                binary).get());
        svc.get(identifier).ifPresent(res -> {
            assertTrue(res.getBinary().isPresent());
            assertTrue(res.stream(Trellis.PreferServerManaged).anyMatch(triple ->
                    triple.getSubject().equals(identifier) && triple.getPredicate().equals(DC.hasPart) &&
                    triple.getObject().equals(binaryIri)));
        });
    }

    @Test
    public void getAclQuads() {
        DBResource.findResource(pg.getPostgresDatabase(), root).ifPresent(res -> {
            final Graph acl = rdf.createGraph();
            res.stream(Trellis.PreferAccessControl).forEach(acl::add);
            assertTrue(acl.contains(null, ACL.mode, ACL.Read));
            assertTrue(acl.contains(null, ACL.mode, ACL.Write));
            assertTrue(acl.contains(null, ACL.mode, ACL.Control));
        });
    }

    @Test
    public void testAuthQuads() throws Exception {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "auth#acl");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Read);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Write);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Control);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Append);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.agentClass, FOAF.Agent);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.accessTo,
                rdf.createIRI(TRELLIS_DATA_PREFIX + "auth"));

        assertTrue(svc.create(identifier, new SimpleSession(Trellis.AnonymousAgent), LDP.RDFSource, dataset, root,
                null).get());
        svc.get(identifier).ifPresent(res -> {
            assertEquals(6L, res.stream(Trellis.PreferAccessControl).count());
        });
    }

    @Test
    public void testEmptyAudit() throws Exception {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());

        final Session session = new SimpleSession(Trellis.AnonymousAgent);
        assertTrue(svc.create(identifier, session, LDP.RDFSource, rdf.createDataset(), root, null).get());
        assertTrue(svc.add(identifier, session, rdf.createDataset()).get());
        assertTrue(svc.get(identifier).isPresent());
        svc.get(identifier).ifPresent(res -> assertEquals(0L, res.stream(Trellis.PreferAudit).count()));
    }

    @Test
    public void getExtraLinkRelations() throws Exception {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "extras");
        final String inbox = "http://example.com/inbox";
        final String annotations = "http://example.com/annotations";
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, identifier, LDP.inbox, rdf.createIRI(inbox));
        dataset.add(Trellis.PreferUserManaged, identifier, OA.annotationService, rdf.createIRI(annotations));
        assertTrue(svc.create(identifier, new SimpleSession(Trellis.AnonymousAgent), LDP.RDFSource, dataset, root,
                null).get());
        svc.get(identifier).ifPresent(res -> {
            assertTrue(res.stream(Trellis.PreferUserManaged).anyMatch(triple ->
                    triple.getSubject().equals(identifier) && triple.getPredicate().equals(LDP.inbox) &&
                    triple.getObject().equals(rdf.createIRI(inbox))));
        });
        DBResource.findResource(pg.getPostgresDatabase(), identifier).ifPresent(res -> {
            assertEquals(2L, res.getExtraLinkRelations().count());
            assertTrue(res.getExtraLinkRelations().anyMatch(rel -> rel.getKey().equals(annotations)
                        && rel.getValue().equals(OA.annotationService.getIRIString())));
            assertTrue(res.getExtraLinkRelations().anyMatch(rel -> rel.getKey().equals(inbox)
                        && rel.getValue().equals(LDP.inbox.getIRIString())));
        });
    }

    @Test
    public void testAddErrorCondition() throws Exception {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Session session = new SimpleSession(Trellis.AnonymousAgent);
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), type, rdf.createLiteral("Invalid quad"));
        assertThrows(ExecutionException.class, () -> svc.add(identifier, session, dataset).get());
    }

    @Test
    public void testCreateErrorCondition() throws Exception {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Session session = new SimpleSession(Trellis.AnonymousAgent);
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, rdf.createBlankNode(), type, rdf.createLiteral("Invalid quad"));
        assertThrows(ExecutionException.class, () ->
                svc.create(identifier, null, LDP.Container, dataset, null, null).get());
    }
}