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
package org.trellisldp.ext.aws;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.jena.commonsrdf.JenaCommonsRDF.fromJena;
import static org.apache.jena.query.DatasetFactory.create;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.Lang.NQUADS;
import static org.apache.jena.vocabulary.RDF.type;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFParser;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.TrellisRuntimeException;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * An S3-based resource.
 */
public class S3Resource implements Resource {

    public static final String CONFIG_AWS_LDP_TYPE = "trellis.aws.ldp-type";
    public static final String INTERACTION_MODEL = "trellis.interactionModel";
    public static final String MODIFIED = "trellis.modified";
    public static final String CONTAINER = "trellis.container";
    public static final String HAS_ACL = "trellis.hasACL";
    public static final String MEMBERSHIP_RESOURCE = "trellis.membershipResource";
    public static final String MEMBER_RELATION = "trellis.hasMemberRelation";
    public static final String MEMBER_OF_RELATION = "trellis.isMemberOfRelation";
    public static final String INSERTED_CONTENT_RELATION = "trellis.insertedContentRelation";
    public static final String BINARY_LOCATION = "trellis.binaryLocation";
    public static final String BINARY_TYPE = "trellis.binaryMimeType";
    public static final String METADATA_GRAPHS = "trellis.metadataGraphs";

    private static final RDF rdf = RDFFactory.getInstance();

    private final AmazonS3 client;
    private final ObjectMetadata metadata;
    private final GetObjectRequest req;
    private final String prefix;

    /**
     * Create a Trellis resource from an S3Object.
     * @param metadata the object metadata
     * @param client the s3 client
     * @param req the GET request
     * @param prefix the prefix
     */
    public S3Resource(final ObjectMetadata metadata, final AmazonS3 client, final GetObjectRequest req,
            final String prefix) {
        this.metadata = requireNonNull(metadata, "s3 metadata may not be null!");
        this.client = requireNonNull(client, "s3 client may not be null!");
        this.req = requireNonNull(req, "s3 request may not be null!");
        this.prefix = requireNonNull(prefix, "prefix may not be null!");
    }

    @Override
    public IRI getIdentifier() {
        final String key = req.getKey().startsWith(prefix) ? req.getKey().substring(prefix.length()) : req.getKey();
        return rdf.createIRI(TRELLIS_DATA_PREFIX + key.split("\\?version=")[0]);
    }

    @Override
    public Instant getModified() {
        final String modified = metadata.getUserMetaDataOf(MODIFIED);
        return modified != null ? Instant.parse(modified) : Instant.now();
    }

    @Override
    public IRI getInteractionModel() {
        final String model = metadata.getUserMetaDataOf(INTERACTION_MODEL);
        return model != null ? rdf.createIRI(model) : LDP.Resource;
    }

    @Override
    public Optional<IRI> getContainer() {
        return ofNullable(metadata.getUserMetaDataOf(CONTAINER)).map(rdf::createIRI);
    }

    @Override
    public Optional<IRI> getMembershipResource() {
        return ofNullable(metadata.getUserMetaDataOf(MEMBERSHIP_RESOURCE)).map(rdf::createIRI);
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        return ofNullable(metadata.getUserMetaDataOf(MEMBER_RELATION)).map(rdf::createIRI);
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        return ofNullable(metadata.getUserMetaDataOf(MEMBER_OF_RELATION)).map(rdf::createIRI);
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        return ofNullable(metadata.getUserMetaDataOf(INSERTED_CONTENT_RELATION)).map(rdf::createIRI);
    }

    @Override
    public Set<IRI> getMetadataGraphNames() {
        if (ofNullable(metadata.getUserMetaDataOf(HAS_ACL)).isPresent()) {
            return singleton(Trellis.PreferAccessControl);
        }
        final String graphs = metadata.getUserMetaDataOf(METADATA_GRAPHS);
        if (graphs != null) {
            return Arrays.stream(graphs.split(",")).map(rdf::createIRI).collect(toSet());
        }
        return emptySet();
    }

    @Override
    public Optional<BinaryMetadata> getBinaryMetadata() {
        final String binaryLocation = metadata.getUserMetaDataOf(BINARY_LOCATION);
        final String binaryType = metadata.getUserMetaDataOf(BINARY_TYPE);
        return ofNullable(binaryLocation).map(rdf::createIRI)
            .map(loc -> BinaryMetadata.builder(loc).mimeType(binaryType).build());
    }

    @Override
    public Stream<Quad> stream() {
        final Dataset dataset = create();
        try (final InputStream input = client.getObject(req).getObjectContent()) {
            RDFParser.source(input).lang(NQUADS).parse(dataset);
        } catch (final IOException ex) {
            dataset.close();
            throw new TrellisRuntimeException("Error parsing input from S3", ex);
        }
        if (getConfig().getOptionalValue(CONFIG_AWS_LDP_TYPE, Boolean.class).orElse(Boolean.TRUE)) {
            final Model model = createDefaultModel();
            model.createResource(getIdentifier().getIRIString())
                .addProperty(type, model.createResource(getInteractionModel().getIRIString()));
            dataset.addNamedModel(Trellis.PreferServerManaged.getIRIString(), model);
        }
        return fromJena(dataset.asDatasetGraph()).stream().map(Quad.class::cast).onClose(dataset::close);
    }
}
