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
package nl.strohalm.cyclos.services.transactions;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nl.strohalm.cyclos.dao.accounts.transactions.ScheduledPaymentDAO;
import nl.strohalm.cyclos.dao.accounts.transactions.TransferDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPaymentQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment.Status;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.exceptions.MaxAmountPerDayExceededException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.UpperCreditLimitReachedException;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation for scheduled payment service
 * @author Jefferson Magno
 */
public class ScheduledPaymentServiceImpl implements ScheduledPaymentService {

    private SettingsService      settingsService;
    private AccountStatusHandler accountStatusHandler;
    private FetchService         fetchService;
    private ScheduledPaymentDAO  scheduledPaymentDao;
    private TransferDAO          transferDao;
    private PaymentService       paymentService;
    private PermissionService    permissionService;

    public ScheduledPayment block(ScheduledPayment scheduledPayment) {
        scheduledPayment = fetchService.fetch(scheduledPayment, ScheduledPayment.Relationships.TRANSFERS);
        final AccountOwner owner = LoggedUser.accountOwner();
        // Ensure that if the logged user is the from member, the transfer type allows blocking
        if (owner instanceof Member && owner.equals(scheduledPayment.getFromOwner())) {
            if (!scheduledPayment.getType().isAllowBlockScheduledPayments()) {
                throw new PermissionDeniedException();
            }
        }
        final Status status = scheduledPayment.getStatus();
        if (status == Status.PROCESSED || status == Status.BLOCKED || status == Status.CANCELED || status == Status.DENIED) {
            throw new UnexpectedEntityException();
        }
        for (final Transfer transfer : scheduledPayment.getTransfers()) {
            final Status transferStatus = transfer.getStatus();
            if (transferStatus == Status.SCHEDULED || transferStatus == Status.FAILED) {
                transferDao.updateStatus(transfer.getId(), Status.BLOCKED);
            }
        }
        return updateScheduledPaymentStatus(scheduledPayment);
    }

    public ScheduledPayment cancel(ScheduledPayment scheduledPayment) {
        scheduledPayment = fetchService.fetch(scheduledPayment, ScheduledPayment.Relationships.TRANSFERS);
        final AccountOwner owner = LoggedUser.accountOwner();
        // Ensure that if the logged user is the from member, the transfer type allows canceling
        if (owner instanceof Member && owner.equals(scheduledPayment.getFromOwner())) {
            if (!scheduledPayment.getType().isAllowCancelScheduledPayments()) {
                throw new PermissionDeniedException();
            }
        }
        final Status status = scheduledPayment.getStatus();
        if (status == Status.PROCESSED || status == Status.CANCELED || status == Status.DENIED) {
            throw new UnexpectedEntityException();
        }
        for (final Transfer transfer : scheduledPayment.getTransfers()) {
            final Status transferStatus = transfer.getStatus();
            if (transferStatus == Status.SCHEDULED || transferStatus == Status.PENDING || transferStatus == Status.BLOCKED) {
                // Ensure the amount is liberated
                accountStatusHandler.liberateReservedAmountForInstallment(transfer);
            }
            if (transferStatus == Status.SCHEDULED || transferStatus == Status.PENDING || transferStatus == Status.FAILED || transferStatus == Status.BLOCKED) {
                transferDao.updateStatus(transfer.getId(), Status.CANCELED);
            }
        }
        scheduledPayment.setStatus(Status.CANCELED);
        return scheduledPaymentDao.update(scheduledPayment);
    }

    public boolean hasUnrelatedPendingPayments(final Member member, final Collection<MemberAccountType> accountTypes) {
        return scheduledPaymentDao.hasUnrelatedPendingPayments(member, accountTypes);
    }

    public ScheduledPayment load(final Long id, final Relationship... fetch) {
        ScheduledPayment scheduledPayment = scheduledPaymentDao.load(id, fetch);
        if (LoggedUser.isAdministrator()) {
            if (scheduledPayment.getFromOwner() instanceof SystemAccountOwner) {
                if (!permissionService.checkPermission("systemAccounts", "scheduledInformation")) {
                    throw new PermissionDeniedException();
                }
            } else {
                AdminGroup adminGroup = LoggedUser.group();
                adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
                final Relationship fromRel = RelationshipHelper.nested(ScheduledPayment.Relationships.FROM, MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP);
                final Relationship toRel = RelationshipHelper.nested(ScheduledPayment.Relationships.TO, MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP);
                scheduledPayment = fetchService.fetch(scheduledPayment, fromRel, toRel);
                final MemberGroup fromGroup = ((Member) scheduledPayment.getFromOwner()).getMemberGroup();
                final MemberGroup toGroup = ((Member) scheduledPayment.getToOwner()).getMemberGroup();
                if (!adminGroup.getManagesGroups().contains(fromGroup) && !adminGroup.getManagesGroups().contains(toGroup)) {
                    throw new PermissionDeniedException();
                } else if (!permissionService.checkPermission("adminMemberAccounts", "scheduledInformation")) {
                    throw new PermissionDeniedException();
                }
            }
        } else if (LoggedUser.isBroker()) {
            if (scheduledPayment.getFromOwner() instanceof SystemAccountOwner) {
                throw new PermissionDeniedException();
            } else {
                final Member from = (Member) scheduledPayment.getFromOwner();
                final Member to = (Member) scheduledPayment.getToOwner();
                if (!LoggedUser.element().equals(from.getBroker()) && !LoggedUser.element().equals(to.getBroker())) {
                    throw new PermissionDeniedException();
                }
            }
        } else if (LoggedUser.isOperator() || LoggedUser.isMember()) {
            if (scheduledPayment.getFromOwner() instanceof SystemAccountOwner) {
                throw new PermissionDeniedException();
            } else {
                final Member from = (Member) scheduledPayment.getFromOwner();
                final Member to = (Member) scheduledPayment.getToOwner();
                final Member member = (Member) LoggedUser.element().getAccountOwner();
                if (!member.equals(from) && !member.equals(to)) {
                    throw new PermissionDeniedException();
                }
            }
        } else { // invalid user
            throw new PermissionDeniedException();
        }
        return scheduledPayment;
    }

    public Transfer processTransferAsMemberToMember(final Transfer transfer) {
        return doProcessTransfer(transfer);
    }

    public Transfer processTransferAsMemberToSelf(final Transfer transfer) {
        return doProcessTransfer(transfer);
    }

    public Transfer processTransferAsMemberToSystem(final Transfer transfer) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        return doProcessTransfer(transfer);
    }

    public Transfer processTransferFromMemberToMember(final Transfer transfer) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        return doProcessTransfer(transfer);
    }

    public Transfer processTransferFromMemberToSystem(final Transfer transfer) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        return doProcessTransfer(transfer);
    }

    public Transfer processTransferFromSystemToMember(final Transfer transfer) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        return doProcessTransfer(transfer);
    }

    public int recoverScheduledPayments() {

        // Find whether we are executing less than one hour before the scheduled tasks run.
        // If yes, consider today as deadline. Otherwise, today the scheduled task will yet run,
        // so, consider yesterday as deadline.
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final Calendar now = Calendar.getInstance();
        final Calendar limit = DateHelper.truncate(now);
        limit.set(Calendar.HOUR_OF_DAY, localSettings.getSchedulingHour());
        limit.add(Calendar.HOUR_OF_DAY, -1);
        limit.set(Calendar.MINUTE, localSettings.getSchedulingMinute());
        Calendar time;
        if (now.before(limit)) {
            time = DateHelper.truncatePreviosDay(now);
        } else {
            time = DateHelper.truncate(now);
        }

        // Process each transfer
        final TransferQuery query = new TransferQuery();
        query.setResultType(ResultType.ITERATOR);
        query.setPeriod(Period.endingAt(time));
        query.setStatus(Payment.Status.SCHEDULED);
        query.setUnordered(true);
        final List<Transfer> transfers = transferDao.search(query);
        int count = 0;
        for (final Transfer transfer : transfers) {
            paymentService.processScheduled(transfer, time);
            count++;
        }
        return count;
    }

    public List<ScheduledPayment> search(final ScheduledPaymentQuery query) {
        if (query.getOwner() instanceof SystemAccountOwner) {
            throw new PermissionDeniedException();
        }
        return scheduledPaymentDao.search(query);
    }

    public List<ScheduledPayment> searchSystem(final ScheduledPaymentQuery query) {
        if (!(query.getOwner() instanceof SystemAccountOwner)) {
            throw new PermissionDeniedException();
        }

        return scheduledPaymentDao.search(query);
    }

    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setScheduledPaymentDao(final ScheduledPaymentDAO scheduledPaymentDao) {
        this.scheduledPaymentDao = scheduledPaymentDao;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransferDao(final TransferDAO transferDao) {
        this.transferDao = transferDao;
    }

    public ScheduledPayment unblock(ScheduledPayment scheduledPayment) {
        scheduledPayment = fetchService.fetch(scheduledPayment, ScheduledPayment.Relationships.TRANSFERS);
        final Status status = scheduledPayment.getStatus();
        if (status != Status.BLOCKED) {
            throw new UnexpectedEntityException();
        }
        final Calendar today = DateHelper.truncate(Calendar.getInstance());
        for (final Transfer transfer : scheduledPayment.getTransfers()) {
            final Status transferStatus = transfer.getStatus();
            if (transferStatus == Status.BLOCKED) {
                if (transfer.getDate().before(today)) {
                    throw new UnexpectedEntityException();
                } else {
                    transferDao.updateStatus(transfer.getId(), Status.SCHEDULED);
                }
            }
        }
        return updateScheduledPaymentStatus(scheduledPayment);
    }

    public ScheduledPayment updateScheduledPaymentStatus(final ScheduledPayment scheduledPayment) {
        final Transfer firstOpenTransfer = scheduledPayment.getFirstOpenTransfer();
        Payment.Status newStatus = null;
        if (firstOpenTransfer == null) {
            final List<Transfer> transfers = scheduledPayment.getTransfers();
            if (CollectionUtils.isEmpty(transfers)) {
                newStatus = Payment.Status.PROCESSED;
            } else {
                newStatus = transfers.get(transfers.size() - 1).getStatus();
            }
        } else {
            newStatus = firstOpenTransfer.getStatus();
        }
        scheduledPayment.setStatus(newStatus);
        if (newStatus == Payment.Status.PROCESSED) {
            scheduledPayment.setProcessDate(Calendar.getInstance());
        }
        return scheduledPaymentDao.update(scheduledPayment);
    }

    private Transfer doProcessTransfer(Transfer transfer) {
        transfer = fetchService.fetch(transfer, RelationshipHelper.nested(Transfer.Relationships.SCHEDULED_PAYMENT, ScheduledPayment.Relationships.TRANSFERS));

        final ScheduledPayment scheduledPayment = transfer.getScheduledPayment();
        if (scheduledPayment == null) {
            throw new UnexpectedEntityException();
        }
        final Status scheduledPaymentStatus = scheduledPayment.getStatus();
        if (scheduledPaymentStatus == Status.PROCESSED || scheduledPaymentStatus == Status.PENDING || scheduledPaymentStatus == Status.CANCELED || scheduledPaymentStatus == Status.DENIED) {
            throw new UnexpectedEntityException();
        }

        final Calendar today = DateHelper.truncate(Calendar.getInstance());
        final Transfer firstOpenTransfer = scheduledPayment.getFirstOpenTransfer();
        if (firstOpenTransfer.getDate().before(today) && !firstOpenTransfer.equals(transfer)) {
            throw new UnexpectedEntityException();
        }

        final Status firstOpenTransferStatus = firstOpenTransfer.getStatus();
        if (firstOpenTransferStatus == Status.PROCESSED || firstOpenTransferStatus == Status.CANCELED || firstOpenTransferStatus == Status.DENIED || firstOpenTransferStatus == Status.PENDING) {
            throw new UnexpectedEntityException();
        }

        if (LoggedUser.isValid()) {
            Collection<TransferType> allowedTransferTypes;
            if (LoggedUser.accountOwner().equals(transfer.getFromOwner())) {
                final Group group = fetchService.fetch(LoggedUser.group(), Group.Relationships.TRANSFER_TYPES);
                allowedTransferTypes = group.getTransferTypes();
            } else if (LoggedUser.isAdministrator()) {
                final AdminGroup group = fetchService.fetch((AdminGroup) LoggedUser.group(), AdminGroup.Relationships.TRANSFER_TYPES_AS_MEMBER);
                allowedTransferTypes = group.getTransferTypesAsMember();
            } else if (LoggedUser.isBroker() && LoggedUser.element().equals(transfer.getFromOwner())) {
                final BrokerGroup group = fetchService.fetch((BrokerGroup) LoggedUser.group(), BrokerGroup.Relationships.TRANSFER_TYPES_AS_MEMBER);
                allowedTransferTypes = group.getTransferTypesAsMember();
            } else {
                allowedTransferTypes = Collections.emptyList();
            }
            if (!allowedTransferTypes.contains(transfer.getType())) {
                throw new PermissionDeniedException();
            }
        }
        return paymentService.pay(transfer, null);
    }

}