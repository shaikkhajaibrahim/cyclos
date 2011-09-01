/*
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Cyclos is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Cyclos; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package nl.strohalm.cyclos.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.utils.ClassHelper;
import nl.strohalm.cyclos.utils.CurrentInvocationData;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.PermissionCheck;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.ServicePermissionsDescriptor;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.logging.LoggingHandler;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * AOP aspect used to log action execution
 * @author luis
 */
@Aspect
public class TraceAspect {

    private FetchService                                    fetchService;
    private PermissionService                               permissionService;
    private final Map<Method, ServicePermissionsDescriptor> cachedDescriptors = new HashMap<Method, ServicePermissionsDescriptor>();
    private LoggingHandler                                  loggingHandler;
    private ThreadLocal<Boolean>                            executing;

    public TraceAspect() {
        executing = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setLoggingHandler(final LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Trace this method execution, checking permission and generating logs
     */
    @Around("target(nl.strohalm.cyclos.services.Service)")
    public Object trace(final ProceedingJoinPoint joinPoint) throws Throwable {
        final boolean alreadyExecuting = executing.get();
        if (!alreadyExecuting) {
            // If not already executing, do the trace
            executing.set(true);
            try {
                // Trace this execution
                return doTrace(joinPoint);
            } catch (final Throwable t) {
                // Store the current exception
                CurrentTransactionData.setError(t);
                throw t;
            } finally {
                // We cannot forget to clear the thread local!
                executing.remove();
            }
        } else {
            // If already executing, just proceed normally
            return joinPoint.proceed();
        }
    }

    /**
     * Returns an permission descriptor instance for the given method
     */
    private ServicePermissionsDescriptor buildPermissionDescriptor(final Method method) {
        final ServicePermissionsDescriptor descriptor = new ServicePermissionsDescriptor();
        descriptor.setMethod(method);

        final DontEnforcePermission dontEnforcePermission = ClassHelper.findAnnotation(method, DontEnforcePermission.class, true);
        if (dontEnforcePermission != null) {
            descriptor.setSkipPermissionCheck(true);
            descriptor.setTraceableAction(dontEnforcePermission.traceable());
            return descriptor;
        } else {
            final SystemAction sysAction = ClassHelper.findAnnotation(method, SystemAction.class);
            final AdminAction adminAction = ClassHelper.findAnnotation(method, AdminAction.class);
            final MemberAction memberAction = ClassHelper.findAnnotation(method, MemberAction.class);
            final BrokerAction brokerAction = ClassHelper.findAnnotation(method, BrokerAction.class);
            final OperatorAction operatorAction = ClassHelper.findAnnotation(method, OperatorAction.class);

            // Find member-related annotations on method, and, if not found, on class
            final RelatedEntity relatedEntity = ClassHelper.findAnnotation(method, RelatedEntity.class, true);
            final PathToMember pathToMember = ClassHelper.findAnnotation(method, PathToMember.class, true);
            final IgnoreMember ignoreMember = ClassHelper.findAnnotation(method, IgnoreMember.class);

            // Build the descriptor
            descriptor.setSystemAction(sysAction != null);
            descriptor.setPermissionService(permissionService);
            descriptor.setFetchService(fetchService);
            descriptor.setAnnotations(adminAction, memberAction, brokerAction, operatorAction);
            descriptor.setRelatedEntity(relatedEntity);
            descriptor.setPathToMember(pathToMember);
            descriptor.setIgnoreMember(ignoreMember != null);

            return descriptor;
        }
    }

    /**
     * Execute the trace
     */
    private Object doTrace(final ProceedingJoinPoint joinPoint) throws Throwable {
        // Retrieve the method reference
        final Signature signature = joinPoint.getSignature();
        final Method method = ((MethodSignature) signature).getMethod();

        // Retrieve the permission descriptor for that method
        final ServicePermissionsDescriptor descriptor = getPermissionDescriptor(method);

        if (descriptor.isSkipPermissionCheck() || (descriptor.isForSystem() && CurrentInvocationData.isSystemInvocation())) {
            // only log client invocations not annotated with the DontEnforcePermission annotation or marked as traceable.
            if ((!descriptor.isSkipPermissionCheck() || descriptor.isTraceableAction()) && !CurrentInvocationData.isSystemInvocation()) {
                return executeAndLogAction(null, joinPoint);
            } else {
                // This is not an action, proceed normally
                return joinPoint.proceed();
            }
        } else if (descriptor.isOnlyForSystem() && !CurrentInvocationData.isSystemInvocation()) {
            throw new PermissionDeniedException();
        } else if (!descriptor.isAnnotated()) {
            throw new IllegalArgumentException("The method '" + method + "' is not secured correctly. It must be annotated using some of the security annotations.");
        } else {
            // This is an action - verify related member permission
            final Object[] args = joinPoint.getArgs();
            final PermissionCheck check = descriptor.checkPermission(args.length == 0 ? null : args[0]);
            if (!check.isGranted()) {
                // Determine if log is being generated
                final boolean generateLog = loggingHandler.isTraceEnabled() && LoggedUser.isValid();

                if (generateLog) {
                    loggingHandler.logPermissionDenied(LoggedUser.user(), method, args);
                }
                throw new PermissionDeniedException();
            }

            // Log the action execution
            return executeAndLogAction(check, joinPoint);
        }
    }

    private Object executeAndLogAction(final PermissionCheck check, final ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = null;
        Signature signature = null;
        Method method = null;
        Permission permission = null;

        // Determine if log is being generated
        final boolean generateLog = loggingHandler.isTraceEnabled() && LoggedUser.isValid();

        if (generateLog) {
            args = joinPoint.getArgs();
            signature = joinPoint.getSignature();
            method = ((MethodSignature) signature).getMethod();
            permission = check == null ? null : check.getPermission();
        }
        try {
            final Object retVal = joinPoint.proceed();
            if (generateLog) {
                loggingHandler.trace(LoggedUser.remoteAddress(), LoggedUser.user(), permission, method, args, retVal);
            }
            return retVal;
        } catch (final Throwable t) {
            if (generateLog) {
                loggingHandler.traceError(LoggedUser.user(), permission, method, args, t);
            }
            throw t;
        }
    }

    /**
     * Returns the cached permission descriptor for this method, or creates a new one on a cache miss
     */
    private ServicePermissionsDescriptor getPermissionDescriptor(final Method method) {
        ServicePermissionsDescriptor descriptor = cachedDescriptors.get(method);
        if (descriptor == null) {
            descriptor = buildPermissionDescriptor(method);
            cachedDescriptors.put(method, descriptor);
        }
        return descriptor;
    }

}
