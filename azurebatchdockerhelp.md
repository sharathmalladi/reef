## Azure Batch with docker containers
Azure Batch has the functionality to run jobs and tasks within a docker container. Using docker containers to execute REEF jobs has the benefit of setting up the dependencies in light-weight docker containers instead of the vm node. This section describes how you can configure the pool and REEF to execute REEF jobs inside docker containers.

####	1. Create Dockerfile for your OS with REEF and other dependencies 
##### Option: Windows Docker image

Following example of dockerfile targets the windowsservercode on dockerhub and the pre-requisites for java and other path variables (the reference to vcruntime140.dll is since the default Windows Server core image is missing this important dll to be able to run java programs):

````
FROM microsoft/windowsservercore:latest
ADD http://javadl.oracle.com/webapps/download/AutoDL?BundleId=207775 c:\jre-8u91-windows-x64.exe
RUN powershell -Command Start-Process -FilePath C:\jre-8u91-windows-x64.exe -PassThru -Wait -ArgumentList \"/s /L c:\Java64.log\"
ENV 'JAVA_HOME' 'C:\Program Files\Java\jre1.8.0_91\'
ENV 'PATH' 'C:\Windows\system32;C:\Windows;C:\Windows\System32\Wbem;C:\Windows\System32\WindowsPowerShell\v1.0\;C:\Users\ContainerAdministrator\AppData\Local\Microsoft\WindowsApps;C:\Program Files\Java\jre1.8.0_91\bin\'
ADD vcruntime140.dll C:/Windows/System32/vcruntime140.dll
RUN del c:\jre-8u91-windows-x64.exe

# Add your own dependencies here
````

##### Option: Ubuntu Docker image

The following example installs the jdk and necessary utilities required by REEF:
````
FROM ubuntu
RUN apt-get update && apt-get install -y default-jdk unzip
ENV JAVA_HOME /usr/bin/java

# Add your own dependencies here
````

#### 2. [Create a docker image from the dockerfile and publish the docker image to Azure Container service.](https://docs.microsoft.com/en-us/azure/container-service/kubernetes/container-service-tutorial-kubernetes-prepare-acr)

#### 3. Create a Pool in the Azure Batch account for [Container workloads](https://docs.microsoft.com/en-us/azure/batch/batch-docker-container-workloads)  
Use the following settings when creating the pool:  
*	Set the Max tasks per node to be one (more on this [below](#Limitations)).  
*	Enable Inter-node communication option.

#### 4. Configure REEF to use the docker containers on the pool
Add the following additional properties to the Runtime Configuration:  

````csharp
return AzureBatchRuntimeClientConfiguration.ConfigurationModule
    // All other configuration that applies to Azure Batch pools without containers is also required here. The following is additional configuration that is required.
    .Set(AzureBatchRuntimeClientConfiguration.ContainerRegistryServer, @" mycontainerservice.azurecr.io")
    .Set(AzureBatchRuntimeClientConfiguration.ContainerRegistryUsername, @"<registry name from container service – Access Keys section>")
    .Set(AzureBatchRuntimeClientConfiguration.ContainerRegistryPassword, @"<password from container service – Access Keys section>")
    .Set(AzureBatchRuntimeClientConfiguration.ContainerImageName, @" mycontainerservice.azurecr.io/mydockerimage")
    // Provide at least two ports below that must be reserved for docker container execution on the vm nodes.
    .Set(AzureBatchRuntimeClientConfiguration.AzureBatchPoolDriverPortsList, new List<string> { "2000", "2001" };)
````

#### Limitations
The following imposes the limitation on us to restricts from executing more than a single docker container at a time on a node. Due to this limitation, you are required to set the number of Max Tasks per Node to be one when you create the Azure batch pool for containers:  
 
 The docker container can be configured to map ports on the host vm node to ports on the container. This allows the host vm node to expose the server within the container as a server externally and thereby allow communication between the distributed components. These port mappings must be set when creating the docker container. The container creation succeeds only if it can bind to all the given ports which results in **all of these ports being unavailable** to any other containers that may run on that host vm node. However, Azure Batch does not expose any logic to discover available ports on the vm node. This means that we cannot run more than one container at any given time in order to warrant that the container will start successfully.
