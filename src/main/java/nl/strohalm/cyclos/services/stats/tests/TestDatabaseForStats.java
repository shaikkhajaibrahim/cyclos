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
package nl.strohalm.cyclos.services.stats.tests;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.LoginHistoryLog;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.ads.AdCategory;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.remarks.GroupRemark;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transactions.TransferDTO;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.spring.CustomApplicationContext;
import nl.strohalm.cyclos.utils.conversion.CalendarConverter;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.time.StopWatch;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Multi-purpose test class Set the testing constants via the implemented interface
 * @author luis
 */
public class TestDatabaseForStats implements TestStatsConstants {
    /**
     * this inner class is for keeping the group changes. As cyclos has apparantly no structure for changing the groupHistoryLogs table in mysql via
     * hibernate, this is done directly via jdbc after the session is closed. Therefore, group changes are stored in this structure
     * 
     * @author rinke
     * 
     */
    private static class GroupChange {
        private final Long   memberId;
        private final Long   newGroupId;
        private final Long   oldGroupId;
        private final String dateString;

        // constructor for a group change of the member
        GroupChange(final Long memberId, final Long oldGroupId, final Long nwGroupId, final String dateString) {
            this.memberId = memberId;
            newGroupId = nwGroupId;
            this.dateString = dateString;
            this.oldGroupId = oldGroupId;
        }

        // constructor for creation of the member
        GroupChange(final Long memberId, final Long groupId, final String dateString) {
            this(memberId, null, groupId, dateString);
        }

        /**
         * gets the sql strings needed for the change of the group_history_logs table
         * @return an array with max 2 elements, containing the sql string to perform the needed changes. if only one statement is needed, the second
         * element will be null. When using this, the second element should always be checked on not being null before trying to execute the
         * statement.
         */
        String[] getSqls() {
            final String[] result = new String[2];
            final StringBuilder sql = new StringBuilder("update cyclos3.group_history_logs set start_date=\"");
            sql.append(dateString + " 09:00:00\" where element_id=" + memberId + " and group_id=" + newGroupId);
            sql.append(" and start_date >= curdate()");
            sql.append(" order by id limit 1;");
            result[0] = sql.toString();
            // another row must be updated in case of a group change: the end date for the old group
            if (oldGroupId != null) {
                final StringBuilder sql2 = new StringBuilder();
                sql2.append("update cyclos3.group_history_logs set end_date=\"" + dateString + " 09:00:00\" ");
                sql2.append("where element_id=" + memberId + " and group_id=" + oldGroupId);
                sql2.append(" and end_date >= curdate()");
                sql2.append(" order by id limit 1;");
                result[1] = sql2.toString();
            }
            return result;
        }
    }

    static final String[]         FILES            = {"/nl/strohalm/cyclos/spring/persistence.xml", "/nl/strohalm/cyclos/spring/dao.xml", "/nl/strohalm/cyclos/spring/service.xml", "/nl/strohalm/cyclos/spring/scheduling.xml", "/nl/strohalm/cyclos/spring/misc.xml", "/nl/strohalm/cyclos/spring/aop.xml"};

    static ApplicationContext     APPLICATION_CONTEXT;
    static List<AdCategory>       root;

    // arraylist to keep group changes
    static ArrayList<GroupChange> groupChanges     = new ArrayList<GroupChange>();

    static MemberGroup            fullMembersGroup = null;

    static MemberGroup            fullBrokersGroup = null;

    public static void main(final String[] args) throws Exception {

        final StopWatch sw = new StopWatch();
        sw.start();
        APPLICATION_CONTEXT = new CustomApplicationContext(FILES, TestDatabaseForStats.class);
        System.out.printf("Total startup time: %.3f\n", sw.getTime() / 1000D);

        final TransactionTemplate template = bean("transactionTemplate");

        template.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                try {
                    TestDatabaseForStats.doInTransaction();
                } catch (final ValidationException e) {
                    System.out.println("Validation failed.");
                    final Collection<ValidationError> generalErrors = e.getGeneralErrors();
                    if (generalErrors != null && !generalErrors.isEmpty()) {
                        System.out.println("General errors:");
                        for (final ValidationError error : generalErrors) {
                            System.out.println(error.getKey());
                        }
                    }
                    final Map<String, Collection<ValidationError>> errorsByProperty = e.getErrorsByProperty();
                    if (errorsByProperty != null && !errorsByProperty.isEmpty()) {
                        for (final Map.Entry<String, Collection<ValidationError>> entry : errorsByProperty.entrySet()) {
                            final String name = entry.getKey();
                            final Collection<ValidationError> errors = entry.getValue();
                            if (errors != null && !errors.isEmpty()) {
                                System.out.println("Property errors for '" + name + "':");
                                for (final ValidationError error : errors) {
                                    System.out.println(error.getKey());
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    status.setRollbackOnly();
                    e.printStackTrace();
                }
            }
        });

        updateGroupChangeLog(groupChanges);

        System.out.println("FINISHED");
        System.exit(0);
    }

    private static void doInTransaction() throws Exception {
        final CalendarConverter calendarConverter = new CalendarConverter("yyyy/MM/dd");
        final AccessService accessService = bean("accessService");
        // final AccountService accountService = bean("accountService");
        // final AccountTypeService accountTypeService = bean("accountTypeService");
        final ElementService elementService = bean("elementService");
        final GroupService groupService = bean("groupService");
        final CustomFieldService customFieldService = bean("customFieldService");
        final PaymentService paymentService = bean("paymentService");
        final TransferTypeService transferTypeService = bean("transferTypeService");
        final Session session = getSession();
        final TransferType tradeTransferType = transferTypeService.load(13L);

        class TestMember {
            private Member member;

            TestMember(final MemberGroup memberGroup, final String name, final String creationDate) {
                member = new Member();
                member.setGroup(memberGroup);
                member.setName(name);
                member.setEmail(name + "@members.com");
                member.setUser(new MemberUser());
                member.getUser().setUsername(name);
                member = (Member) elementService.registerMemberByAdmin(member, false); // creation date = current date
                member.setCreationDate(calendarConverter.valueOf(creationDate)); // set new creation date
                groupChanges.add(new GroupChange(member.getId(), member.getGroup().getId(), creationDate));
            }

            void earnFrom(final TestMember payingMember, final BigDecimal amount, final String date) {
                final TransferDTO transferDto = new TransferDTO();
                transferDto.setTransferType(tradeTransferType);
                transferDto.setContext(TransactionContext.PAYMENT);
                transferDto.setFromOwner(payingMember.member);
                transferDto.setToOwner(member);
                transferDto.setDate(calendarConverter.valueOf(date));
                transferDto.setAmount(amount);
                transferDto.setDescription("from " + payingMember.member.getName() + " to " + member.getName() + "; " + amount.toString() + "; " + date);
                paymentService.insertWithoutNotification(transferDto);
            }

            void groupChange(final long newGroupId, final String date) {
                if (newGroupId > 0) {
                    final MemberGroup newGroup = (MemberGroup) groupService.load(newGroupId);
                    groupChanges.add(new GroupChange(member.getId(), member.getGroup().getId(), newGroupId, date));
                    elementService.changeMemberGroup(member, newGroup, "to group #" + newGroupId);
                    // final GroupRemark gr = (GroupRemark) session.createCriteria(GroupRemark.class).add(Expression.eq("subject",
                    // member)).uniqueResult();
                    final Calendar justNow = new GregorianCalendar();
                    justNow.add(Calendar.MINUTE, -5);
                    final GroupRemark gr = (GroupRemark) session.createCriteria(GroupRemark.class).add(Restrictions.eq("subject", member)).add(Restrictions.ge("date", justNow)).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
                    gr.setDate(calendarConverter.valueOf(date));
                    session.flush();
                }
            }

            void login(final String[] loginList) {
                if (loginList != null) {
                    for (final String login : loginList) {
                        final LoginHistoryLog log = new LoginHistoryLog();
                        log.setUser(member.getUser());
                        log.setRemoteAddress("127.0.0.1");
                        log.setDate(calendarConverter.valueOf(login));
                        session.save(log);
                    }
                }
            }
        }

        // Login as administrator to get service permissions
        final User admin = elementService.loadUser("admin");
        accessService.login(admin, "1234", Channel.WEB, "localhost", "session1");

        // Make all fields not required
        for (final CustomField field : customFieldService.listByNature(CustomField.Nature.MEMBER)) {
            field.getValidation().setRequired(false);
            customFieldService.save(field);
        }

        // Get group instances
        final MemberGroup fullMembersGroup = getMemberGroupFromId(5L, groupService);
        final MemberGroup fullBrokersGroup = getMemberGroupFromId(10L, groupService);

        // Set the initial credit
        final TransferType initialCredit = (TransferType) session.load(TransferType.class, 23L);
        final MemberGroupAccountSettings memberAccountSettings = fullMembersGroup.getAccountSettings().iterator().next();
        memberAccountSettings.setInitialCredit(new BigDecimal(1000));
        memberAccountSettings.setInitialCreditTransferType(initialCredit);
        groupService.updateAccountSettings(memberAccountSettings, false);
        // Set the initial credit
        final MemberGroupAccountSettings brokerAccountSettings = fullBrokersGroup.getAccountSettings().iterator().next();
        brokerAccountSettings.setInitialCredit(new BigDecimal(1000));
        brokerAccountSettings.setInitialCreditTransferType(initialCredit);
        groupService.updateAccountSettings(brokerAccountSettings, false);
        int transactionCounter = 0;

        // Create Test members
        final TestMember broker = new TestMember(fullBrokersGroup, "broker", brokerCreationDate);
        final TestMember member1 = new TestMember(getMemberGroupFromId(mem1StartGroup, groupService), "member1", member1CreationDate);
        final TestMember member2 = (member2CreationDate != null) ? new TestMember(getMemberGroupFromId(mem2StartGroup, groupService), "member2", member2CreationDate) : null;
        final TestMember member3 = (member3CreationDate != null) ? new TestMember(getMemberGroupFromId(mem3StartGroup, groupService), "member3", member3CreationDate) : null;
        final TestMember member4 = (member4CreationDate != null) ? new TestMember(getMemberGroupFromId(mem4StartGroup, groupService), "member4", member4CreationDate) : null;
        session.flush();

        // apply group changes
        for (int i = 0; i < member1NewGroups.length; i++) {
            member1.groupChange(member1NewGroups[i], member1GroupChanges[i]);
        }
        // build login history
        member1.login(member1LoginList);
        // apply transactions
        for (final String element : member1EarnDates) {
            member1.earnFrom(broker, new BigDecimal(AMOUNTS[transactionCounter++]), element);
        }
        // repeat these blocks for other members
        // member 2:
        if (member2 != null) {
            for (int i = 0; i < member2NewGroups.length; i++) {
                member2.groupChange(member2NewGroups[i], member2GroupChanges[i]);
            }
            member2.login(member2LoginList);
            for (final String earnDate : member2EarnDates) {
                member2.earnFrom(broker, new BigDecimal(AMOUNTS[transactionCounter++]), earnDate);
            }
        }
        // member 3:
        if (member3 != null) {
            for (int i = 0; i < member3NewGroups.length; i++) {
                member3.groupChange(member3NewGroups[i], member3GroupChanges[i]);
            }
            member3.login(member3LoginList);
            for (final String earnDate : member3EarnDates) {
                member3.earnFrom(broker, new BigDecimal(AMOUNTS[transactionCounter++]), earnDate);
            }
        }
        // member 4:
        if (member4 != null) {
            for (int i = 0; i < member4NewGroups.length; i++) {
                member4.groupChange(member4NewGroups[i], member4GroupChanges[i]);
            }
            member4.login(member4LoginList);
            for (final String earnDate : member4EarnDates) {
                member4.earnFrom(broker, new BigDecimal(AMOUNTS[transactionCounter++]), earnDate);
            }
        }

    }

    private static MemberGroup getMemberGroupFromId(final long id, final GroupService groupService) {
        if (id == 5L) {
            if (fullMembersGroup == null) {
                fullMembersGroup = (MemberGroup) groupService.load(5L);
            }
            return fullMembersGroup;
        }
        if (id == 10L) {
            if (fullBrokersGroup == null) {
                fullBrokersGroup = (MemberGroup) groupService.load(10L);
            }
            return fullBrokersGroup;
        }
        return (MemberGroup) groupService.load(id);
    }

    @SuppressWarnings("unchecked")
    static <T> T bean(final String name) {
        return (T) APPLICATION_CONTEXT.getBean(name);
    }

    @SuppressWarnings("unchecked")
    static Session getSession() {
        final SessionFactory sf = bean("sessionFactory");
        return (Session) new HibernateTemplate(sf).execute(new HibernateCallback() {
            public Object doInHibernate(final Session session) throws HibernateException, SQLException {
                return session;
            }
        });
    }

    /**
     * finally make all updates for the group_history_logs table via jdbc
     * @param groupChanges an arrayList with GroupChange objects.
     */
    static void updateGroupChangeLog(final ArrayList<GroupChange> groupChanges) {
        System.out.println("Finished Cyclos session, now starting group history log updates.");
        final String url = "jdbc:mysql://localhost/cyclos3";
        final String user = "root";
        final String pass = "root";
        Connection con;
        try {
            con = DriverManager.getConnection(url, user, pass);
            try {
                final Statement stat = con.createStatement();
                // stat.execute("truncate cyclos3.group_history_logs;");
                int i = 0;
                for (final GroupChange groupChange : groupChanges) {
                    final String[] sqls = groupChange.getSqls();
                    stat.execute(sqls[0]);
                    System.out.println("GroupHistory update " + i++ + ".0:");
                    System.out.println(sqls[0]);
                    System.out.println();
                    if (sqls[1] != null) {
                        stat.execute(sqls[1]);
                        System.out.println("GroupHistory update " + (i - 1) + ".1:");
                        System.out.println(sqls[1]);
                        System.out.println();
                    }
                }
            } finally {
                con.close();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

}