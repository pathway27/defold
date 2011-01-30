package com.dynamo.cr.client;


import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import com.dynamo.cr.protocol.proto.Protocol.ApplicationInfo;
import com.dynamo.cr.protocol.proto.Protocol.BranchList;
import com.dynamo.cr.protocol.proto.Protocol.BranchStatus;
import com.dynamo.cr.protocol.proto.Protocol.LaunchInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;

public class ProjectClient extends BaseClient implements IProjectClient {

    private ClientFactory factory;
    private URI uri;

    // NOTE: Only public for package
    ProjectClient(ClientFactory factory, URI uri, Client client) {
        this.factory = factory;
        this.uri = uri;
        this.client = client;
        resource = client.resource(uri);
    }

    @Override
    public void createBranch(String branch) throws RepositoryException {
        wrapPut(String.format("/branches/%s", branch));
    }

    @Override
    public BranchStatus getBranchStatus(String branch) throws RepositoryException {
        return wrapGet(String.format("/branches/%s", branch), BranchStatus.class);
    }

    @Override
    public BranchList getBranchList() throws RepositoryException {
        return wrapGet(String.format("/branches"), BranchList.class);
    }

    @Override
    public void deleteBranch(String branch) throws RepositoryException {
        wrapDelete(String.format("/branches/%s", branch));
    }


    @Override
    public BranchClient getBranchClient(String branch) {
        return new BranchClient(factory, UriBuilder.fromUri(uri).path("/branches/" + branch).build(), client);
    }

    @Override
    public LaunchInfo getLaunchInfo() throws RepositoryException {
        return wrapGet("/launch_info", LaunchInfo.class);
    }

    @Override
    public ApplicationInfo getApplicationInfo(String platform)
            throws RepositoryException {

        try {
            ClientResponse resp = resource.path("/application_info").queryParam("platform", platform).get(ClientResponse.class);
            if (resp.getStatus() != 200) {
                throwRespositoryException(resp);
            }
            return resp.getEntity(ApplicationInfo.class);
        }
        catch (ClientHandlerException e) {
            throwRespositoryException(e);
            return null; // Never reached
        }
    }

    @Override
    public InputStream getApplicationData(String platform)
            throws RepositoryException {
        try {
            ClientResponse resp = resource.path("/application_data").queryParam("platform", platform).get(ClientResponse.class);
            if (resp.getStatus() != 200) {
                throwRespositoryException(resp);
            }
            return resp.getEntityInputStream();
        }
        catch (ClientHandlerException e) {
            throwRespositoryException(e);
            return null; // Never reached
        }
    }

}
