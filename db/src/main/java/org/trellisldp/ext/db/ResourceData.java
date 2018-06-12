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

import java.util.HashMap;
import java.util.Map;

import org.trellisldp.api.Binary;

/**
 * A simple Data POJO.
 */
class ResourceData {

    public String interactionModel;
    public Long modified;
    public String isPartOf;
    public Boolean hasAcl = false;
    public Boolean isDeleted = false;

    public String membershipResource;
    public String hasMemberRelation;
    public String isMemberOfRelation;
    public String insertedContentRelation;

    public Binary binary;
    public Map<String, String> extra = new HashMap<>();
}