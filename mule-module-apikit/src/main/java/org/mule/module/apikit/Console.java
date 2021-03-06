/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;

import java.util.Collection;

import org.raml.model.Raml;

public class Console implements MessageProcessor, Initialisable, MuleContextAware, FlowConstructAware
{

    private AbstractConfiguration config;
    private MuleContext muleContext;
    private ConsoleHandler consoleHandler;
    private FlowConstruct flowConstruct;
    protected RamlDescriptorHandler ramlHandler;

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public void setConfig(AbstractConfiguration config)
    {
        this.config = config;
    }

    public AbstractConfiguration getConfig()
    {
        return config;
    }

    private Raml getApi()
    {
        return getConfig().getApi();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        //avoid spring initialization
        if (flowConstruct == null)
        {
            return;
        }
        if (config == null)
        {
            Collection<AbstractConfiguration> configurations = AbstractConfiguration.getAllConfigurations(muleContext);
            if (configurations.size() != 1)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("APIKit configuration not Found"), this);
            }
            config = configurations.iterator().next();
        }
        consoleHandler = new ConsoleHandler(getConfig().getEndpointAddress(flowConstruct));
        config.addConsoleUrl(consoleHandler.getConsoleUrl());
        ramlHandler = new RamlDescriptorHandler(config);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        HttpRestRequest request = new HttpRestRequest(event, getConfig());

        //check for raml descriptor request
        if (ramlHandler.handles(request))
        {
            return ramlHandler.processConsoleRequest(event);
        }

        return consoleHandler.process(event);
    }
}
