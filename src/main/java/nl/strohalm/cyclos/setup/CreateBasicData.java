/*
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Cyclos is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. �See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Cyclos; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA �02111-1307 �USA

 */
package nl.strohalm.cyclos.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import nl.strohalm.cyclos.CyclosConfiguration;
import nl.strohalm.cyclos.entities.Application;
import nl.strohalm.cyclos.entities.Application.PasswordHash;
import nl.strohalm.cyclos.entities.access.AdminUser;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.ChannelPrincipal;
import nl.strohalm.cyclos.entities.access.Module;
import nl.strohalm.cyclos.entities.access.Operation;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.access.Channel.Principal;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings.PasswordPolicy;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings.EmailValidation;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.settings.Setting;
import nl.strohalm.cyclos.entities.settings.MessageSettings.MessageSettingsEnum;
import nl.strohalm.cyclos.utils.HashHandler;
import nl.strohalm.cyclos.utils.conversion.LocaleConverter;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

/**
 * Creates basic data, like application version, permissions, groups, a system administrator and the default settings
 * @author luis
 */
@SuppressWarnings("deprecation")
public class CreateBasicData implements Runnable {

    public static void createSettings(final Session session, final ResourceBundle bundle, final Locale locale, final Properties cyclosProperties) {
        createSetting(session, Setting.Type.LOCAL, "charset", "UTF-8");
        createSetting(session, Setting.Type.LOCAL, "language", LocaleConverter.instance().toString(locale));
        createSetting(session, Setting.Type.LOCAL, "applicationUsername", bundle.getString("settings.local.application-name"));
        createSetting(session, Setting.Type.LOCAL, "defaultExternalPaymentDescription", bundle.getString("settings.local.default-external-payment-description"));
        createSetting(session, Setting.Type.LOCAL, "chargebackDescription", bundle.getString("settings.local.chargeback-description"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "invitationSubject", bundle.getString("settings.mail.invitation.subject"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "invitationMessage", bundle.getString("settings.mail.invitation.message"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "activationSubject", bundle.getString("settings.mail.activation.subject"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "activationMessageWithoutPassword", bundle.getString("settings.mail.activationWithoutPassword.message"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "activationMessageWithPassword", bundle.getString("settings.mail.activationWithPassword.message"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "resetPasswordSubject", bundle.getString("settings.mail.reset-password.subject"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "resetPasswordMessage", bundle.getString("settings.mail.reset-password.message"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "mailValidationSubject", bundle.getString("settings.mail.mail-validation.subject"));
        createSetting(session, Setting.Type.MAIL_TRANSLATION, "mailValidationMessage", bundle.getString("settings.mail.mail-validation.message"));

        for (final MessageSettingsEnum messageSetting : MessageSettingsEnum.values()) {
            if (messageSetting.messageSettingKey() != null) {
                try {
                    final String body = bundle.getString(messageSetting.messageSettingKey());
                    createSetting(session, Setting.Type.MESSAGE, messageSetting.messageSettingName(), body);
                } catch (final Exception e) {
                    // No message in the bundle. Ignore, as Cyclos will use the default value
                }
            }
            if (messageSetting.subjectSettingKey() != null) {
                try {
                    final String subject = bundle.getString(messageSetting.subjectSettingKey());
                    createSetting(session, Setting.Type.MESSAGE, messageSetting.subjectSettingName(), subject);
                } catch (final Exception e) {
                    // No message in the bundle. Ignore, as Cyclos will use the default value
                }
            }
            if (messageSetting.smsSettingKey() != null) {
                try {
                    final String sms = bundle.getString(messageSetting.smsSettingKey());
                    createSetting(session, Setting.Type.MESSAGE, messageSetting.smsSettingName(), sms);
                } catch (final Exception e) {
                    // No message in the bundle. Ignore, as Cyclos will use the default value
                }
            }
        }

        // Define logs dir
        final String defaultLogDir = cyclosProperties.getProperty("cyclos.default.logDir", "%t");
        final String defaultLogPrefix = cyclosProperties.getProperty("cyclos.default.logPrefix", "cyclos_");
        createSetting(session, Setting.Type.LOG, "traceFile", defaultLogDir + "/" + defaultLogPrefix + "trace%g.log");
        createSetting(session, Setting.Type.LOG, "transactionFile", defaultLogDir + "/" + defaultLogPrefix + "transactions%g.log");
        createSetting(session, Setting.Type.LOG, "accountFeeFile", defaultLogDir + "/" + defaultLogPrefix + "account_fees%g.log");
        createSetting(session, Setting.Type.LOG, "scheduledTaskFile", defaultLogDir + "/" + defaultLogPrefix + "scheduled_task%g.log");

    }

    public static List<Channel> getBuiltinChannels(final ResourceBundle bundle) {
        final List<Channel> channels = new ArrayList<Channel>();
        channels.add(buildChannel(bundle, Channel.WEB, Principal.USER, Credentials.DEFAULT));
        channels.add(buildChannel(bundle, Channel.WAP2, Principal.USER, Credentials.DEFAULT));
        channels.add(buildChannel(bundle, Channel.WAP1, Principal.USER, Credentials.DEFAULT));
        channels.add(buildChannel(bundle, Channel.WEBSHOP, Principal.USER, Credentials.DEFAULT));
        channels.add(buildChannel(bundle, Channel.POSWEB, Principal.USER, Credentials.PIN));
        channels.add(buildChannel(bundle, Channel.POS, Principal.USER, Credentials.PIN));
        return channels;
    }

    private static Channel buildChannel(final ResourceBundle resourceBundle, final String internalName, final Principal principal, final Credentials credentials) {
        final Channel channel = new Channel();
        channel.setInternalName(internalName);
        channel.setDisplayName(resourceBundle.getString("channel." + internalName));
        channel.setCredentials(credentials);
        final ChannelPrincipal cp = new ChannelPrincipal();
        cp.setChannel(channel);
        cp.setPrincipal(principal);
        channel.setPrincipals(Collections.singleton(cp));
        return channel;
    }

    private static void createSetting(final Session session, final Setting.Type type, final String name, final String value) {
        final String newValue = StringUtils.trimToEmpty(value);

        Setting setting = (Setting) session.createQuery("from Setting s where s.type=:type and s.name=:name").setParameter("type", type).setParameter("name", name).uniqueResult();

        if (setting == null) {
            setting = new Setting();
            setting.setType(type);
            setting.setName(name);
        } else {
            if (StringUtils.isNotEmpty(setting.getValue())) {
                // Already contains a value
                return;
            }
        }

        if (!newValue.equals(setting.getValue())) {
            setting.setValue(value);
            session.saveOrUpdate(setting);
        }
    }

    private final ResourceBundle bundle;
    private Properties           cyclosProperties;
    private final Session        session;
    private final boolean        setupOnly;
    private final Locale         locale;
    private AdminGroup           systemAdmins;

    public CreateBasicData(final Setup setup, final boolean setupOnly) {
        this.setupOnly = setupOnly;
        session = setup.getSession();
        bundle = setup.getBundle();
        try {
            cyclosProperties = CyclosConfiguration.getCyclosProperties();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        locale = setup.getLocale() == null ? Locale.getDefault() : setup.getLocale();
    }

    /**
     * Create the basic data
     */
    public void run() {
        // Check if the basic data is already there
        if (session.createCriteria(Application.class).uniqueResult() != null) {
            Setup.out.println(bundle.getString("basic-data.error.already"));
            return;
        }

        Setup.out.println(bundle.getString("basic-data.start"));

        createApplication();
        createModules();
        createChannels();
        if (!setupOnly) {
            createGroups();
            createAdministrator();
        }
        createSettings(session, bundle, locale, cyclosProperties);

        session.flush();

        Setup.out.println(bundle.getString("basic-data.end"));
    }

    private void associateDefaultChannels(final MemberGroup group) {
        associateGroupToChannel(Channel.WEB, group);
        associateGroupToChannel(Channel.WAP1, group);
        associateGroupToChannel(Channel.WAP2, group);
    }

    private void associateGroupToChannel(final String channelStr, final MemberGroup memberGroup) {
        // Get the channel
        final Channel channel = (Channel) session.createCriteria(Channel.class).add(Expression.eq("internalName", channelStr)).uniqueResult();

        // Get the "channels" collection
        Collection<Channel> channels = memberGroup.getChannels();

        // If the "channels" collection does not exist, create a new one and set it on the member group
        if (channels == null) {
            channels = new ArrayList<Channel>();
            memberGroup.setChannels(channels);
        }

        // Add the channel to the "channels" collection
        channels.add(channel);

        // Get the "default channels" collection
        Collection<Channel> defaultChannels = memberGroup.getDefaultChannels();

        // If the "default channels" collection does not exist, create a new one and set it on the member group
        if (defaultChannels == null) {
            defaultChannels = new ArrayList<Channel>();
            memberGroup.setDefaultChannels(defaultChannels);
        }

        // Add the channel to the "default channels" collection
        defaultChannels.add(channel);
    }

    private void createAdministrator() {
        final HashHandler hashHandler = new HashHandler();
        final AdminUser user = new AdminUser();
        user.setSalt(hashHandler.newSalt());
        user.setUsername("admin");
        user.setPassword(hashHandler.hash(user.getSalt(), "1234"));
        user.setPasswordDate(Calendar.getInstance());
        final Administrator administrator = new Administrator();
        administrator.setEmail("admin@mail.nl");
        administrator.setName("Administrator");
        administrator.setCreationDate(Calendar.getInstance());
        administrator.setGroup(systemAdmins);
        administrator.setUser(user);
        session.save(administrator);
    }

    private void createApplication() {
        final Version currentVersion = new VersionHistoryReader().read().getCurrent();
        final Application application = new Application();
        application.setVersion(currentVersion.getLabel());
        application.setAccountStatusEnabledSince(Calendar.getInstance());
        application.setPasswordHash(PasswordHash.SHA2_SALT);
        session.save(application);
    }

    private void createChannels() {
        final List<Channel> builtinChannels = getBuiltinChannels(bundle);
        for (final Channel channel : builtinChannels) {
            session.save(channel);
        }
    }

    @SuppressWarnings("unchecked")
    private <G extends Group> G createGroup(final Class<G> groupClass, final Group.Status status, final String keyPart, final Module.Type... moduleTypes) {
        G group = null;
        try {
            group = groupClass.newInstance();
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        group.setStatus(status);
        group.setName(bundle.getString("group." + keyPart + ".name"));
        group.setDescription(bundle.getString("group." + keyPart + ".description"));
        group.getBasicSettings().setPasswordPolicy(PasswordPolicy.NONE);

        session.save(group);
        if (moduleTypes != null && moduleTypes.length > 0) {
            if (group.getPermissions() == null) {
                group.setPermissions(new ArrayList<Operation>());
            }
            // Since there are no documents on the default database, remove the view document permission, or the menu item will appear with no actual
            // documents
            final String hql = "from Operation op where op.module.type = :type and op.module.name not in ('adminMemberDocuments', 'memberDocuments', 'brokerDocuments')";
            for (final Module.Type moduleType : moduleTypes) {
                group.getPermissions().addAll(session.createQuery(hql).setParameter("type", moduleType).list());
            }

            session.flush();
        }
        return group;
    }

    private void createGroups() {
        systemAdmins = createGroup(AdminGroup.class, Group.Status.NORMAL, "system-admins", Module.Type.BASIC, Module.Type.ADMIN_SYSTEM, Module.Type.ADMIN_ADMIN, Module.Type.ADMIN_MEMBER);
        removeUnwantedAdminPermissions(systemAdmins);

        final AdminGroup accountAdmins = createGroup(AdminGroup.class, Group.Status.NORMAL, "account-admins", Module.Type.BASIC, Module.Type.ADMIN_MEMBER);
        removeUnwantedAdminPermissions(accountAdmins);
        removeUnwantedAccountAdminPermissions(accountAdmins);

        // Create disabled admins group and removed admins group
        createGroup(AdminGroup.class, Group.Status.NORMAL, "disabled-admins");
        createGroup(AdminGroup.class, Group.Status.REMOVED, "removed-admins");

        // Create full members group
        final MemberGroup fullMembers = createGroup(MemberGroup.class, Group.Status.NORMAL, "full-members", Module.Type.BASIC, Module.Type.MEMBER);
        fullMembers.getMemberSettings().setEmailValidation(EmailValidation.PUBLIC);
        removeUnwantedMemberPermissions(fullMembers);
        associateDefaultChannels(fullMembers);

        // Create inactive members group
        final MemberGroup inactiveMembers = createGroup(MemberGroup.class, Group.Status.NORMAL, "inactive-members");
        inactiveMembers.setInitialGroup(true);

        // Crate disabled members group and removed members group
        final MemberGroup disabledMembers = createGroup(MemberGroup.class, Group.Status.NORMAL, "disabled-members");
        final MemberGroup removedMembers = createGroup(MemberGroup.class, Group.Status.REMOVED, "removed-members");

        // Create full brokers group
        final BrokerGroup fullBrokers = createGroup(BrokerGroup.class, Group.Status.NORMAL, "full-brokers", Module.Type.BASIC, Module.Type.MEMBER, Module.Type.BROKER);
        removeUnwantedMemberPermissions(fullBrokers);
        associateDefaultChannels(fullBrokers);

        // Create disabled brokers group and removed brokers group
        final BrokerGroup disabledBrokers = createGroup(BrokerGroup.class, Group.Status.NORMAL, "disabled-brokers");
        final BrokerGroup removedBrokers = createGroup(BrokerGroup.class, Group.Status.REMOVED, "removed-brokers");

        final List<MemberGroup> allMemberGroups = Arrays.asList(fullMembers, inactiveMembers, disabledMembers, removedMembers, fullBrokers, disabledBrokers, removedBrokers);

        // Set the default permissions to manage member group
        systemAdmins.setManagesGroups(new ArrayList<MemberGroup>(allMemberGroups));
        accountAdmins.setManagesGroups(new ArrayList<MemberGroup>(allMemberGroups));

        // Allow admins to see each other
        systemAdmins.setViewConnectedAdminsOf(Arrays.asList(systemAdmins, accountAdmins));
        accountAdmins.setViewConnectedAdminsOf(Arrays.asList(systemAdmins, accountAdmins));

        // Set the default permissions to view profile and ads
        fullMembers.setCanViewAdsOfGroups(Arrays.asList(fullMembers, fullBrokers));
        fullMembers.setCanViewProfileOfGroups(Arrays.asList(fullMembers, fullBrokers));
        fullBrokers.setCanViewAdsOfGroups(Arrays.asList(fullMembers, fullBrokers));
        fullBrokers.setCanViewProfileOfGroups(Arrays.asList(fullMembers, fullBrokers));
    }

    private void createModules() {
        final List<Module> modules = Permissions.all();
        for (final Module module : modules) {
            final Collection<Operation> ops = module.getOperations();
            module.setOperations(null);
            session.save(module);
            for (final Operation operation : ops) {
                session.save(operation);
            }
        }
    }

    private void removeUnwantedAccountAdminPermissions(final AdminGroup group) {
        for (final Iterator<Operation> iterator = group.getPermissions().iterator(); iterator.hasNext();) {
            final Operation op = iterator.next();
            final String operation = op.getName();
            final String module = op.getModule().getName();
            if (module.equals("adminMembers") && operation.equals("import")) {
                iterator.remove();
            } else if (operation.contains("adminMemberAds") || operation.equals("import")) {
                iterator.remove();
            }
        }
    }

    private void removeUnwantedAdminPermissions(final AdminGroup group) {
        for (final Iterator<Operation> iterator = group.getPermissions().iterator(); iterator.hasNext();) {
            final Operation op = iterator.next();
            final String operation = op.getName();
            final String module = op.getModule().getName();
            if (module.equals("basic") && operation.equals("inviteMember")) {
                iterator.remove();
            } else if (module.equals("adminMembers") && operation.equals("changeUsername")) {
                iterator.remove();
            } else if (operation.contains("AsMember") || operation.contains("WithDate")) {
                iterator.remove();
            } else if (operation.contains("Operator")) {
                iterator.remove();
            } else if ((module.equals("systemPayments") || module.equals("adminMemberPayments")) && (operation.equals("authorize") || operation.equals("cancel"))) {
                iterator.remove();
            } else if (module.equals("systemReports") && operation.equals("smsLogs")) {
                iterator.remove();
            } else if (module.equals("adminMemberSms") || module.equals("brokerMemberSms") || module.equals("memberSms")) {
                iterator.remove();
            } else if (operation.equals("authorizedInformation") || operation.equals("viewAuthorized")) {
                iterator.remove();
            } else if (module.equals("systemGuaranteeTypes") || module.equals("adminMemberGuarantees")) {
                iterator.remove();
            } else if (module.equals("adminMemberAccounts") && operation.equals("simulateConversion")) {
                iterator.remove();
            } else if (module.equals("adminMemberPos")) {
                iterator.remove();
            }
        }
    }

    private void removeUnwantedMemberPermissions(final MemberGroup memberGroup) {
        for (final Iterator<Operation> iterator = memberGroup.getPermissions().iterator(); iterator.hasNext();) {
            final Operation op = iterator.next();
            final String module = op.getModule().getName();
            final String operation = op.getName();
            if (module.equals("basic") && operation.equals("inviteMember")) {
                iterator.remove();
            } else if (module.equals("memberPayments") && operation.equals("paymentToSelf")) {
                iterator.remove();
            } else if (module.equals("memberPayments") && operation.equals("request")) {
                iterator.remove();
            } else if (module.equals("memberInvoices") && operation.equals("sendToSystem")) {
                iterator.remove();
            } else if (module.equals("memberAccount") && operation.equals("simulateConversion")) {
                iterator.remove();
            } else if (module.equals("brokerAccounts") && operation.equals("brokerSimulateConversion")) {
                iterator.remove();
            } else if (module.equals("memberOperators")) {
                iterator.remove();
            } else if (module.equals("memberCommissions")) {
                iterator.remove();
            } else if (module.equals("memberGuarantees")) {
                iterator.remove();
            } else if (module.equals("brokerMemberSms") || module.equals("memberSms")) {
                iterator.remove();
            } else if (module.equals("brokerMemberPayments") && operation.contains("AsMember")) {
                iterator.remove();
            } else if (module.equals("brokerInvoices") && operation.contains("AsMember")) {
                iterator.remove();
            } else if (module.equals("brokerMembers") && operation.equals("changeUsername")) {
                iterator.remove();
            } else if (module.equals("brokerMembers") && operation.equals("changeName")) {
                iterator.remove();
            } else if (operation.equals("authorizedInformation")) {
                iterator.remove();
            } else if (module.equals("memberPayments") && (operation.equals("authorize") || operation.equals("cancel"))) {
                iterator.remove();
            }
        }
    }
}