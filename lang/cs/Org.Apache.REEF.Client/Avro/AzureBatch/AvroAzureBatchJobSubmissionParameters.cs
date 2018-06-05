﻿// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
// 
//   http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

using System.Collections.Generic;
using System.Runtime.Serialization;
using Org.Apache.REEF.Utilities.Attributes;

namespace Org.Apache.REEF.Client.Avro.AzureBatch
{
    /// <summary>
    /// Used to serialize and deserialize Avro record 
    /// org.apache.reef.reef.bridge.client.avro.AvroAzureBatchJobSubmissionParameters.
    /// This is a (mostly) auto-generated class. 
    /// For instructions on how to regenerate, please view the README.md in the same folder.
    /// </summary>
    [Private]
    [DataContract(Namespace = "org.apache.reef.reef.bridge.client.avro")]
    public sealed class AvroAzureBatchJobSubmissionParameters
    {
        private const string JsonSchema = @"{""type"":""record"",""name"":""org.apache.reef.reef.bridge.client.avro.AvroAzureBatchJobSubmissionParameters"",""doc"":""Job submission parameters used by the Azure Batch runtime"",""fields"":[{""name"":""sharedJobSubmissionParameters"",""type"":{""type"":""record"",""name"":""org.apache.reef.reef.bridge.client.avro.AvroJobSubmissionParameters"",""doc"":""General cross-language job submission parameters shared by all runtimes"",""fields"":[{""name"":""jobId"",""type"":""string""},{""name"":""jobSubmissionFolder"",""type"":""string""}]}},{""name"":""AzureBatchAccountKey"",""type"":""string""},{""name"":""AzureBatchAccountName"",""type"":""string""},{""name"":""AzureBatchAccountUri"",""type"":""string""},{""name"":""AzureBatchPoolId"",""type"":""string""},{""name"":""AzureStorageAccountKey"",""type"":""string""},{""name"":""AzureStorageAccountName"",""type"":""string""},{""name"":""AzureStorageContainerName"",""type"":""string""},{""name"":""AzureBatchIsWindows"",""type"":""boolean""},{""name"":""AzureBatchPoolDriverPortsList"",""type"":{""type"": ""array"", ""items"": ""string""}}]}";

        /// <summary>
        /// Gets the schema.
        /// </summary>
        public static string Schema
        {
            get
            {
                return JsonSchema;
            }
        }

        /// <summary>
        /// Gets or sets the sharedJobSubmissionParameters field.
        /// </summary>
        [DataMember]
        public AvroJobSubmissionParameters sharedJobSubmissionParameters { get; set; }

        /// <summary>
        /// Gets or sets the AzureBatchAccountKey field.
        /// </summary>
        [DataMember]
        public string AzureBatchAccountKey { get; set; }

        /// <summary>
        /// Gets or sets the AzureBatchAccountName field.
        /// </summary>
        [DataMember]
        public string AzureBatchAccountName { get; set; }

        /// <summary>
        /// Gets or sets the AzureBatchAccountUri field.
        /// </summary>
        [DataMember]
        public string AzureBatchAccountUri { get; set; }

        /// <summary>
        /// Gets or sets the AzureBatchPoolId field.
        /// </summary>
        [DataMember]
        public string AzureBatchPoolId { get; set; }

        /// <summary>
        /// Gets or sets the AzureStorageAccountKey field.
        /// </summary>
        [DataMember]
        public string AzureStorageAccountKey { get; set; }

        /// <summary>
        /// Gets or sets the AzureStorageAccountName field.
        /// </summary>
        [DataMember]
        public string AzureStorageAccountName { get; set; }

        /// <summary>
        /// Gets or sets the AzureStorageContainerName field.
        /// </summary>
        [DataMember]
        public string AzureStorageContainerName { get; set; }

        /// <summary>
        /// Gets or sets the AzureBatchIsWindows field.
        /// </summary>
        [DataMember]
        public bool AzureBatchIsWindows { get; set; }

        /// <summary>
        /// Gets or sets the AzureBatchPoolDriverPortsList field.
        /// </summary>
        [DataMember]
        public IList<string> AzureBatchPoolDriverPortsList { get; set; }

        /// <summary>
        /// Initializes a new instance of the <see cref="AvroAzureBatchJobSubmissionParameters"/> class.
        /// </summary>
        public AvroAzureBatchJobSubmissionParameters()
        {
        }
    }
}
