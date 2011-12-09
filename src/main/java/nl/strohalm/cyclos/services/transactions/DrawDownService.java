package nl.strohalm.cyclos.services.transactions;

import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.CurrencyService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *  Performs draw down process on selected system accounts.
 */
public class DrawDownService {

    private static final Log LOG = LogFactory.getLog(DrawDownService.class);

    private PaymentService paymentService;

    private AccountService accountService;

    private TransferTypeService transferTypeService;

    private CurrencyService currencyService;

    private Long destinationAccountId;

    private Account destinationAccount;

    private List<Account> sourceAccountList;

    private Map<Long, Long> sourceAccountIdAndTransferTypeIdMap;

    public DrawDownService() {
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public void setTransferTypeService(TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public void setCurrencyService(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    public void setSourceAccountIdAndTransferTypeIdMap(Map<Long, Long> sourceAccountIdAndTransferTypeIdMap) {
        this.sourceAccountIdAndTransferTypeIdMap = sourceAccountIdAndTransferTypeIdMap;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public String start() {
        drawDownReport = new StringBuffer();
        appendToReport("\nDraw down process started at "
                + new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(new Date()));
        appendToReport("\n\nDraw down accounts balances : \n");
        setUpAccounts();
        appendToReport("\n\nDraw down payments : \n");
        doPayments();
        return drawDownReport.toString();
    }

    private void setUpAccounts() {
        SystemAccountOwner accountOwner = SystemAccountOwner.instance();
        List<Account> systemAccountList = (List<Account>) accountService.getAccounts(accountOwner);
        findDrawDownAccount(systemAccountList);
        extractSourceAccountList(systemAccountList);
    }

    private void extractSourceAccountList(List<Account> accountList) {
        SystemAccountOwner accountOwner = SystemAccountOwner.instance();
        List<Account> systemAccountList = (List<Account>) accountService.getAccounts(accountOwner);

        sourceAccountList = new ArrayList<Account>(systemAccountList.size());

        for (Account account : systemAccountList) {
            LOG.error("\nLoggedUser.accountOwner() " + LoggedUser.accountOwner() + " account type id "
                    + account.getType().getId() + " owner " + account.getOwner().toString()
                    + " name " + account.getOwnerName() + " balance " + getBalance(account));
            if (sourceAccountIdAndTransferTypeIdMap.keySet().contains(account.getType().getId())) {
                appendToReport("\naccount : " + account.getOwnerName() + " balance : " + getBalance(account) + " NGN");
                sourceAccountList.add(account);
            }
        }
    }

    private StringBuffer drawDownReport;

    private StringBuffer appendToReport(String text) {
        return drawDownReport.append(text);
    }

    private void doPayments() {
        for ( Account account : sourceAccountList) {
            doDrawDownPayment(account);
        }
    }
    
    private String createDescription() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z");
        StringBuffer sb = new StringBuffer();
        sb.append("Draw Down transfer ");
        sb.append(simpleDateFormat.format(new Date()));
        return sb.toString();
    }

    private void doDrawDownPayment(Account sourceAccount) {
        BigDecimal amount = getBalance(sourceAccount);
        if ( amount.compareTo(new BigDecimal("0")) <= 0 ) {
            LOG.error("account " + sourceAccount.getType().getId() + " is empty");
            return;
        }
        DoPaymentDTO doPaymentDTO = new DoPaymentDTO();
        doPaymentDTO.setAmount(amount);
        doPaymentDTO.setDescription(createDescription());
        doPaymentDTO.setChannel(Channel.WEB);
        doPaymentDTO.setTo(destinationAccount.getOwner());

        doPaymentDTO.setCurrency(currencyService.loadBySymbolOrId("NGN"));
        doPaymentDTO.setTransferType(transferTypeService.load(
                sourceAccountIdAndTransferTypeIdMap.get(sourceAccount.getType().getId())));

        doPaymentDTO.setFrom(sourceAccount.getOwner());
        doPaymentDTO.setContext(TransactionContext.SELF_PAYMENT);
        try {
            paymentService.doPaymentFromSystemToSystem(doPaymentDTO);
            appendToReport("\npayment from " + sourceAccount.getOwnerName() + " to " + destinationAccount.getOwnerName()
                            + " amount " + amount.toString() + " NGN");
        } catch (Throwable th) {
            LOG.error("exception " + th.getMessage(), th);
            appendToReport("\nFAILED payment execution from " + sourceAccount.getOwnerName() + " to "
                            + destinationAccount.getOwnerName()
                            + " amount " + amount.toString() + " NGN - error : " + th.getMessage() );
        }
    }

    private BigDecimal getBalance(Account account) {
        return accountService.getBalance(new GetTransactionsDTO(account)).setScale(2);
    }

    private void findDrawDownAccount(List<Account> accountList) {
        for (Account account : accountList) {
            if (account.getType().getId() == destinationAccountId) {
                destinationAccount = account;
            }
        }
        if (destinationAccount == null) {
            throw new RuntimeException("Draw down account not defined : id = " + destinationAccountId);
        }
    }

}
