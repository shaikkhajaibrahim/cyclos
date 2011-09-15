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
package mp.platform.cyclone.webservices.pos;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.Channel.Principal;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.pos.MemberPos;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorizationDTO;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings.TransactionNumber;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.accounts.pos.MemberPosService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.DoExternalPaymentDTO;
import nl.strohalm.cyclos.services.transactions.DoPaymentDTO;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transactions.TransferAuthorizationService;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.MessageHelper;
import nl.strohalm.cyclos.utils.query.PageParameters;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.WebServiceFaultsEnum;
import mp.platform.cyclone.webservices.accounts.AccountHistoryResultPage;
import mp.platform.cyclone.webservices.model.AccountHistoryTransferVO;
import mp.platform.cyclone.webservices.model.AccountStatusVO;
import mp.platform.cyclone.webservices.model.DetailedAccountTypeVO;
import mp.platform.cyclone.webservices.model.TransactionNumberVO;
import mp.platform.cyclone.webservices.model.WSPrincipalType;
import mp.platform.cyclone.webservices.model.WSPrincipalType.WSPrincipal;
import mp.platform.cyclone.webservices.payments.ChargebackResult;
import mp.platform.cyclone.webservices.payments.ChargebackStatus;
import mp.platform.cyclone.webservices.payments.PaymentResult;
import mp.platform.cyclone.webservices.payments.PaymentStatus;
import mp.platform.cyclone.webservices.utils.WebServiceHelper;
import mp.platform.cyclone.webservices.utils.server.AccountHelper;
import mp.platform.cyclone.webservices.utils.server.MemberHelper;
import mp.platform.cyclone.webservices.utils.server.PaymentHelper;

/**
 * Implementation for PosWebService <br>
 * <b>Important: Please take care of the WebServiceContext initialization. Each web service operation MUST call #initContext(String) at the beginning
 * of its implementation.</b>
 * @author luis
 */
@WebService(name = "pos", serviceName = "pos")
public class PosWebServiceImpl implements PosWebService {

    private AccessService                accessService;
    private TransferTypeService          transferTypeService;
    private ElementService               elementService;
    private AccountService               accountService;
    private MemberPosService             memberPosService;
    private ChannelService               channelService;
    private SettingsService              settingsService;
    private PaymentService               paymentService;
    private AccountHelper                accountHelper;
    private PaymentHelper                paymentHelper;
    private MemberHelper                 memberHelper;
    private WebServiceHelper             webServiceHelper;
    private TransferAuthorizationService transferAuthorizationService;

    public ChargebackResult chargeback(final ChargebackParameters parameters) {
        ChargebackStatus status = null;
        final Member member = WebServiceContext.getMember();

        // Find the transfer
        Transfer transfer = null;
        Transfer chargebackTransfer = null;
        try {
            transfer = paymentService.load(parameters.getTransferId());
            // Ensure the member is the one who received the payment
            if (!transfer.getToOwner().equals(member)) {
                throw new EntityNotFoundException();
            }
        } catch (final EntityNotFoundException e) {
            status = ChargebackStatus.TRANSFER_NOT_FOUND;
        }
        // Check if the transfer can be charged back
        if (status == null && !paymentService.canChargeback(transfer, false)) {
            if (transfer.getChargedBackBy() != null) {
                status = ChargebackStatus.TRANSFER_ALREADY_CHARGEDBACK;
            } else {
                if (transfer.getStatus() == Payment.Status.PENDING) {
                    final TransferAuthorizationDTO transferAuthorizationDto = new TransferAuthorizationDTO();
                    transferAuthorizationDto.setTransfer(transfer);
                    transferAuthorizationDto.setShowToMember(false);
                    chargebackTransfer = transferAuthorizationService.cancelFromMemberAsReceiver(transferAuthorizationDto);
                    status = ChargebackStatus.SUCCESS;
                } else {
                    status = ChargebackStatus.TRANSFER_CANNOT_BE_CHARGEDBACK;
                }
            }
        }

        // Check the amount
        if (status == null) {
            final LocalSettings localSettings = settingsService.getLocalSettings();
            if (!localSettings.truncate(transfer.getAmount()).equals(localSettings.truncate(parameters.getAmount()))) {
                status = ChargebackStatus.INVALID_PARAMETERS;
            }
        }

        // Do the chargeback
        if (status == null) {
            chargebackTransfer = paymentService.chargebackReceivedPayment(transfer);
            status = ChargebackStatus.SUCCESS;
        }

        if (!status.isSuccessful()) {
            webServiceHelper.error("Chargeback result " + status);
        }

        // Build the result
        if (status == ChargebackStatus.SUCCESS || status == ChargebackStatus.TRANSFER_ALREADY_CHARGEDBACK) {
            final AccountHistoryTransferVO originalVO = accountHelper.toVO(member, transfer, null);
            final AccountHistoryTransferVO chargebackVO = accountHelper.toVO(member, chargebackTransfer, null);
            return new ChargebackResult(status, originalVO, chargebackVO);
        } else {
            return new ChargebackResult(status, null, null);
        }
    }

    public AccountStatusVO getAccountStatus(final GetAccountStatusParameters parameters) {
        final Member member = WebServiceContext.getMember();
        final MemberAccountType accountType = EntityHelper.reference(MemberAccountType.class, parameters.getAccountTypeId());
        final AccountStatus accountStatus = accountService.getStatus(new GetTransactionsDTO(member, accountType));
        return accountHelper.toVO(accountStatus);
    }

    @SuppressWarnings("unchecked")
    public PosInitializationData getInitializationData(final InitializationParameters params) {
        final MemberPos memberPos = WebServiceContext.getPos().getMemberPos();
        final Member member = memberPos.getMember();

        final PosInitializationData initializationData = new PosInitializationData();

        // Set the owner
        initializationData.setOwner(memberHelper.toVO(memberPos.getMember()));

        // Get the POS channel principal type
        final Channel posChannel = WebServiceContext.getChannel();
        final List<WSPrincipalType> principalTypes = new ArrayList<WSPrincipalType>();
        final PrincipalType defaultPrincipalType = posChannel.getDefaultPrincipalType();
        for (final PrincipalType principalType : posChannel.getPrincipalTypes()) {
            final WSPrincipalType type = new WSPrincipalType();
            final Principal principal = principalType.getPrincipal();
            type.setPrincipal(WSPrincipal.valueOf(principal.name()));
            type.setDefault(principalType.equals(defaultPrincipalType));
            final MemberCustomField customField = principalType.getCustomField();
            if (customField != null) {
                type.setCustomFieldInternalName(customField.getInternalName());
                type.setLabel(customField.getName());
            } else {
                type.setLabel(MessageHelper.message(WebServiceContext.getRequest(), principal.getKey()));
            }

            principalTypes.add(type);
        }
        initializationData.setPrincipalTypes(principalTypes);

        // Get the member accounts
        final List<DetailedAccountTypeVO> accountVOs = new ArrayList<DetailedAccountTypeVO>();
        for (final MemberAccount memberAccount : (List<MemberAccount>) accountService.getAccounts(member)) {
            if (memberAccount.getStatus() == MemberAccount.Status.ACTIVE) { // only add the active accounts
                accountVOs.add(accountHelper.toDetailedTypeVO(Channel.POS, memberAccount));
            }
        }
        initializationData.setAccountTypes(accountVOs);

        // Get data from settings
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final TransactionNumber transactionNumber = localSettings.getTransactionNumber();
        if (transactionNumber != null) {
            initializationData.setTransactionNumber(new TransactionNumberVO(transactionNumber.getPrefix(), transactionNumber.getPadLength(), transactionNumber.getSuffix()));
        }
        initializationData.setDecimalDigits(localSettings.getPrecision().getValue());

        // Get data from MemberPos
        initializationData.setMaxSchedulingPayments(memberPos.getMaxSchedulingPayments());
        initializationData.setNumberOfCopies(memberPos.getNumberOfCopies());
        initializationData.setResultPageSize(memberPos.getResultPageSize());
        initializationData.setAllowMakePayment(memberPos.isAllowMakePayment());

        return initializationData;
    }

    public PaymentResult makePayment(final MakePaymentParameters params) {
        PaymentStatus status = null;
        final MemberPos memberPos = WebServiceContext.getPos().getMemberPos();
        DoPaymentDTO dto = null;
        Member toMember = null;
        TransferType transferType = null;
        try {
            // In that case the pos pin is not validated by the interceptor (MakePaymentParameters not implements the IPosPinParameter interface)
            // because we want to set a status indicating the error
            checkPin(params.getPosPin());
        } catch (final Exception e) {
            webServiceHelper.error(e);
            status = paymentHelper.toStatus(e);
        }
        if (status == null) {
            // Ensure make payment is enabled
            if (!memberPos.isAllowMakePayment()) {
                WebServiceFaultsEnum.UNAUTHORIZED_ACCESS.throwFault("Make payment is not allowed");
            }
            // Get the parameters
            try {
                final PrincipalType principalType = channelService.resolvePrincipalType(Channel.POS, params.getToMemberPrincipalType());
                toMember = elementService.loadByPrincipal(principalType, params.getToMemberPrincipal());
                transferType = transferTypeService.load(params.getTransferTypeId());
            } catch (final Exception e) {
                webServiceHelper.error(e);
                status = PaymentStatus.INVALID_PARAMETERS;
            }
        }
        // Ok so far: set the transfer DTO
        if (status == null) {
            dto = new DoPaymentDTO();
            dto.setContext(TransactionContext.PAYMENT);
            dto.setChannel(Channel.POS);
            dto.setAmount(params.getAmount());
            dto.setFrom(memberPos.getMember());
            dto.setTo(toMember);
            dto.setTransferType(transferType);
        }
        // Perform the payment
        AccountHistoryTransferVO transferVO = null;
        if (status == null) {
            try {
                final Transfer transfer = (Transfer) paymentService.doPaymentFromMemberToMember(dto);
                status = paymentHelper.toStatus(transfer);
                transferVO = accountHelper.toVO(dto.getFrom(), transfer, null);
            } catch (final Exception e) {
                webServiceHelper.error(e);
                status = paymentHelper.toStatus(e);
            }
        }
        return new PaymentResult(status, transferVO);
    }

    public PaymentResult receivePayment(final ReceivePaymentParameters params) {
        // Get the member pos
        final Member member = WebServiceContext.getMember();

        final PrincipalType principalType = channelService.resolvePrincipalType(Channel.POS, params.getFromMemberPrincipalType());

        // Get the parameters
        PaymentStatus status = null;
        DoExternalPaymentDTO dto = null;
        Member fromMember = null;
        TransferType transferType = null;
        try {
            final HttpServletRequest request = WebServiceContext.getRequest();
            final String principal = params.getFromMemberPrincipal();
            final String credentials = params.getFromMemberCredentials();
            final String remoteAddress = request.getRemoteAddr();
            final MemberUser user = accessService.checkCredentials(Channel.POS, principalType, principal, credentials, remoteAddress, member);
            fromMember = user.getMember();
            transferType = transferTypeService.load(params.getTransferTypeId());
        } catch (final Exception e) {
            webServiceHelper.error(e);
            status = paymentHelper.toStatus(e);
        }
        // Ok so far: set the transfer DTO
        if (status == null) {
            dto = new DoExternalPaymentDTO();
            dto.setContext(TransactionContext.PAYMENT);
            dto.setChannel(Channel.POS);
            dto.setAmount(params.getAmount());
            dto.setFrom(fromMember);
            dto.setTo(member);
            dto.setTransferType(transferType);
        }
        // Perform the payment
        AccountHistoryTransferVO transferVO = null;
        if (status == null) {
            try {
                final Transfer transfer = (Transfer) paymentService.insertExternalPayment(dto);
                status = paymentHelper.toStatus(transfer);
                transferVO = accountHelper.toVO(member, transfer, null);
            } catch (final Exception e) {
                webServiceHelper.error(e);
                status = paymentHelper.toStatus(e);
            }
        }
        return new PaymentResult(status, transferVO);
    }

    public AccountHistoryResultPage searchAccountHistory(final GetAccountHistoryParameters parameters) {
        final MemberPos memberPos = WebServiceContext.getPos().getMemberPos();

        // Prepare the parameters
        final Member member = memberPos.getMember();
        final int pageSize = memberPos.getResultPageSize();
        final int currentPage = parameters.getCurrentPage();
        final AccountType accountType = EntityHelper.reference(AccountType.class, parameters.getAccountTypeId());

        // Query the transfers
        final TransferQuery query = new TransferQuery();
        query.setOwner(member);
        query.setType(accountType);
        query.setPageParameters(new PageParameters(pageSize, currentPage));
        final List<Transfer> transfers = paymentService.search(query);

        return accountHelper.toResultPage(member, transfers);
    }

    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    public void setAccountHelper(final AccountHelper accountHelper) {
        this.accountHelper = accountHelper;
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    public void setMemberPosService(final MemberPosService memberPosService) {
        this.memberPosService = memberPosService;
    }

    public void setPaymentHelper(final PaymentHelper paymentHelper) {
        this.paymentHelper = paymentHelper;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransferAuthorizationService(final TransferAuthorizationService transferAuthorizationService) {
        this.transferAuthorizationService = transferAuthorizationService;
    }

    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public void setWebServiceHelper(final WebServiceHelper webServiceHelper) {
        this.webServiceHelper = webServiceHelper;
    }

    private void checkPin(final String posPin) {
        memberPosService.checkPin(WebServiceContext.getPos().getMemberPos(), posPin);
    }
}
