package nl.strohalm.cyclos.controls.tokens;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.tokens.SenderRedeemTokenData;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewDTO;
import org.apache.struts.action.ActionForward;

public class ConfirmSenderTokenRedemptionAction extends BaseTokenAction {

    @Override
    ActionForward tokenSubmit(BaseTokenForm token, Member loggedMember, ActionContext actionContext) {
        SenderRedeemTokenData senderRedeemTokenData = new SenderRedeemTokenData();
        senderRedeemTokenData.setPin(token.getPin());
        senderRedeemTokenData.setTransactionId(token.getTransactionId());
        LocalSettings localSettings = settingsService.getLocalSettings();
        Long ttId = actionContext.isBroker() ?
                localSettings.getBrokerSenderTokenRedemptionTransferType()
                : localSettings.getMemberSenderTokenRedemptionTransferType();

        senderRedeemTokenData.setTransferTypeId(ttId);

        tokenService.senderRedeemToken(loggedMember, senderRedeemTokenData);
        return actionContext.getSuccessForward();
    }

    @Override
    protected void prepareForm(ActionContext context) throws Exception {
        BaseTokenForm baseTokenForm = context.getForm();

        Long ttId = context.isBroker() ? settingsService.getLocalSettings().getBrokerSenderTokenRedemptionTransferType()
                        : settingsService.getLocalSettings().getMemberSenderTokenRedemptionTransferType();
        TransferType tt = transferTypeService.load(ttId);

        Token token = tokenService.loadTokenByTransactionId(baseTokenForm.getTransactionId());
        TransactionFeePreviewDTO transferFees = transactionFeeService.preview(
                SystemAccountOwner.instance(), loadLoggedMember(context), tt, token.getAmount());
        context.getRequest().setAttribute("token", token);
        context.getRequest().setAttribute("unitsPattern", tt.getFrom().getCurrency().getPattern());
        context.getRequest().setAttribute("finalAmount", transferFees.getFinalAmount());

        context.getRequest().setAttribute("fees", transferFees.getFees());

    }

}
