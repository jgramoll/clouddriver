/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.google.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.services.compute.model.Address
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.google.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.google.cache.Keys
import com.netflix.spinnaker.clouddriver.google.security.GoogleNamedAccountCredentials
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.google.cache.Keys.Namespace.ADDRESSES

@Slf4j
class GoogleRegionalAddressCachingAgent extends AbstractGoogleCachingAgent {

  final String region

  final Set<AgentDataType> providedDataTypes = [
      AUTHORITATIVE.forType(ADDRESSES.ns)
  ] as Set

  String agentType = "$accountName/$region/$GoogleRegionalAddressCachingAgent.simpleName"

  GoogleRegionalAddressCachingAgent(String clouddriverUserAgentApplicationName,
                                    GoogleNamedAccountCredentials credentials,
                                    ObjectMapper objectMapper,
                                    Registry registry,
                                    String region) {
    super(clouddriverUserAgentApplicationName,
          credentials,
          objectMapper,
          registry)
    this.region = region
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    List<Address> addresses = loadAddresses()
    buildCacheResult(providerCache, addresses)
  }

  List<Address> loadAddresses() {
    timeExecute(compute.addresses().list(project, region),
                "compute.addresses.list",
                TAG_SCOPE,
                SCOPE_GLOBAL).items as List
  }

  private CacheResult buildCacheResult(ProviderCache _, List<Address> addressList) {
    log.debug("Describing items in ${agentType}")

    def cacheResultBuilder = new CacheResultBuilder()

    addressList.each { Address address ->
      def addressKey = Keys.getAddressKey(accountName, region, address.getName())

      cacheResultBuilder.namespace(ADDRESSES.ns).keep(addressKey).with {
        attributes.address = address
      }
    }

    log.debug("Caching ${cacheResultBuilder.namespace(ADDRESSES.ns).keepSize()} items in ${agentType}")

    cacheResultBuilder.build()
  }
}
