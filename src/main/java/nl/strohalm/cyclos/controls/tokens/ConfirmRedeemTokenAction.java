package nl.strohalm.cyclos.controls.tokens;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.tokens.GenerateTokenDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewDTO;
import org.apache.struts.action.ActionForward;

public class ConfirmRedeemTokenAction extends BaseTokenAction<GenerateTokenDTO> {

    @Override
    ActionForward tokenSubmit(BaseTokenForm token, Member loggedMember, ActionContext context) throws Exception {
        String pin = (String) token.getToken("pin");
        Long ttId = context.isBroker() ? settingsService.getLocalSettings().getBrokerRedeemTokenTransferType()
                : settingsService.getLocalSettings().getMemberRedeemTokenTransferType();
        tokenService.redeemToken(loggedMember, token.getTokenId(), pin, ttId);
        context.sendMessage("tokens.tokenRedeemed", token.getTokenId());
        return context.getSuccessForward();
    }


    @Override
    protected void prepareForm(ActionContext context) throws Exception {
        BaseTokenForm baseTokenForm = context.getForm();

        Long ttId = context.isBroker() ? settingsService.getLocalSettings().getBrokerRedeemTokenTransferType()
                : settingsService.getLocalSettings().getMemberRedeemTokenTransferType();
        TransferType tt = transferTypeService.load(ttId);

        Token token = tokenService.loadTokenById(baseTokenForm.getTokenId());
        TransactionFeePreviewDTO transferFees = transactionFeeService.preview(
                SystemAccountOwner.instance(), loadLoggedMember(context), tt, token.getAmount());
        context.getRequest().setAttribute("token", token);
        context.getRequest().setAttribute("unitsPattern", tt.getFrom().getCurrency().getPattern());
        context.getRequest().setAttribute("finalAmount", transferFees.getFinalAmount());

        context.getRequest().setAttribute("fees", transferFees.getFees());

    }

}
