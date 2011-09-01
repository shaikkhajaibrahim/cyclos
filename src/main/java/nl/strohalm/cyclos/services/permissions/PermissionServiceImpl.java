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
package nl.strohalm.cyclos.services.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.dao.groups.GroupDAO;
import nl.strohalm.cyclos.dao.permissions.ModuleDAO;
import nl.strohalm.cyclos.dao.permissions.OperationDAO;
import nl.strohalm.cyclos.entities.access.Module;
import nl.strohalm.cyclos.entities.access.Operation;
import nl.strohalm.cyclos.entities.access.Module.Type;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.setup.Permissions;
import nl.strohalm.cyclos.utils.DataIteratorHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.IPermissionRequestor;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.PermissionsDescriptor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;

/**
 * Implementation class for permission services.
 * @author rafael
 */
public class PermissionServiceImpl implements PermissionService {

    private FetchService                                    fetchService;
    private GroupDAO                                        groupDao;
    private ModuleDAO                                       moduleDao;
    private OperationDAO                                    operationDao;
    private SortedMap<String, Module>                       cachedModules;
    private Map<Module.Type, SortedSet<Module>>             cachedModulesByType;
    private Map<Group.Nature, List<Module>>                 cachedModulesByGroupNature;
    private SortedMap<String, SortedMap<String, Operation>> cachedOperations;
    private Collection<Operation>                           allCachedOperations;
    private boolean                                         cacheLoaded       = false;
    private final Map<Group, String[]>                      cachedPermissions = new HashMap<Group, String[]>();

    public boolean checkPermission(Group group, final String module, final String operation) {
        // When an operator is logged in and the module is member, test the operator's member's permissions
        if (group.getNature() == Group.Nature.OPERATOR && Module.Type.getByModuleName(module) == Module.Type.MEMBER) {
            final OperatorGroup operatorGroup = fetchService.fetch((OperatorGroup) group, OperatorGroup.Relationships.MEMBER);
            group = operatorGroup.getMember().getGroup();
        }
        // Get the permissions list from the cache
        String[] permissions = cachedPermissions.get(group);
        if (permissions == null) {
            group = fetchService.reload(group, Group.Relationships.PERMISSIONS, RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP));
            final Set<String> col = new TreeSet<String>();
            for (final Operation op : group.getPermissions()) {
                final String operationName = op.getName();
                if (group instanceof OperatorGroup) {
                    // For operator groups, ensure when there's a member permission with the same name, his member has permission also
                    final String memberModule = op.getModule().getName().replaceAll("operator", "member");
                    try {
                        loadOperation(memberModule, operationName);
                        // If this line is reached, it means there is an equivalent member operation
                        final MemberGroup memberGroup = ((OperatorGroup) group).getMember().getMemberGroup();
                        if (!checkPermission(memberGroup, memberModule, operationName)) {
                            // The member does not have permission. Skip this one
                            continue;
                        }
                    } catch (final EntityNotFoundException e) {
                        // Ok, no equivalent member permission, no further check
                    }

                }
                col.add(fetchService.fetch(op.getModule()).getName() + "#" + operationName);
            }
            permissions = col.toArray(new String[col.size()]);
            cachedPermissions.put(group, permissions);
        }
        final String modulePart = module + "#";
        if (StringUtils.isEmpty(operation)) {
            // Check for module only - any permissions with that module are ok
            for (final String permission : permissions) {
                if (permission.startsWith(modulePart)) {
                    return true;
                }
            }
            return false;
        } else {
            // Check for module / operation
            return Arrays.binarySearch(permissions, modulePart + operation) >= 0;
        }
    }

    public boolean checkPermissions(final IPermissionRequestor permissionRequestor) {
        final PermissionsDescriptor descriptor = new PermissionsDescriptor() {
            { // descriptor initialization
                setFetchService(fetchService);
                setPermissionService(PermissionServiceImpl.this);
                setCheckAllRelatedMembers(false);
                for (final Group.Nature nature : Group.Nature.values()) {
                    final Collection<Permission> permissions = permissionRequestor.requiredPermissions(nature);
                    setPermissionsForNature(nature, permissions == null ? null : permissions.toArray(new Permission[permissions.size()]));
                    setActionForNature(nature, permissionRequestor.isAllowed(nature));
                    setIgnoreMemberForNature(nature, CollectionUtils.isEmpty(permissionRequestor.managedMembers(nature)));
                }
            }
        };

        if (!descriptor.isAnnotated()) {
            return true;
        } else if (descriptor.checkPermission(permissionRequestor).isGranted()) {
            return true;
        }

        return false;
    }

    public boolean checkPermission(final String module, final String operation) {
        return checkPermission(LoggedUser.group(), module, operation);
    }

    @SuppressWarnings("unchecked")
    public Collection<Operation> getCachedOperations(final Collection<Operation> operations) {
        if (operations == null) {
            return Collections.emptyList();
        }
        return CollectionUtils.select(allCachedOperations, new Predicate() {
            public boolean evaluate(final Object object) {
                return operations.contains(object);
            }
        });
    }

    public void importNew() {
        final List<Operation> operations = allOperations();
        final List<Module> allExistingModules = moduleDao.listAll();
        final List<Operation> allExistingOperations = operationDao.listAll();

        // Import new operations and modules
        for (final Operation current : operations) {
            Module module = moduleInCollection(allExistingModules, current.getModule().getName());
            if (module == null) {
                module = moduleDao.insert(current.getModule());
                allExistingModules.add(module);
            }
            final Operation operation = operationInCollection(allExistingOperations, module.getName(), current.getName());
            if (operation == null) {
                current.setModule(module);
                operationDao.insert(current);
            }
        }
    }

    public List<Module> listModules(final Group.Nature nature) {
        readOperations();
        return cachedModulesByGroupNature.get(nature);
    }

    public Module loadModule(final String name) throws EntityNotFoundException {
        readOperations();
        final Module module = cachedModules.get(name);
        if (module == null) {
            throw new EntityNotFoundException(Module.class);
        }
        return module;
    }

    public Operation loadOperation(final String moduleName, final String operationName) throws EntityNotFoundException {
        readOperations();
        Operation operation = null;
        final SortedMap<String, Operation> operations = cachedOperations.get(moduleName);
        if (operations != null) {
            operation = operations.get(operationName);
        }
        if (operation == null) {
            throw new EntityNotFoundException(Operation.class);
        }
        return operation;
    }

    public void refreshCache() {
        cachedPermissions.clear();
    }

    public void refreshCache(Group group) {
        group = fetchService.fetch(group);
        cachedPermissions.remove(group);
        if (group instanceof MemberGroup) {
            // When a member group, refresh the cache for all operator groups of members of that group
            final MemberGroup memberGroup = (MemberGroup) group;
            final List<OperatorGroup> operatorGroups = groupDao.iterateOperatorGroups(memberGroup);
            try {
                for (final OperatorGroup operatorGroup : operatorGroups) {
                    cachedPermissions.remove(operatorGroup);
                }
            } finally {
                DataIteratorHelper.close(operatorGroups);
            }
        }
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupDao(final GroupDAO groupDao) {
        this.groupDao = groupDao;
    }

    public void setModuleDao(final ModuleDAO permissionDao) {
        moduleDao = permissionDao;
    }

    public void setOperationDao(final OperationDAO operationDao) {
        this.operationDao = operationDao;
    }

    private List<Operation> allOperations() {
        final List<Operation> allOperations = new ArrayList<Operation>();
        for (final Module module : Permissions.all()) {
            allOperations.addAll(module.getOperations());
        }
        return allOperations;
    }

    /**
     * Finds a module by name in a collection
     */
    private Module moduleInCollection(final List<Module> collection, final String name) {
        for (final Module module : collection) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Finds an operation by name in a collection
     */
    private Operation operationInCollection(final List<Operation> collection, final String moduleName, final String operationName) {
        for (final Operation operation : collection) {
            if (operation.getName().equals(operationName) && operation.getModule().getName().equals(moduleName)) {
                return operation;
            }
        }
        return null;
    }

    private void readOperations() {
        if (!cacheLoaded) {
            cachedModules = new TreeMap<String, Module>();
            cachedOperations = new TreeMap<String, SortedMap<String, Operation>>();
            allCachedOperations = new ArrayList<Operation>();
            cachedModulesByType = new HashMap<Type, SortedSet<Module>>();
            final List<Module> allModules = (List<Module>) fetchService.fetch(moduleDao.listAll(), Module.Relationships.OPERATIONS);
            for (final Module module : allModules) {
                final Type type = module.getType();
                final String moduleName = module.getName();

                // Add the module on the cached modules collection
                cachedModules.put(moduleName, module);

                // Create the modules by type map
                SortedSet<Module> modules = cachedModulesByType.get(type);
                if (modules == null) {
                    modules = new TreeSet<Module>();
                    cachedModulesByType.put(type, modules);
                }
                modules.add(module);

                // Create the operations cache
                final SortedMap<String, Operation> operations = new TreeMap<String, Operation>();
                for (final Operation operation : module.getOperations()) {
                    // Set the same module instance to avoid fetching problems
                    operation.setModule(module);
                    operations.put(operation.getName(), operation);
                    allCachedOperations.add(operation);
                }
                cachedOperations.put(moduleName, operations);
            }

            // Build the cache by group nature
            List<Module> modules;
            cachedModulesByGroupNature = new TreeMap<Group.Nature, List<Module>>();

            // Admin modules
            modules = new ArrayList<Module>();
            modules.addAll(cachedModulesByType.get(Module.Type.BASIC));
            modules.addAll(cachedModulesByType.get(Module.Type.ADMIN_SYSTEM));
            modules.addAll(cachedModulesByType.get(Module.Type.ADMIN_MEMBER));
            modules.addAll(cachedModulesByType.get(Module.Type.ADMIN_ADMIN));
            cachedModulesByGroupNature.put(Group.Nature.ADMIN, Collections.unmodifiableList(modules));

            // Member modules
            modules = new ArrayList<Module>();
            modules.addAll(cachedModulesByType.get(Module.Type.BASIC));
            modules.addAll(cachedModulesByType.get(Module.Type.MEMBER));
            cachedModulesByGroupNature.put(Group.Nature.MEMBER, Collections.unmodifiableList(modules));

            // Broker modules
            modules = new ArrayList<Module>();
            modules.addAll(cachedModulesByType.get(Module.Type.BASIC));
            modules.addAll(cachedModulesByType.get(Module.Type.MEMBER));
            modules.addAll(cachedModulesByType.get(Module.Type.BROKER));
            cachedModulesByGroupNature.put(Group.Nature.BROKER, Collections.unmodifiableList(modules));

            // Operator modules
            modules = new ArrayList<Module>();
            modules.addAll(cachedModulesByType.get(Module.Type.BASIC));
            modules.addAll(cachedModulesByType.get(Module.Type.OPERATOR));
            cachedModulesByGroupNature.put(Group.Nature.OPERATOR, Collections.unmodifiableList(modules));

            // Store the flag
            cacheLoaded = true;
        }
    }

}