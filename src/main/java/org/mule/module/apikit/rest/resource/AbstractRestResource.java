/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.apikit.rest.resource;

import static org.mule.module.apikit.rest.operation.RestOperationType.EXISTS;
import static org.mule.module.apikit.rest.operation.RestOperationType.RETRIEVE;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.apikit.rest.RestException;
import org.mule.module.apikit.rest.RestRequest;
import org.mule.module.apikit.rest.operation.AbstractRestOperation;
import org.mule.module.apikit.rest.operation.OperationNotAllowedException;
import org.mule.module.apikit.rest.operation.RestOperation;
import org.mule.module.apikit.rest.operation.RestOperationType;
import org.mule.module.apikit.rest.param.RestParameter;
import org.mule.module.apikit.rest.representation.RepresentationMetaData;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRestResource implements RestResource
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String name;
    protected String description = "";
    protected List<RestOperation> operations = new ArrayList<RestOperation>();
    protected List<RestParameter> parameters = new ArrayList<RestParameter>();
    protected Collection<RepresentationMetaData> representations = new HashSet<RepresentationMetaData>();
    protected RestResource parentResource;
    protected RestOperation swaggerOperation;

    public AbstractRestResource(String name, RestResource parentResource)
    {
        this.name = name;
        this.parentResource = parentResource;
        if (parentResource != null)
        {
            parameters.addAll(parentResource.getParameters());
        }
    }

    public String getName()
    {
        return name;
    }

    public List<RestOperation> getOperations()
    {
        return operations;
    }

    public void setOperations(List<RestOperation> operations)
    {
        this.operations = operations;
        for (RestOperation operation : operations)
        {
            if (operation instanceof AbstractRestOperation)
            {
                ((AbstractRestOperation) operation).setResource(this);
            }
        }
    }

    public RestOperation getOperation(RestOperationType operationType)
    {
        if (RestOperationType.OPTIONS == operationType)
        {
            return new OptionsOperation(this);
        }
        RestOperation action = null;
        for (RestOperation a : getOperations())
        {
            if (a.getType() == operationType)
            {
                action = a;
                break;
            }
        }
        return action;
    }

    protected RestOperation getAction(RestOperationType actionType, RestRequest request)
        throws OperationNotAllowedException
    {
        if (!getSupportedActionTypes().contains(actionType))
        {
            throw new OperationNotAllowedException(this, actionType);
        }
        RestOperation action = getOperation(actionType);
        if (action == null && EXISTS == actionType)
        {
            action = useRetrieveAsExists();
        }
        if (action == null)
        {
            throw new OperationNotAllowedException(this, actionType);
        }
        return action;
    }

    private RestOperation useRetrieveAsExists()
    {
        RestOperation retrieve = getOperation(RETRIEVE);
        if (retrieve == null)
        {
            return null;
        }
        AbstractRestOperation retrieveOperation = new ExistsByRetrieveOperation(retrieve);
        retrieveOperation.setResource(this);
        return retrieveOperation;
    }

    @Override
    public boolean isOperationTypeAllowed(RestOperationType actionType)
    {
        for (RestOperation operation : operations)
        {
            if (operation.getType().equals(actionType))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<RestOperationType> getAllowedOperationTypes()
    {
        Set<RestOperationType> allowedTypes = new HashSet<RestOperationType>();
        for (RestOperation operation : operations)
        {
            allowedTypes.add(operation.getType());
        }
        return allowedTypes;
    }

    @Override
    public void handle(RestRequest restCall) throws RestException
    {
        processResource(restCall);
    }

    protected MuleEvent processResource(RestRequest request) throws RestException
    {
        try
        {
            this.getAction(request.getProtocolAdaptor().getOperationType(), request).handle(request);
            if (RestOperationType.EXISTS == request.getProtocolAdaptor().getOperationType())
            {
                request.getMuleEvent().getMessage().setPayload(NullPayload.getInstance());
            }
        }
        catch (RestException rana)
        {
            request.getProtocolAdaptor().handleException(rana, request);
        }
        return request.getMuleEvent();
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    protected abstract Set<RestOperationType> getSupportedActionTypes();

    static class ExistsByRetrieveOperation extends AbstractRestOperation
    {

        private RestOperation retrieve;

        public ExistsByRetrieveOperation(RestOperation retrieve)
        {
            this.retrieve = retrieve;
            this.type = EXISTS;
        }

        @Override
        public MessageProcessor getHandler()
        {
            return retrieve.getHandler();
        }
    }

    public Collection<RepresentationMetaData> getRepresentations()
    {
        return representations;
    }

    public void setRepresentations(Collection<RepresentationMetaData> representations)
    {
        this.representations = representations;
    }

    public String getPath()
    {
        if (parentResource == null)
        {
            return getName();
        }
        else
        {
            return parentResource.getPath() + "/" + getName();
        }
    }

    @Override
    public List<RestParameter> getParameters()
    {
        return parameters;
    }

    public void setParameters(List<RestParameter> parameters)
    {
        this.parameters = parameters;
    }

    public RestResource getParentResource()
    {
        return parentResource;
    }

}
