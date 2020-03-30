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
package org.trellisldp.ext.webapp;

import static javax.ws.rs.client.ClientBuilder.newBuilder;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

import java.net.URL;

import javax.ws.rs.client.Client;

import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.trellisldp.test.AbstractApplicationMementoTests;

@DisabledOnOs(WINDOWS)
@EnabledIfSystemProperty(named = "trellis.test.quarkus.embedded-pgsql", matches = "true")
@QuarkusTest
class EmbeddedPgsqlMementoTest extends AbstractApplicationMementoTests {

    private static final Client client = newBuilder().build();
    private static EmbeddedPostgres pg;

    @TestHTTPResource
    URL url;

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public String getBaseURL() {
        return url.toString();
    }

    @BeforeAll
    static void setUp() throws Exception {
        pg = EmbeddedPostgres.builder()
            .setDataDirectory("/tmp/testing/" + "pgdata-" + new RandomStringGenerator
                    .Builder().withinRange('a', 'z').build().generate(10)).start();
        System.setProperty("quarkus.datasource.username", "postgres");
        System.setProperty("quarkus.datasource.password", "postgres");
        System.setProperty("quarkus.datasource.url", "jdbc:postgresql://localhost:" + pg.getPort() + "/postgres");
    }

    @AfterAll
    static void tearDown() throws Exception {
        System.clearProperty("quarkus.datasource.url");
        System.clearProperty("quarkus.datasource.username");
        System.clearProperty("quarkus.datasource.password");
        pg.close();
    }
}