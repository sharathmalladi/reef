## Azure Batch with Docker containers
Azure Batch has the functionality to run jobs and tasks within a Docker container. Using Docker containers to execute REEF jobs has the benefit of isolating the runtime dependencies in light-weight Docker containers instead of the vm node. This section describes how you can configure the pool and REEF to execute REEF jobs inside Docker containers.

#### 1. Create Dockerfile for your OS with REEF and other dependencies 
The Docker container must be configured to execute REEF jobs since the jobs will execute within the Docker container environment. You can add additional dependencies as you see fit. Following are the dockerfiles listing dependencies required for REEF for Windows and Linux based containers.

##### Option: Windows Docker image

The following example of dockerfile targets the windowsservercode on dockerhub and the pre-requisites for java and other path variables:

````dockerfile
FROM microsoft/windowsservercore:latest
ADD http://javadl.oracle.com/webapps/download/AutoDL?BundleId=207775 c:\jre-8u91-windows-x64.exe
RUN powershell -Command Start-Process -FilePath C:\jre-8u91-windows-x64.exe -PassThru -Wait -ArgumentList \"/s /L c:\Java64.log\"
ENV 'JAVA_HOME' 'C:\Program Files\Java\jre1.8.0_91\'
ENV 'PATH' 'C:\Windows\system32;C:\Windows;C:\Windows\System32\Wbem;C:\Windows\System32\WindowsPowerShell\v1.0\;C:\Users\ContainerAdministrator\AppData\Local\Microsoft\WindowsApps;C:\Program Files\Java\jre1.8.0_91\bin\'
# The following reference is added since the default Windows Server core 
# image is missing this important dll to be able to run java programs
ADD vcruntime140.dll C:/Windows/System32/vcruntime140.dll
RUN del c:\jre-8u91-windows-x64.exe

# Add your own dependencies here
````

##### Option: Ubuntu Docker image

The following example of dockerfile installs the jdk and necessary utilities required by REEF:
````dockerfile
FROM ubuntu
RUN apt-get update && apt-get install -y default-jdk unzip
ENV JAVA_HOME /usr/bin/java

# Add your own dependencies here
````

#### 2. [Create a Docker image from the dockerfile and publish the Docker image to Azure Container service.](https://docs.microsoft.com/en-us/azure/container-service/kubernetes/container-service-tutorial-kubernetes-prepare-acr)

#### 3. Create a Pool in the Azure Batch account for [Container workloads](https://docs.microsoft.com/en-us/azure/batch/batch-docker-container-workloads)  
Use the following settings when creating the pool:  
*    Set the Max tasks per node to be one (more on this [below](#Limitations)).  
*    Enable Inter-node communication option.
*   If REEF client-driver communication is necessary, you will also need to [configure the pool](TBD: Link to "How to configure REEF .NET Driver Client communication on Azure Batch" from the wiki page) to use these same set of ports.

#### 4. Configure REEF to use the Docker containers on the pool
Add the following additional properties to the Runtime Configuration:  

````csharp
return AzureBatchRuntimeClientConfiguration.ConfigurationModule
    // All other configuration that applies to Azure Batch pools without containers is also required here. The following is additional configuration that is required.
    .Set(AzureBatchRuntimeClientConfiguration.ContainerRegistryServer, @" mycontainerservice.azurecr.io")
    .Set(AzureBatchRuntimeClientConfiguration.ContainerRegistryUsername, @"<registry name from container service – Access Keys section>")
    .Set(AzureBatchRuntimeClientConfiguration.ContainerRegistryPassword, @"<password from container service – Access Keys section>")
    .Set(AzureBatchRuntimeClientConfiguration.ContainerImageName, @" mycontainerservice.azurecr.io/mydockerimage")
    // Provide at least three ports below that must be reserved for Docker container execution on the vm nodes (one each for http server, wake and name server).
    .Set(AzureBatchRuntimeClientConfiguration.AzureBatchPoolDriverPortsList, new List<string> { "2000", "2001", "2002" };)
````

#### Limitations

Containers must be limited to a single docker container per node at a time. This is done by setting the number of **Max Tasks per Node** to be one when you create the Azure batch pool for containers. Following is a brief explanation about this limitation:  
 
 For a service to communicate to clients external to the container, it must be executing on ports that are [explicitly mapped to ports](https://docs.docker.com/engine/reference/run/#expose-incoming-ports) at the host level. The port mapping can only be set when creating the Docker container. Since we do not have knowledge of the ports available to us from Azure Batch, we are limited to executing only one container at a time with a predefined port list to ensure that it will start successfully.