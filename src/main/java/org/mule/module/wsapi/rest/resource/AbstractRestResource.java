
package org.mule.module.wsapi.rest.resource;

import static org.mule.module.wsapi.rest.action.ActionType.EXISTS;
import static org.mule.module.wsapi.rest.action.ActionType.RETRIEVE;

import org.mule.api.MuleEvent;
import org.mule.module.wsapi.rest.RestException;
import org.mule.module.wsapi.rest.RestRequest;
import org.mule.module.wsapi.rest.action.ActionNotSupportedException;
import org.mule.module.wsapi.rest.action.ActionType;
import org.mule.module.wsapi.rest.action.RestAction;
import org.mule.transport.NullPayload;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRestResource implements RestResource
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String name;
    protected List<RestAction> actions;

    public AbstractRestResource(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public List<RestAction> getActions()
    {
        return actions;
    }

    public void setActions(List<RestAction> actions)
    {
        this.actions = actions;
    }

    private RestAction getAction(ActionType actionType)
    {
        RestAction action = null;
        for (RestAction a : getActions())
        {
            if (a.getType() == actionType)
            {
                action = a;
                break;
            }
        }
        return action;
    }

    protected RestAction getAction(ActionType actionType, MuleEvent muleEvent)
        throws ActionNotSupportedException
    {
        if (!isActionSupported(actionType))
        {
            throw new ActionNotSupportedException(this, actionType);
        }
        RestAction action = getAction(actionType);
        if (action == null && EXISTS == actionType)
        {
            action = getAction(RETRIEVE);
        }
        if (action == null)
        {
            throw new ActionNotSupportedException(this, actionType);
        }
        return action;
    }

    @Override
    public boolean isActionSupported(ActionType actionType)
    {
        return getSupportedActions().contains(actionType);
    }

    @Override
    public MuleEvent handle(RestRequest restCall) throws RestException
    {
        return processResource(restCall);
    }

    protected MuleEvent processResource(RestRequest restCall) throws RestException
    {
        try
        {
            this.getAction(restCall.getProtocolAdaptor().getActionType(), restCall.getMuleEvent()).handle(restCall);
            if (ActionType.EXISTS == restCall.getProtocolAdaptor().getActionType())
            {
                restCall.getMuleEvent().getMessage().setPayload(NullPayload.getInstance());
            }
        }
        catch (RestException rana)
        {
            restCall.getProtocolAdaptor().handleException(rana, restCall.getMuleEvent());
        }
        return restCall.getMuleEvent();
    }

}