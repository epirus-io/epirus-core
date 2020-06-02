/*
 * Copyright 2020 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.epirus.web3j;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.web3j.protocol.Network;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.http.HttpService;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EpirusHttpTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @Test
    public void testHttpServiceEnvironmentVariable() throws Exception {
        withEnvironmentVariable("EPIRUS_LOGIN_TOKEN", "token")
                .and("EPIRUS_APP_URL", "http://localhost:8000")
                .execute(
                        () -> {
                            HttpService service =
                                    EpirusHttpServiceProvider.getEpirusHttpService(Network.RINKEBY);
                            assertEquals(
                                    "http://localhost:8000/api/rpc/rinkeby/token/",
                                    service.getUrl());
                        });
    }

    @Test
    public void testEpirusPlatformWorksAgainstMock() throws Exception {
        stubFor(
                post(urlPathMatching("/api/rpc/mainnet/token/"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\n"
                                                        + "  \"id\":67,\n"
                                                        + "  \"jsonrpc\":\"2.0\",\n"
                                                        + "  \"result\": \"Mist/v0.9.3/darwin/go1.4.1\"\n"
                                                        + "}")));

        withEnvironmentVariable("EPIRUS_LOGIN_TOKEN", "token")
                .and("EPIRUS_APP_URL", wireMockServer.baseUrl())
                .execute(
                        () -> {
                            Web3j web3j = Epirus.buildWeb3j();

                            String netVersion =
                                    web3j.web3ClientVersion().send().getWeb3ClientVersion();
                            assertEquals("Mist/v0.9.3/darwin/go1.4.1", netVersion);
                        });
    }

    @Test
    @Disabled("requires the CLI to be logged in to pass")
    public void testEpirusPlatformWorksAgainstProd() throws Exception {
        Web3j web3j = Epirus.buildWeb3j();
        String netVersion =
                web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), false)
                        .send()
                        .getBlock()
                        .getHash();
        System.out.println(netVersion);
    }
}
