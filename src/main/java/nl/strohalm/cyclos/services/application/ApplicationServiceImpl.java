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
package nl.strohalm.cyclos.services.application;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.strohalm.cyclos.dao.ApplicationDAO;
import nl.strohalm.cyclos.entities.Application;
import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Indexable;
import nl.strohalm.cyclos.entities.Application.PasswordHash;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.InvoiceQuery;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.alerts.Alert;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.messages.MessageBox;
import nl.strohalm.cyclos.entities.members.messages.MessageQuery;
import nl.strohalm.cyclos.entities.members.records.MemberRecord;
import nl.strohalm.cyclos.scheduling.SchedulingHandler;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.accountfees.AccountFeeService;
import nl.strohalm.cyclos.services.ads.AdService;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.services.alerts.ErrorLogService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.MemberRecordService;
import nl.strohalm.cyclos.services.elements.MessageService;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.lucene.IndexHandler;
import nl.strohalm.cyclos.utils.query.PageHelper;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * Implementation class for the application service interface.
 * @author rafael
 */
public class ApplicationServiceImpl implements ApplicationService, InitializingBean {
    private AccountFeeService   accountFeeService;
    private AccessService       accessService;
    private AlertService        alertService;
    private MessageService      messageService;
    private InvoiceService      invoiceService;
    private ErrorLogService     errorLogService;
    private ElementService      elementService;
    private MemberRecordService memberRecordService;
    private AdService           adService;
    private ApplicationDAO      applicationDao;
    private boolean             initialized;
    private SchedulingHandler   schedulingHandler;
    private Application         application;
    private long                startupTime;
    private IndexHandler        indexHandler;
    private boolean             online = true;

    public void afterPropertiesSet() throws Exception {
        application = applicationDao.read();
    }

    public Calendar getAccountStatusEnabledSince() {
        return application.getAccountStatusEnabledSince();
    }

    public ApplicationStatusVO getApplicationStatus() {
        final ApplicationStatusVO vo = new ApplicationStatusVO();

        // Uptime period
        final long diff = System.currentTimeMillis() - startupTime;
        final int days = (int) (diff / DateUtils.MILLIS_PER_DAY);
        final int hours = (int) ((diff % DateUtils.MILLIS_PER_DAY) / DateUtils.MILLIS_PER_HOUR);
        vo.setUptimeDays(days);
        vo.setUptimeHours(hours);

        // Logged users
        vo.setConnectedAdmins(accessService.listConnectedUsers(EnumSet.of(Group.Nature.ADMIN)).size());
        vo.setConnectedMembers(accessService.listConnectedUsers(EnumSet.of(Group.Nature.MEMBER)).size());
        vo.setConnectedBrokers(accessService.listConnectedUsers(EnumSet.of(Group.Nature.BROKER)).size());
        vo.setConnectedOperators(accessService.listConnectedUsers(EnumSet.of(Group.Nature.OPERATOR)).size());

        // Cyclos version
        vo.setCyclosVersion(getCyclosVersion());

        // Number of alerts
        vo.setMemberAlerts(alertService.getAlertCount(Alert.Type.MEMBER));
        vo.setSystemAlerts(alertService.getAlertCount(Alert.Type.SYSTEM));
        vo.setErrors(errorLogService.getCount());

        // Unread messages
        vo.setUnreadMessages(countUnreadMessages());

        // Open invoices
        vo.setOpenInvoices(countOpenInvoices());

        return vo;
    }

    public String getCyclosVersion() {
        return application.getVersion();
    }

    public Map<Class<? extends Indexable>, IndexStatus> getFullTextIndexesStatus() {
        final Map<Class<? extends Indexable>, IndexStatus> stats = new LinkedHashMap<Class<? extends Indexable>, IndexStatus>();
        stats.put(Member.class, elementService.getIndexStatus(Member.class));
        stats.put(Administrator.class, elementService.getIndexStatus(Administrator.class));
        stats.put(Operator.class, elementService.getIndexStatus(Operator.class));
        stats.put(Ad.class, adService.getIndexStatus());
        stats.put(MemberRecord.class, memberRecordService.getIndexStatus());
        return stats;
    }

    public PasswordHash getPasswordHash() {
        return application.getPasswordHash();
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Store the startup time
        startupTime = System.currentTimeMillis();

        // Set AWT to headless mode. This way we can use XFree86 libs
        // on *nix systems without an X server running.
        setHeadlessMode();

        // Resolve any possible failures on account fees
        accountFeeService.resolveFailures();

        // Start the scheduling handler
        schedulingHandler.initialize();

        // Create a system alert
        alertService.create(SystemAlert.Alerts.APPLICATION_RESTARTED);

        if (indexHandler.indexesExists()) {
            // Indexes exists: just add unindexed entities
            elementService.addMissingEntitiesToIndex();
            adService.addMissingEntitiesToIndex();
            memberRecordService.addMissingEntitiesToIndex();
        } else {
            // Indexes does not exists: rebuild all indexes
            rebuildIndexes();
        }
    }

    public boolean isOnline() {
        return online;
    }

    public void optimizeIndexes(final Class<? extends Indexable> entityType) {
        if (entityType == null) {
            elementService.optimizeIndex(Member.class);
            elementService.optimizeIndex(Administrator.class);
            elementService.optimizeIndex(Operator.class);
            adService.optimizeIndex();
            memberRecordService.optimizeIndex();
        } else if (entityType == Member.class) {
            elementService.optimizeIndex(Member.class);
        } else if (entityType == Administrator.class) {
            elementService.optimizeIndex(Administrator.class);
        } else if (entityType == Operator.class) {
            elementService.optimizeIndex(Operator.class);
        } else if (entityType == Ad.class) {
            adService.optimizeIndex();
        } else if (entityType == MemberRecord.class) {
            memberRecordService.optimizeIndex();
        }
        indexHandler.notifyOptimized(entityType);
    }

    public void rebuildIndexes(final Class<? extends Indexable> entityType) {
        indexHandler.notifyRebuildStart(entityType);
        if (entityType == null) {
            elementService.rebuildIndex(Member.class);
            elementService.rebuildIndex(Administrator.class);
            elementService.rebuildIndex(Operator.class);
            adService.rebuildIndex();
            memberRecordService.rebuildIndex();
        } else if (entityType == Member.class) {
            elementService.rebuildIndex(Member.class);
        } else if (entityType == Administrator.class) {
            elementService.rebuildIndex(Administrator.class);
        } else if (entityType == Operator.class) {
            elementService.rebuildIndex(Operator.class);
        } else if (entityType == Ad.class) {
            adService.rebuildIndex();
        } else if (entityType == MemberRecord.class) {
            memberRecordService.rebuildIndex();
        }
        indexHandler.notifyRebuildEnd(entityType);
    }

    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    public void setAccountFeeService(final AccountFeeService accountFeeService) {
        this.accountFeeService = accountFeeService;
    }

    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    public void setAlertService(final AlertService alertService) {
        this.alertService = alertService;
    }

    public void setApplicationDao(final ApplicationDAO applicationDao) {
        this.applicationDao = applicationDao;
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setErrorLogService(final ErrorLogService errorLogService) {
        this.errorLogService = errorLogService;
    }

    public void setIndexHandler(final IndexHandler indexHandler) {
        this.indexHandler = indexHandler;
    }

    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public void setMemberRecordService(final MemberRecordService memberRecordService) {
        this.memberRecordService = memberRecordService;
    }

    public void setMessageService(final MessageService messageService) {
        this.messageService = messageService;
    }

    public void setOnline(final boolean online) {
        final boolean changed = this.online != online;
        if (changed) {
            this.online = online;

            if (!online) {
                // Disconnect all logged users but the current user
                accessService.disconnectAllButLogged();
            }
        }
    }

    public void setSchedulingHandler(final SchedulingHandler schedulingHandler) {
        this.schedulingHandler = schedulingHandler;
    }

    public void shutdown() {
        alertService.create(SystemAlert.Alerts.APPLICATION_SHUTDOWN);
    }

    private int countOpenInvoices() {
        final InvoiceQuery query = new InvoiceQuery();
        query.setOwner(SystemAccountOwner.instance());
        query.setDirection(InvoiceQuery.Direction.INCOMING);
        query.setStatus(Invoice.Status.OPEN);
        query.setPageForCount();
        return PageHelper.getTotalCount(invoiceService.search(query));
    }

    private int countUnreadMessages() {
        final MessageQuery query = new MessageQuery();
        query.setGetter(LoggedUser.element());
        query.setMessageBox(MessageBox.INBOX);
        query.setRead(false);
        query.setPageForCount();
        return PageHelper.getTotalCount(messageService.search(query));
    }

    private void rebuildIndexes() {
        rebuildIndexes(null);
    }

    private void setHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }
}
