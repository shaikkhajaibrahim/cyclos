package nl.strohalm.cyclos.controls.tokens;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.tokens.ResetPinTokenData;
import org.apache.struts.action.ActionForward;

public class ConfirmResetPinTokenAction extends BaseTokenAdminAction {

    @Override
    ActionForward tokenSubmit(BaseTokenForm token, ActionContext actionContext) {
        ResetPinTokenData resetPinTokenData = new ResetPinTokenData();
        resetPinTokenData.setTransactionId(token.getTransactionId());

        tokenService.resetPinToken(resetPinTokenData);
        actionContext.sendMessage("tokens.tokenResetPin", token.getTransactionId());
        return actionContext.getSuccessForward();
    }

    @Override
    protected void prepareForm(ActionContext context) throws Exception {
        BaseTokenForm baseTokenForm = context.getForm();

        Long ttId = settingsService.getLocalSettings().getBrokerRedeemTokenTransferType();
        TransferType tt = transferTypeService.load(ttId);

        Token token = tokenService.loadTokenByTransactionId(baseTokenForm.getTransactionId());
        context.getRequest().setAttribute("token", token);
        context.getRequest().setAttribute("unitsPattern", tt.getFrom().getCurrency().getPattern());
    }

}
