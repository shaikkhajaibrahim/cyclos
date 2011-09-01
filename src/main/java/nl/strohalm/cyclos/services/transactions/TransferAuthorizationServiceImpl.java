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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.dao.accounts.transactions.TransferAuthorizationDAO;
import nl.strohalm.cyclos.dao.accounts.transactions.TransferDAO;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorization;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorizationDTO;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorizationQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransfersAwaitingAuthorizationQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel.Authorizer;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.transactions.exceptions.AlreadyAuthorizedException;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.StringUtils;

public class TransferAuthorizationServiceImpl implements TransferAuthorizationService {

    private FetchService             fetchService;
    private ScheduledPaymentService  scheduledPaymentService;
    private TransferDAO              transferDao;
    private TransferAuthorizationDAO transferAuthorizationDao;
    private AccountStatusHandler     accountStatusHandler;

    public Transfer authorizeAsPayer(final TransferAuthorizationDTO transferAuthorizationDto) {
        initializeTransferAuthorizationDto(transferAuthorizationDto);
        validateAuthorizationLevel(transferAuthorizationDto.getTransfer(), AuthorizationLevel.Authorizer.PAYER);
        return doAuthorize(transferAuthorizationDto);
    }

    public Transfer authorizeAsReceiver(final TransferAuthorizationDTO transferAuthorizationDto) {
        initializeTransferAuthorizationDto(transferAuthorizationDto);
        validateAuthorizationLevel(transferAuthorizationDto.getTransfer(), AuthorizationLevel.Authorizer.RECEIVER);
        return doAuthorize(transferAuthorizationDto);
    }

    public Transfer authorizeFromMember(final TransferAuthorizationDTO transferAuthorizationDto) {
        initializeTransferAuthorizationDto(transferAuthorizationDto);
        validateFromMemberAuthorization(transferAuthorizationDto.getTransfer());
        return doAuthorize(transferAuthorizationDto);
    }

    public Transfer authorizeFromSystem(final TransferAuthorizationDTO transferAuthorizationDto) {
        initializeTransferAuthorizationDto(transferAuthorizationDto);
        validateAdministratorAuthorization(transferAuthorizationDto.getTransfer());
        return doAuthorize(transferAuthorizationDto);
    }

    public Transfer authorizeOnInsert(Transfer transfer) {
        // Only process when a member is performing a payment
        if (LoggedUser.isValid()) {
            transfer = fetchService.fetch(transfer, Transfer.Relationships.PARENT, RelationshipHelper.nested(Transfer.Relationships.NEXT_AUTHORIZATION_LEVEL, AuthorizationLevel.Relationships.ADMIN_GROUPS));
            final Transfer parent = transfer.getParent();
            final AuthorizationLevel authorizationLevel = transfer.getNextAuthorizationLevel();
            final Member fromMember = transfer.isFromSystem() ? null : (Member) transfer.getFromOwner();
            // Only process root transfer from members and that require authorization
            if (parent == null && fromMember != null && authorizationLevel != null) {
                boolean authorize = false;
                switch (authorizationLevel.getAuthorizer()) {
                    case BROKER:
                        // Automatically authorize when logged as the member's broker and the authorization is made by broker, or as administrator
                        authorize = (LoggedUser.isBroker() && LoggedUser.element().equals(fromMember.getBroker())) || (LoggedUser.isAdministrator() && authorizationLevel.getAdminGroups().contains(LoggedUser.group()));
                        break;
                    case ADMIN:
                        // Automatically authorize when logged as administrator
                        authorize = LoggedUser.isAdministrator() && authorizationLevel.getAdminGroups().contains(LoggedUser.group());
                        break;
                }
                // Perform the authorization
                if (authorize) {
                    final TransferAuthorizationDTO dto = new TransferAuthorizationDTO();
                    dto.setTransfer(transfer);
                    transfer = doAuthorize(dto);
                }
            }
        }
        return transfer;
    }

    public Transfer cancelAsMember(final TransferAuthorizationDTO transferAuthorizationDto) {
        initializeTransferAuthorizationDto(transferAuthorizationDto);
        validateCancel(transferAuthorizationDto.getTransfer());
        return doCancel(transferAuthorizationDto);
    }

    public Transfer cancelFromMember(final TransferAuthorizationDTO transferAuthorizationDto) {
        initializeTransferAuthorizationDto(transferAuthorizationDto);
        validateCancel(transferAuthorizationDto.getTransfer());
        return doCancel(transferAuthorizationDto);
    }

    public Transfer cancelFromMemberAsReceiver(final TransferAuthorizationDTO transferAuthorizationDto) {
        return cancelFromMember(transferAuthorizationDto);
    }

    public Transfer cancelFromSystem(final TransferAuthorizationDTO transferAuthorizationDto) {
        initializeTransferAuthorizationDto(transferAuthorizationDto);
        validateCancel(transferAuthorizationDto.getTransfer());
        return doCancel(transferAuthorizationDto);
    }

    public Transfer denyAsReceiver(final TransferAuthorizationDTO transferAuthorizationDto) {
        validateDenyAuthorization(transferAuthorizationDto);
        validateAuthorizationLevel(transferAuthorizationDto.getTransfer(), AuthorizationLevel.Authorizer.RECEIVER);
        return doDeny(transferAuthorizationDto);
    }

    public Transfer denyFromMember(final TransferAuthorizationDTO transferAuthorizationDto) {
        validateDenyAuthorization(transferAuthorizationDto);
        validateFromMemberAuthorization(transferAuthorizationDto.getTransfer());
        return doDeny(transferAuthorizationDto);
    }

    public Transfer denyFromSystem(final TransferAuthorizationDTO transferAuthorizationDto) {
        validateDenyAuthorization(transferAuthorizationDto);
        validateAdministratorAuthorization(transferAuthorizationDto.getTransfer());
        return doDeny(transferAuthorizationDto);
    }

    public List<TransferAuthorization> searchAuthorizations(final TransferAuthorizationQuery query) {
        if (LoggedUser.isValid()) {
            final Element by = fetchService.fetch(query.getBy(), Member.Relationships.BROKER);
            if (LoggedUser.isAdministrator()) {
                // Administrators search by administration when 'by member' not specified
                query.setByAdministration(query.getBy() == null);
            } else if (LoggedUser.isBroker()) {
                // Brokers can search by themselves or by their members
                if (by != null && !(by instanceof Member)) {
                    throw new PermissionDeniedException();
                }
                final Member byMember = (Member) by;
                final Member loggedBroker = LoggedUser.element();
                if (byMember != null && !loggedBroker.equals(byMember.getBroker())) {
                    throw new PermissionDeniedException();
                }
                query.setByAdministration(false);
                query.setBy(byMember == null ? loggedBroker : byMember);
            } else {
                // Members can only search by themselves
                final Member loggedMember = (Member) LoggedUser.accountOwner();
                query.setByAdministration(false);
                query.setBy(loggedMember);
            }
            return transferAuthorizationDao.search(query);
        } else {
            throw new PermissionDeniedException();
        }
    }

    public List<Transfer> searchTransfersAwaitingMyAuthorization(final TransfersAwaitingAuthorizationQuery query) {
        query.setAuthorizer(LoggedUser.element());
        return transferDao.searchTransfersAwaitingAuthorization(query);
    }

    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setScheduledPaymentService(final ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    public void setTransferAuthorizationDao(final TransferAuthorizationDAO transferAuthorizationDao) {
        this.transferAuthorizationDao = transferAuthorizationDao;
    }

    public void setTransferDao(final TransferDAO transferDao) {
        this.transferDao = transferDao;
    }

    /**
     * Create a transfer authorization log
     */
    private TransferAuthorization createAuthorization(final Transfer transfer, final TransferAuthorization.Action action, final AuthorizationLevel level, final String comments, final boolean showToMember) {
        TransferAuthorization transferAuthorization = new TransferAuthorization();
        transferAuthorization.setTransfer(transfer);
        transferAuthorization.setLevel(level);
        transferAuthorization.setBy(LoggedUser.element());
        transferAuthorization.setDate(Calendar.getInstance());
        transferAuthorization.setAction(action);
        transferAuthorization.setComments(comments);
        transferAuthorization.setShowToMember(showToMember);
        transferAuthorization = transferAuthorizationDao.insert(transferAuthorization);

        return transferAuthorization;
    }

    private Transfer doAuthorize(final TransferAuthorizationDTO transferAuthorizationDto) {
        Transfer transfer = fetchService.reload(transferAuthorizationDto.getTransfer(), Transfer.Relationships.SCHEDULED_PAYMENT, Transfer.Relationships.AUTHORIZATIONS);
        validateAuthorization(transfer);

        final String comments = transferAuthorizationDto.getComments();
        final boolean showToMember = transferAuthorizationDto.isShowToMember();

        // Check if the authorizer has already authorized
        final Element logged = LoggedUser.element();
        for (final TransferAuthorization authorization : transfer.getAuthorizations()) {
            if (logged.equals(authorization.getBy())) {
                throw new AlreadyAuthorizedException();
            }
        }

        // Update transfer
        final AuthorizationLevel authorizationLevel = transfer.getNextAuthorizationLevel();
        final AuthorizationLevel nextAuthorizationLevel = getHigherLevel(transfer);

        final boolean processed = nextAuthorizationLevel == null;
        if (processed) {
            transfer = transferDao.updateAuthorizationData(transfer.getId(), Transfer.Status.PROCESSED, null, Calendar.getInstance());
        } else {
            transfer = transferDao.updateAuthorizationData(transfer.getId(), Transfer.Status.PENDING, nextAuthorizationLevel, null);
        }

        // Create the transfer authorization object
        final TransferAuthorization authorization = createAuthorization(transfer, TransferAuthorization.Action.AUTHORIZE, authorizationLevel, comments, showToMember);

        // Update both account status when the payment was processed
        if (processed) {
            // IMPORTANT: first to-account must be processed, then from-account.
            accountStatusHandler.processAuthorization(transfer.getTo(), transfer, authorization);
            accountStatusHandler.processAuthorization(transfer.getFrom(), transfer, authorization);
        }

        transfer.getAuthorizations().add(authorization);

        // Update child transfers
        for (final Transfer childTransfer : transfer.getChildren()) {
            updateChildTransfer(transfer, childTransfer, authorization, processed);
        }

        // If the transfer is part of a scheduled payment, update the scheduled payment status
        if (transfer.getScheduledPayment() != null) {
            scheduledPaymentService.updateScheduledPaymentStatus(transfer.getScheduledPayment());
        }

        return transfer;
    }

    private Transfer doCancel(final TransferAuthorizationDTO transferAuthorizationDto) {
        Transfer transfer = transferAuthorizationDto.getTransfer();
        validateAuthorization(transfer);
        // User can't cancel a transfer that is part of a scheduled payment
        if (transfer.getScheduledPayment() != null) {
            throw new UnexpectedEntityException();
        }

        final String comments = transferAuthorizationDto.getComments();
        final AuthorizationLevel authorizationLevel = transfer.getNextAuthorizationLevel();

        // Update transfer
        transfer = transferDao.updateAuthorizationData(transfer.getId(), Transfer.Status.CANCELED, null, null);

        // Create the transfer authorization object
        final TransferAuthorization authorization = createAuthorization(transfer, TransferAuthorization.Action.CANCEL, authorizationLevel, comments, true);

        // Update the from account status only
        accountStatusHandler.processAuthorization(transfer.getFrom(), transfer, authorization);

        // Update child transfers
        for (final Transfer childTransfer : transfer.getChildren()) {
            updateChildTransfer(transfer, childTransfer, authorization, true);
        }

        return transfer;
    }

    private Transfer doDeny(final TransferAuthorizationDTO transferAuthorizationDto) {
        Transfer transfer = transferAuthorizationDto.getTransfer();
        validateAuthorization(transfer);

        final String comments = transferAuthorizationDto.getComments();
        final boolean showToMember = transferAuthorizationDto.isShowToMember();

        // Update transfer
        final AuthorizationLevel authorizationLevel = transfer.getNextAuthorizationLevel();
        transfer = transferDao.updateAuthorizationData(transfer.getId(), Transfer.Status.DENIED, null, null);

        // If the transfer is part of a scheduled payment, deny transfers of the payment with status PENDING, SCHEDULED or BLOCKED
        if (transfer.getScheduledPayment() != null) {
            final ScheduledPayment scheduledPayment = fetchService.fetch(transfer.getScheduledPayment(), ScheduledPayment.Relationships.TRANSFERS);
            for (final Transfer currentTransfer : scheduledPayment.getTransfers()) {
                final Transfer.Status currentTransferStatus = currentTransfer.getStatus();
                if (currentTransferStatus == Transfer.Status.PENDING || currentTransferStatus == Transfer.Status.SCHEDULED || currentTransferStatus == Transfer.Status.BLOCKED) {
                    transferDao.updateAuthorizationData(currentTransfer.getId(), Transfer.Status.DENIED, null, null);
                }
            }
            scheduledPaymentService.updateScheduledPaymentStatus(transfer.getScheduledPayment());
        }

        // Create the transfer authorization object
        final TransferAuthorization authorization = createAuthorization(transfer, TransferAuthorization.Action.DENY, authorizationLevel, comments, showToMember);

        // Update the from account status only
        accountStatusHandler.processAuthorization(transfer.getFrom(), transfer, authorization);

        // Update child transfers
        for (final Transfer childTransfer : transfer.getChildren()) {
            updateChildTransfer(transfer, childTransfer, authorization, true);
        }

        return transfer;
    }

    private AuthorizationLevel getHigherLevel(final Transfer transfer) {
        final AuthorizationLevel authorizationLevel = transfer.getNextAuthorizationLevel();
        final List<AuthorizationLevel> authorizationLevels = new ArrayList<AuthorizationLevel>(transfer.getType().getAuthorizationLevels());
        final int index = authorizationLevels.indexOf(authorizationLevel);
        if (index < 0 || index == authorizationLevels.size() - 1) {
            // This level is the highest
            return null;
        }
        final AuthorizationLevel wouldBeNext = authorizationLevels.get(index + 1);
        // Amount is not enough for the next level
        if (transfer.getAmount().compareTo(wouldBeNext.getAmount()) < 0) {
            return null;
        }
        return wouldBeNext;
    }

    /*
     * Fetchs the transfer, initializes the transfer type authorization levels and update TransferAuthorizationDTO object
     */
    private void initializeTransferAuthorizationDto(final TransferAuthorizationDTO transferAuthorizationDto) {
        // Fetch transfer
        Transfer transfer = transferAuthorizationDto.getTransfer();
        transfer = fetchService.fetch(transfer, Transfer.Relationships.NEXT_AUTHORIZATION_LEVEL, Transfer.Relationships.TYPE, Transfer.Relationships.CHILDREN, Transfer.Relationships.SCHEDULED_PAYMENT);

        // Initialize transfer type authorization levels
        final TransferType transferType = transfer.getType();
        final Collection<AuthorizationLevel> authorizationLevels = fetchService.fetch(transferType.getAuthorizationLevels(), RelationshipHelper.nested(AuthorizationLevel.Relationships.ADMIN_GROUPS));
        transferType.setAuthorizationLevels(authorizationLevels);

        // Update transferAuthorizatonDto object
        transferAuthorizationDto.setTransfer(transfer);
    }

    private void updateChildTransfer(final Transfer parent, Transfer child, final TransferAuthorization authorization, final boolean processed) {
        child = fetchService.fetch(child, Transfer.Relationships.CHILDREN);
        transferDao.updateAuthorizationData(child.getId(), parent.getStatus(), parent.getNextAuthorizationLevel(), parent.getProcessDate());

        // Update the account status
        if (processed) {
            // First to, then from, for correct rate processing.
            accountStatusHandler.processAuthorization(child.getTo(), child, authorization);
            accountStatusHandler.processAuthorization(child.getFrom(), child, authorization);
        }

        // Update child transfers
        for (final Transfer childTransfer : child.getChildren()) {
            updateChildTransfer(child, childTransfer, authorization, processed);
        }
    }

    private void validateAdministratorAuthorization(final Transfer transfer) {
        final AuthorizationLevel currentAuthorizationLevel = fetchService.fetch(transfer.getNextAuthorizationLevel(), AuthorizationLevel.Relationships.ADMIN_GROUPS);
        if (currentAuthorizationLevel.getAuthorizer() == AuthorizationLevel.Authorizer.RECEIVER) {
            throw new PermissionDeniedException();
        }
        final Collection<AdminGroup> adminGroups = currentAuthorizationLevel.getAdminGroups();
        if (!adminGroups.contains(LoggedUser.group())) {
            throw new PermissionDeniedException();
        }
    }

    private void validateAuthorization(final Transfer transfer) {
        // Can't cancel a nested transfer
        if (!transfer.isRoot()) {
            throw new UnexpectedEntityException();
        }
    }

    private void validateAuthorizationLevel(final Transfer transfer, final AuthorizationLevel.Authorizer expected) {
        final AuthorizationLevel currentAuthorizationLevel = transfer.getNextAuthorizationLevel();
        if (currentAuthorizationLevel.getAuthorizer() != expected) {
            throw new PermissionDeniedException();
        }
    }

    private void validateCancel(final Transfer transfer) {
        if (transfer.getStatus() != Transfer.Status.PENDING) {
            throw new UnexpectedEntityException();
        }
    }

    /*
     * Deny requires comments
     */
    private void validateDenyAuthorization(final TransferAuthorizationDTO transferAuthorizationDto) {
        if (StringUtils.isEmpty(transferAuthorizationDto.getComments())) {
            throw new ValidationException("comments", "transferAuthorization.comments", new RequiredError());
        }
    }

    private void validateFromMemberAuthorization(final Transfer transfer) {
        final AuthorizationLevel currentAuthorizationLevel = transfer.getNextAuthorizationLevel();
        final Authorizer authorizer = currentAuthorizationLevel.getAuthorizer();
        if (authorizer == AuthorizationLevel.Authorizer.RECEIVER) {
            throw new PermissionDeniedException();
        }
        if (LoggedUser.isBroker() && authorizer != AuthorizationLevel.Authorizer.BROKER) {
            throw new PermissionDeniedException();
        }
        if (LoggedUser.isAdministrator()) {
            validateAdministratorAuthorization(transfer);
        }
    }
}