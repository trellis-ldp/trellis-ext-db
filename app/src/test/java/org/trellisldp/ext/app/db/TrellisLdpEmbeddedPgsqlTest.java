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
package org.trellisldp.ext.app.db;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.io.File.separator;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import io.dropwizard.testing.DropwizardTestSupport;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.client.Client;

import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.trellisldp.test.AbstractApplicationLdpTests;

/**
 * Run LDP-Related Tests.
 */
@DisabledOnOs(WINDOWS)
@DisabledIfEnvironmentVariable(named = "TRAVIS", matches = "true")
public class TrellisLdpEmbeddedPgsqlTest extends AbstractApplicationLdpTests {

    private static EmbeddedPostgres pg;
    private static DropwizardTestSupport<AppConfiguration> APP;
    private static Client CLIENT;

    @BeforeAll
    public static void setup() throws Exception {
        pg = EmbeddedPostgres.builder()
            .setDataDirectory(resourceFilePath("data") + separator + "pgdata-" + new RandomStringGenerator
                    .Builder().withinRange('a', 'z').build().generate(10)).start();

        APP = TestUtils.buildPgsqlApp("jdbc:postgresql://localhost:" + pg.getPort() + "/postgres",
                "postgres", "postgres");
        APP.getApplication().run("db", "migrate", resourceFilePath("trellis-config.yml"));
        CLIENT = TestUtils.buildClient(APP);
    }

    @Override
    public Client getClient() {
        return CLIENT;
    }

    @Override
    public String getBaseURL() {
        return "http://localhost:" + APP.getLocalPort() + "/";
    }

    @Override
    public Set<String> supportedJsonLdProfiles() {
        return singleton("http://www.w3.org/ns/anno.jsonld");
    }

    @AfterAll
    public static void cleanup() throws IOException {
        APP.after();
        pg.close();
    }
}
