package nl.strohalm.cyclos.controls.tokens;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.tokens.GenerateTokenDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeeService;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import org.apache.struts.action.ActionForward;

import java.math.BigDecimal;

public class ConfirmGenerateTokenAction extends BaseTokenAction<GenerateTokenDTO> {

    TransactionFeeService transactionFeeService;

    TransferTypeService transferTypeService;

    @Inject
    public void setTransactionFeeService(TransactionFeeService transactionFeeService) {
        this.transactionFeeService = transactionFeeService;
    }

    @Inject
    public void setTransferTypeService(TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    ActionForward tokenSubmit(BaseTokenForm form, Member loggedMember, ActionContext context) {
        final GenerateTokenDTO generateTokenDTO = (GenerateTokenDTO) context.getSession().getAttribute("token");
        String tokenId = tokenService.generateToken(generateTokenDTO);
        context.getSession().removeAttribute("token");
        context.sendMessage("tokens.tokenGenerated", tokenId);
        return context.getSuccessForward();
    }

    @Override
    protected void prepareForm(ActionContext context) throws Exception {

        final GenerateTokenDTO generateTokenDTO = (GenerateTokenDTO) context.getSession().getAttribute("token");
        Long ttId = context.isBroker() ? settingsService.getLocalSettings().getBrokerTokenGenerationTransferType()
                : settingsService.getLocalSettings().getMemberTokenGenerationTransferType();
        TransferType tt = transferTypeService.load(ttId);

        context.getRequest().setAttribute("token", generateTokenDTO);
        TransactionFeePreviewDTO transferFees = transactionFeeService.preview(loadLoggedMember(context), SystemAccountOwner.instance(),
                tt, generateTokenDTO.getAmount());
        context.getRequest().setAttribute("token", generateTokenDTO);
        context.getRequest().setAttribute("unitsPattern", tt.getFrom().getCurrency().getPattern());
        context.getRequest().setAttribute("finalAmount", transferFees.getFinalAmount());

        context.getRequest().setAttribute("fees", transferFees.getFees());

    }

    @Override
    protected DataBinder createBinder(LocalSettings localSettings) {
        final BeanBinder<GenerateTokenDTO> binder = BeanBinder.instance(GenerateTokenDTO.class);
        binder.registerBinder("amount", PropertyBinder.instance(BigDecimal.class, "amount", localSettings.getNumberConverter()));
        binder.registerBinder("senderMobilePhone", PropertyBinder.instance(String.class, "senderMobilePhone"));
        binder.registerBinder("recipientMobilePhone", PropertyBinder.instance(String.class, "recipientMobilePhone"));
        return binder;
    }
}
