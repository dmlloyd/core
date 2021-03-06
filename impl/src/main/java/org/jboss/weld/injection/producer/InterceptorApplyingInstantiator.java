/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.injection.producer;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.weld.bean.proxy.CombinedInterceptorAndDecoratorStackMethodHandler;
import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.exceptions.DeploymentException;
import org.jboss.weld.injection.AroundConstructCallback;
import org.jboss.weld.interceptor.proxy.DefaultInvocationContextFactory;
import org.jboss.weld.interceptor.proxy.InterceptionContext;
import org.jboss.weld.interceptor.proxy.InterceptorMethodHandler;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * A wrapper over {@link SubclassedComponentInstantiator} that registers interceptors within the method handler. This class is
 * thread-safe.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public class InterceptorApplyingInstantiator<T> extends ForwardingInstantiator<T> {

    private final InterceptionModel<ClassMetadata<?>, ?> interceptionModel;

    public InterceptorApplyingInstantiator(Instantiator<T> delegate, InterceptionModel<ClassMetadata<?>, ?> model) {
        super(delegate);
        this.interceptionModel = model;
    }

    @Override
    public T newInstance(CreationalContext<T> ctx, BeanManagerImpl manager, AroundConstructCallback<T> callback) {
        InterceptionContext interceptionContext = InterceptionContext.forNonConstructorInterception(interceptionModel, ctx, manager);

        T instance = delegate().newInstance(ctx, manager, callback);

        applyInterceptors(instance, interceptionContext);
        return instance;
    }

    protected T applyInterceptors(T instance, InterceptionContext interceptionContext) {
        try {
            InterceptorMethodHandler methodHandler = new InterceptorMethodHandler(interceptionContext, new DefaultInvocationContextFactory());
            CombinedInterceptorAndDecoratorStackMethodHandler wrapperMethodHandler = (CombinedInterceptorAndDecoratorStackMethodHandler) ((ProxyObject) instance).getHandler();
            wrapperMethodHandler.setInterceptorMethodHandler(methodHandler);
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
        return instance;
    }

    @Override
    public String toString() {
        return "InterceptorApplyingInstantiator for " + delegate();
    }

    @Override
    public boolean hasInterceptorSupport() {
        return true;
    }
}
