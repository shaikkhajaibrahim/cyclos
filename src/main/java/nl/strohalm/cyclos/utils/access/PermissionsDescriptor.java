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
package nl.strohalm.cyclos.utils.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.access.Module;
import nl.strohalm.cyclos.entities.access.Module.Type;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * It contains data used to control business logic execution
 * @author luis
 */
public abstract class PermissionsDescriptor {
    private boolean                         annotated;
    private Map<Group.Nature, Boolean>      annotationsByType      = new EnumMap<Group.Nature, Boolean>(Group.Nature.class);
    private Map<Group.Nature, Boolean>      ignoreMemberByType     = new EnumMap<Group.Nature, Boolean>(Group.Nature.class);
    private Map<Group.Nature, Permission[]> permissionsByType      = new EnumMap<Group.Nature, Permission[]>(Group.Nature.class);
    private RelatedEntity                   relatedEntity;
    private PathToMember                    pathToMember;

    /* This flag enforces that all related member must be checked regarding the user management relationship */
    private boolean                         checkAllRelatedMembers = true;

    private PermissionService               permissionService;

    private FetchService                    fetchService;

    /**
     * Check if the logged user has permission over his related member data for this action
     */
    public PermissionCheck checkPermission(final IPermissionRequestor permissionRequestor) {
        return checkPermission(permissionRequestor, null);
    }

    /**
     * Check if the logged user has permission over his related member data for this action
     * @param arg it's used to retrieve the related members
     */
    public PermissionCheck checkPermission(final Object arg) {
        return checkPermission(null, arg);
    }

    public RelatedEntity getRelatedEntity() {
        return relatedEntity;
    }

    public boolean isAnnotated() {
        return annotated;
    }

    public void setActionForNature(final Group.Nature groupNature, final boolean isRequiredAction) {
        annotationsByType.put(groupNature, isRequiredAction);

        annotated |= isRequiredAction;
    }

    public void setAnnotations(final AdminAction adminAction, final MemberAction memberAction, final BrokerAction brokerAction, final OperatorAction operatorAction) {
        annotationsByType.put(Group.Nature.ADMIN, adminAction != null);
        permissionsByType.put(Group.Nature.ADMIN, adminAction != null ? adminAction.value() : null);

        annotationsByType.put(Group.Nature.MEMBER, memberAction != null);
        permissionsByType.put(Group.Nature.MEMBER, memberAction != null ? memberAction.value() : null);

        annotationsByType.put(Group.Nature.BROKER, brokerAction != null);
        permissionsByType.put(Group.Nature.BROKER, brokerAction != null ? brokerAction.value() : null);

        annotationsByType.put(Group.Nature.OPERATOR, operatorAction != null);
        permissionsByType.put(Group.Nature.OPERATOR, operatorAction != null ? operatorAction.value() : null);

        annotated = adminAction != null || memberAction != null || brokerAction != null || operatorAction != null;
    }

    public void setCheckAllRelatedMembers(final boolean checkAllRelatedMembers) {
        this.checkAllRelatedMembers = checkAllRelatedMembers;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setIgnoreMember(final boolean ignoreMember) {
        for (final Group.Nature nature : Group.Nature.values()) {
            ignoreMemberByType.put(nature, ignoreMember);
        }
    }

    public void setIgnoreMemberForNature(final Group.Nature groupNature, final boolean ignoreMember) {
        ignoreMemberByType.put(groupNature, ignoreMember);
    }

    public void setPathToMember(final PathToMember pathToMember) {
        this.pathToMember = pathToMember;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setPermissionsForNature(final Group.Nature groupNature, final Permission[] permissions) {
        permissionsByType.put(groupNature, permissions);
    }

    public void setRelatedEntity(final RelatedEntity relatedEntity) {
        this.relatedEntity = relatedEntity;
    }

    private boolean checkForAdmin(final Collection<? extends Element> members) {
        if (members.isEmpty()) {
            return false;
        }

        AdminGroup group = LoggedUser.group();
        group = fetchService.reload(group, AdminGroup.Relationships.MANAGES_GROUPS);
        for (Element member : members) {
            if (!fetchService.isInitialized(member) || member.getGroup() == null) {
                member = fetchService.fetch(member, Element.Relationships.GROUP);
            }
            final Group memberGroup = fetchService.fetch(member.getGroup());
            final boolean isOk = (member.getNature() == Element.Nature.ADMIN ?
                    group.getManagesAdminGroups() : group.getManagesGroups()).contains(memberGroup);
            if (!isOk && checkAllRelatedMembers) {
                return false;
            } else if (isOk && !checkAllRelatedMembers) {
                return true;
            }
        }
        return checkAllRelatedMembers ? true : false;
    }

    private boolean checkForBroker(final Collection<? extends Element> members) {
        if (members.isEmpty()) {
            return false;
        }

        final Member broker = LoggedUser.element();
        for (final Element member : members) {
            final boolean isOk = member instanceof Member && broker.equals(((Member) member).getBroker());
            if (!isOk && checkAllRelatedMembers) {
                return false;
            } else if (isOk && !checkAllRelatedMembers) {
                return true;
            }
        }
        return checkAllRelatedMembers ? true : false;
    }

    private boolean checkForMember(final Collection<? extends Element> members) {
        if (members.isEmpty()) {
            return false;
        }

        final Element element = LoggedUser.element();
        for (final Element current : members) {
            final boolean isOk = element.equals(current);
            if (!isOk && checkAllRelatedMembers) {
                return false;
            } else if (isOk && !checkAllRelatedMembers) {
                return true;
            }
        }
        return checkAllRelatedMembers ? true : false;
    }

    // It's expected a members collection's size of 1
    private boolean checkForOperator(final Collection<? extends Element> members) {
        if (members.isEmpty()) {
            return false;
        }

        final Operator operator = LoggedUser.element();
        for (final Element member : members) {
            final boolean isOk = operator.getMember().equals(member);
            if (!isOk && checkAllRelatedMembers) {
                return false;
            } else if (isOk && !checkAllRelatedMembers) {
                return true;
            }
        }
        return checkAllRelatedMembers ? true : false;
    }

    /**
     * Check if the logged user has permission over his related member data for this action
     */
    private PermissionCheck checkPermission(final IPermissionRequestor permissionRequestor, final Object arg) {
        boolean granted = false;
        Permission perm = null;
        boolean useMemberNature = false;
        Collection<? extends Element> members = Collections.emptyList();

        if (annotated) {
            boolean inferMember = false;
            // (*) this is necessary to support an action annotated for broker and member
            // but logged as broker and working as a member (we need the related members to infer if he is working as member or as broker)
            if (!isIgnoreMember(Group.Nature.MEMBER)) {
                if (permissionRequestor != null) {
                    members = permissionRequestor.managedMembers(Group.Nature.MEMBER);
                    inferMember = CollectionUtils.isNotEmpty(members);
                    useMemberNature = checkForMember(members);
                    if (!useMemberNature) {
                        members = permissionRequestor.managedMembers(LoggedUser.group().getNature());
                    }

                } else if (pathToMember != null) {
                    members = resolveMembers(arg);
                    useMemberNature = checkForMember(members);
                    inferMember = true;
                }
            }

            final Group group = LoggedUser.group();
            Group.Nature nature = useMemberNature ? Group.Nature.MEMBER : group.getNature();
            boolean annotatedForNature = Boolean.TRUE.equals(isAnnotated(nature));
            if (!annotatedForNature && nature == Group.Nature.BROKER && Boolean.TRUE.equals(isAnnotated(Group.Nature.MEMBER))) {
                // A broker and the action is annotated for member, test as member
                nature = Group.Nature.MEMBER;
                annotatedForNature = true;
            }
            if (annotatedForNature) {
                Permission[] permissions = permissionsByType.get(nature);
                if (permissions == null || permissions.length == 0) {
                    // If there's no declared permissions, it's granted
                    granted = true;
                } else {
                    perm = getGrantedPermission(group, permissions);
                    granted = perm != null;
                }
                // this is to support (*) when we don't have related members (e.g. pathTomember is null or ignoreMember is set)
                if (!granted && !inferMember && group.getNature() == Group.Nature.BROKER && isAnnotated(Group.Nature.BROKER) && isAnnotated(Group.Nature.MEMBER)) {
                    permissions = permissionsByType.get(Group.Nature.MEMBER);
                    if (permissions == null || permissions.length == 0) {
                        // If there's no declared permissions, it's granted
                        granted = true;
                    } else {
                        perm = getGrantedPermission(group, permissions);
                        granted = perm != null;
                    }
                }
            }
            if (perm != null && Module.Type.getByModuleName(perm.module()).isRelatedToMember() && !isIgnoreMember(nature) && arg == null && permissionRequestor == null) {
                throw new IllegalStateException("Invalid permission descriptor for '" + this + "' there is no way to load related member data (permission: <" + perm.module() + ", " + perm.operation() + ">");
            } else if (granted) {
                granted = verifyRelatedMember(nature, perm, members, arg, permissionRequestor);
            }
        }

        return new PermissionCheck(granted, perm);
    }

    private boolean checkRelated(final Permission permission, final Collection<? extends Element> members) {
        final Type type = Module.Type.getByModuleName(permission.module());
        boolean checkForBroker = false;
        switch (type) {
            case ADMIN_MEMBER:
            case ADMIN_ADMIN:
                // If logged as admin, check if can manage each member's group
                if (!checkForAdmin(members)) {
                    return false;
                }
                break;

            case BROKER:
                // If logged as broker, check if the each member's broker is the logged user
                if (checkForBroker(members)) {
                    checkForBroker = true;
                }
                // break;
            case MEMBER:
                // If logged as member, check if the each (!? - normally only one :-) member is the logged member himself
                if (!checkForBroker && !checkForMember(members)) {
                    return false;
                }
                break;
            case OPERATOR:
                // If logged as operator check if each member is the operator's member
                if (!checkForOperator(members)) {
                    return false;
                }

                // Additionally, if logged as operator ensure that if the action requires an annotated for member,
                // the operator's member has the permission too
                final Operator operator = LoggedUser.element();
                final Member operatorMember = operator.getMember();
                final Permission[] permissions = getPermissions(Group.Nature.MEMBER);
                if (permissions != null && permissions.length > 0) {
                    final String memberModule = permission.module().replaceAll("operator", "member");
                    final String memberOperation = permission.operation();
                    for (final Permission current : permissions) {
                        if (current.module().equals(memberModule) && current.operation().equals(memberOperation)) {
                            if (!permissionService.checkPermission(operatorMember.getMemberGroup(), memberModule, memberOperation)) {
                                return false;
                            }
                        }
                    }
                }
                break;
        }
        return true;
    }

    /**
     * Recursively fetch the path until the member
     */
    private Element fetchMember(Object bean, final String path) {
        final String name = path;
        String first = PropertyHelper.firstProperty(name);
        String nested = PropertyHelper.nestedPath(name);
        String previousProperty = null;
        Object previousBean = null;

        while (bean != null && first != null) {
            if (bean instanceof Entity) {
                try {
                    final Entity entity = (Entity) bean;
                    if (!entity.isTransient()) {
                        bean = fetchService.fetch(entity, RelationshipHelper.forName(first));
                    }
                } catch (final UnexpectedEntityException e) {
                    // Ignore - It's just null or transient
                }
            }
            final Object value = PropertyHelper.get(bean, first);
            if (PropertyUtils.isWriteable(bean, first)) {
                PropertyHelper.set(bean, first, value);
            }
            previousBean = bean;
            previousProperty = first;

            // Process the next property in the properties list
            bean = value;
            first = PropertyHelper.firstProperty(nested);
            nested = PropertyHelper.nestedPath(nested);
        }

        if (bean instanceof Entity) {
            try {
                final Entity entity = (Entity) bean;
                if (!entity.isTransient()) {
                    bean = fetchService.fetch(entity);
                    if (previousBean != null) {
                        if (PropertyUtils.isWriteable(previousBean, previousProperty)) {
                            PropertyHelper.set(previousBean, previousProperty, bean);
                        }
                    }
                }
            } catch (final UnexpectedEntityException e) {
                // Ignore - It's just null or transient
            }
        }

        return (Element) bean;
    }

    private Permission getGrantedPermission(final Group group, final Permission[] permissions) {
        Permission perm = null;
        for (final Permission permission : permissions) {
            if (permissionService.checkPermission(group, permission.module(), permission.operation())) {
                perm = permission;
                break;
            }
        }
        return perm;
    }

    private PathToMember getPathToMember() {
        return pathToMember;
    }

    private Permission[] getPermissions(final Group.Nature nature) {
        return permissionsByType.get(nature);
    }

    private Boolean isAnnotated(final Group.Nature nature) {
        return annotationsByType.get(nature);
    }

    private boolean isIgnoreMember(final Group.Nature groupNature) {
        final Boolean b = ignoreMemberByType.get(groupNature);
        return b != null && b;
    }

    private Collection<? extends Element> resolveMembers(Object argument) {
        if (argument == null) {
            throw new IllegalArgumentException("Can not resolve related members: null argument");
        }

        final PathToMember pathToMember = getPathToMember();
        final RelatedEntity relatedEntity = getRelatedEntity();
        if (pathToMember == null) {
            throw new IllegalStateException("The " + this + " has no @PathToMember annotation");
        }

        // Coerce from String to Long
        if (argument instanceof String) {
            argument = CoercionHelper.coerce(Long.class, argument);
        } else if (argument instanceof String[]) {
            argument = CoercionHelper.coerce(Long[].class, argument);
        }

        // Resolve the objects to search for members
        Object[] objects;
        if (argument instanceof Long || argument instanceof Long[]) {
            if (relatedEntity == null) {
                throw new IllegalStateException("The " + this + " has no @RelatedEntity annotation");
            }
            final Class<? extends Entity> entityType = relatedEntity.value();
            if (argument instanceof Long) {
                objects = new Object[] { EntityHelper.reference(entityType, (Long) argument) };
            } else {
                final Long[] ids = (Long[]) argument;
                objects = new Object[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    final Long id = ids[i];
                    objects[i] = EntityHelper.reference(entityType, id);
                }
            }
        } else {
            objects = CoercionHelper.coerce(Object[].class, argument);
        }

        // Get the paths to members
        final String[] paths = pathToMember.value();
        for (int i = 0; i < paths.length; i++) {
            paths[i] = StringUtils.trimToNull(paths[i]);
        }
        final List<Element> members = new ArrayList<Element>();
        for (final Object object : objects) {
            for (final String path : paths) {
                try {
                    final Element member = fetchMember(object, path);
                    // if (member == null) {
                    // throw new IllegalStateException("The '" + path + "' property on argument '" + object + "' for method " + method + " was null");
                    // }
                    // members.add(member);
                    if (member != null) {
                        members.add(member);
                    }
                } catch (final ClassCastException e) {
                    throw new IllegalStateException("The '" + path + "' property on argument '" + object + "' for " + this + " was not a member, but a '" + e.getMessage() + "'");
                }
            }
        }
        return members;
    }

    /**
     * Verify permission to the related member for the given arguments
     */
    private boolean verifyRelatedMember(final Group.Nature nature, final Permission permission, final Collection<? extends Element> members, final Object arg, final IPermissionRequestor permissionRequestor) {
        if (permission != null) {
            final Type type = Module.Type.getByModuleName(permission.module());
            // Check if we must verify the member also
            if (type.isRelatedToMember() && !isIgnoreMember(nature)) {
                return checkRelated(permission, members);
            }
        } else if (!isIgnoreMember(nature) && (arg != null || permissionRequestor != null)) {
            if (LoggedUser.isAdministrator() && !checkForAdmin(members)) {
                return false;
            } else if (LoggedUser.isBroker() && !checkForBroker(members)) {
                if (!checkForMember(members)) {
                    return false;
                }
            } else if (LoggedUser.isOperator() && !checkForOperator(members)) {
                return false;
            } else if (LoggedUser.isMember() && !checkForMember(members)) {
                return false;
            }
        }
        return true;
    }
}