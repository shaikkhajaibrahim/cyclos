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
package nl.strohalm.cyclos.services.transfertypes;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.accounts.fee.transaction.TransactionFeeDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.dao.members.brokerings.BrokeringCommissionStatusDAO;
import nl.strohalm.cyclos.dao.members.brokerings.DefaultBrokerCommissionDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.BrokerCommission;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.SimpleTransactionFee;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFee;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFeeQuery;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.BrokerCommission.When;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.SimpleTransactionFee.ARateRelation;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFee.ChargeType;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFee.Nature;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFee.Subject;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContract;
import nl.strohalm.cyclos.entities.members.brokerings.Brokering;
import nl.strohalm.cyclos.entities.members.brokerings.BrokeringCommissionStatus;
import nl.strohalm.cyclos.entities.members.brokerings.BrokeringCommissionStatusQuery;
import nl.strohalm.cyclos.entities.members.brokerings.DefaultBrokerCommission;
import nl.strohalm.cyclos.entities.members.brokerings.DefaultBrokerCommissionQuery;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.AccountDTO;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.rates.ARateService;
import nl.strohalm.cyclos.services.accounts.rates.ARatedFeeDTO;
import nl.strohalm.cyclos.services.accounts.rates.DRateService;
import nl.strohalm.cyclos.services.accounts.rates.RatesDTO;
import nl.strohalm.cyclos.services.elements.BrokeringService;
import nl.strohalm.cyclos.services.elements.CommissionService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.Amount;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.MessageResolver;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.TimePeriod.Field;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.conversion.NumberConverter;
import nl.strohalm.cyclos.utils.conversion.UnitsConverter;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.GeneralValidation;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.Validator;
import nl.strohalm.cyclos.utils.validation.Validator.Property;

/**
 * Implementation class for transaction fee service
 * @author Jefferson Magno
 * @author rafael
 * @author Rinke (everything with rates)
 */
public class TransactionFeeServiceImpl implements TransactionFeeService {

    /**
     * Count is required when "When" property is COUNT or DAYS
     * @author Jefferson Magno
     */
    public class CountValidation implements PropertyValidation {
        private static final long serialVersionUID = -8075223146818925038L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final BrokerCommission commission = (BrokerCommission) object;
            final When when = commission.getWhen();
            if (when == When.COUNT || when == When.DAYS) {
                return RequiredValidation.instance().validate(object, property, value);
            }
            return null;
        }
    }

    /**
     * Validates the generated transfer type
     * @author Jefferson Magno
     */
    public class GeneratedTransferTypeValidation implements GeneralValidation {
        private static final long serialVersionUID = 1616929350799341483L;

        public ValidationError validate(final Object object) {
            final TransactionFee fee = (TransactionFee) object;
            final TransferType generatedType = fetchService.fetch(fee.getGeneratedTransferType(), TransferType.Relationships.FROM);
            fee.setGeneratedTransferType(generatedType);

            // This validation is necessary only when updating a transaction fee
            if (fee.isTransient()) {
                return null;
            }

            // Retrieve saved fee and it's generated transfer type
            final TransactionFee savedFee = load(fee.getId(), TransactionFee.Relationships.GENERATED_TRANSFER_TYPE);
            final TransferType savedGeneratedType = savedFee.getGeneratedTransferType();

            // If it's a broker commission paid by member, the generated type cannot be changed
            if (savedFee.getNature() == Nature.BROKER && !savedFee.isFromSystem()) {
                if (!generatedType.equals(savedGeneratedType)) {
                    return new ValidationError("transactionFee.error.cannotChangeGeneratedType");
                }
            }

            // It's not allowed to change a "from system" generated type to a "from member" generated type
            if (savedFee.isFromSystem() && !fee.isFromSystem()) {
                return new ValidationError("transactionFee.erro.fromSystemGeneratedTypeRequired");
            }

            return null;
        }
    }

    private static class PayerAndReceiverValidation implements GeneralValidation {

        private static final long serialVersionUID = -1969165853079125625L;

        public ValidationError validate(final Object object) {
            final SimpleTransactionFee fee = (SimpleTransactionFee) object;
            final Subject payer = fee.getPayer();
            final Subject receiver = fee.getReceiver();
            if ((payer == Subject.SOURCE || payer == Subject.DESTINATION) && payer == receiver) {
                return new ValidationError("transactionFee.error.samePayerAndReceiver");
            }
            return null;
        }
    }

    private MessageResolver              messageResolver;

    private AccountService               accountService;

    private DRateService                 dRateService;
    private ARateService                 aRateService;
    private BrokeringService             brokeringService;
    private CommissionService            commissionService;
    private FetchService                 fetchService;
    private SettingsService              settingsService;
    private TransferTypeService          transferTypeService;
    private BrokeringCommissionStatusDAO brokeringCommissionStatusDao;
    private DefaultBrokerCommissionDAO   defaultBrokerCommissionDao;
    private TransactionFeeDAO            transactionFeeDao;

    public Transfer buildTransfer(final BuildTransferWithFeesDTO params) {
        final Calendar date = params.getDate();
        final Account from = params.getFrom();
        final Account to = params.getTo();
        final BigDecimal transferAmount = params.getTransferAmount();
        TransactionFee fee = params.getFee();
        final boolean simulation = params.isSimulation();
        final BigDecimal aRateParam = params.getARate();
        final BigDecimal dRateParam = params.getDRate();

        if (fee.isTransient()) {
            throw new UnexpectedEntityException();
        }
        fee = fetchService.fetch(fee, TransactionFee.Relationships.ORIGINAL_TRANSFER_TYPE, RelationshipHelper.nested(TransactionFee.Relationships.GENERATED_TRANSFER_TYPE, TransferType.Relationships.FROM, TransferType.Relationships.TO, AccountType.Relationships.CURRENCY), TransactionFee.Relationships.FROM_GROUPS, TransactionFee.Relationships.TO_GROUPS, TransactionFee.Relationships.FROM_FIXED_MEMBER, SimpleTransactionFee.Relationships.TO_FIXED_MEMBER);

        if (!doTests(fee, transferAmount, from, to)) {
            return null;
        }

        final AccountOwner originalFromAccountOwner = from.getOwner();

        BrokerCommissionContract commissionContract = null;

        ChargeType feeChargeType = fee.getChargeType();
        BigDecimal feeValue = fee.getValue();
        final TransferType generated = fee.getGeneratedTransferType();

        final AccountOwner fromOwner = getFromOwner(fee, originalFromAccountOwner, to);

        // Get the to owner
        AccountOwner toOwner = null;
        if (fee.getNature() == Nature.SIMPLE) {
            toOwner = getToOwner(fee, originalFromAccountOwner, to);
        } else { // It´s a broker commission
            // TODO this is an extremely long method. What about putting this broker part in a separate method?
            final BrokerCommission brokerCommission = (BrokerCommission) fee;

            // Check if the broker is the source´s broker or is destination´s broker
            Account relatedAccount = null;
            switch (brokerCommission.getWhichBroker()) {
                case SOURCE:
                    relatedAccount = from;
                    break;
                case DESTINATION:
                    relatedAccount = to;
                    break;
            }

            // When source member pays destination´s broker or when destination member pays source´s broker, it´s a cross payment
            boolean crossPayment = false;
            if ((fee.getPayer() == TransactionFee.Subject.SOURCE && brokerCommission.getWhichBroker() == BrokerCommission.WhichBroker.DESTINATION) || (fee.getPayer() == TransactionFee.Subject.DESTINATION && brokerCommission.getWhichBroker() == BrokerCommission.WhichBroker.SOURCE)) {
                crossPayment = true;
            }

            // The toOwner is a member's broker
            if (relatedAccount instanceof MemberAccount) {
                final Member member = ((MemberAccount) relatedAccount).getMember();
                toOwner = member.getBroker();

                // For a broker commission, first, get the brokering
                final Brokering brokering = brokeringService.getActiveBrokering(member);
                // There is no active brokering, return null
                if (brokering == null) {
                    return null;
                }
                final Member broker = brokering.getBroker();

                // Check broker groups
                if (!brokerCommission.isAllBrokerGroups()) {
                    final BrokerGroup brokerGroup = (BrokerGroup) fetchService.fetch(brokering.getBroker().getGroup());
                    if (!brokerCommission.getBrokerGroups().contains(brokerGroup)) {
                        return null;
                    }
                }

                // When the broker commission status is created, it's data is gotten from the default broker commission (commissions
                // paid by member) or from the broker commission (commissions paid by system)
                BrokeringCommissionStatus brokeringCommissionStatus = commissionService.getBrokeringCommissionStatus(brokering, brokerCommission);

                boolean testBrokeringCommissionStatus = false;
                When when = null;
                int maxCount = 0;
                Amount feeAmount;
                // Member paying commission to one broker
                final boolean fromMember = generated.isFromMember();
                if (fromMember) {
                    if (crossPayment) {
                        // On cross payments, the commission is always charged and the fee is retrieved from the commission it self
                        // The brokering commission status conditions are never tested
                        feeAmount = brokerCommission.getAmount();
                    } else {
                        commissionContract = commissionService.getActiveBrokerCommissionContract(brokering, brokerCommission);
                        if (commissionContract != null) {
                            // There is an active broker contract, the fee is retrieved from the contract
                            feeAmount = commissionContract.getAmount();
                        } else if (brokeringCommissionStatus != null) {
                            // There is a default broker commission, the fee is retrieved from the brokering commission status
                            if (brokeringCommissionStatus.getPeriod().getEnd() != null) {
                                return null;
                            }
                            // Parameters for testing broker commission status conditions
                            testBrokeringCommissionStatus = true;
                            when = brokeringCommissionStatus.getWhen();
                            if (when != BrokerCommission.When.ALWAYS) {
                                maxCount = brokeringCommissionStatus.getMaxCount();
                            }
                            feeAmount = brokeringCommissionStatus.getAmount();
                        } else {
                            // If there is not a contract and there is not a default broker commission, no commission will be charged
                            return null;
                        }
                    }
                } else {
                    // System paying a commission to a broker
                    // Parameters for testing brokering commission status conditions
                    testBrokeringCommissionStatus = true;
                    when = brokerCommission.getWhen();
                    if (when != BrokerCommission.When.ALWAYS) {
                        maxCount = brokerCommission.getCount();
                    }
                    feeAmount = brokeringCommissionStatus.getAmount();
                }

                // Set the charge data
                feeChargeType = ChargeType.from(feeAmount.getType());
                feeValue = feeAmount.getValue();

                // Test brokering commission status conditions (suspended, number of transactions or period of validity)
                if (testBrokeringCommissionStatus) {
                    // When member's pay, a DefaultBrokerCommission is required
                    if (fromMember) {
                        // The broker commission is suspended, don´t charge the commission
                        final DefaultBrokerCommission defaultBrokerCommission = commissionService.getDefaultBrokerCommission(broker, brokerCommission);
                        if (defaultBrokerCommission == null || defaultBrokerCommission.isSuspended()) {
                            return null;
                        }
                    }
                    // The brokering commission status is closed, don't charge commission anymore
                    if (brokeringCommissionStatus.getPeriod().getEnd() != null) {
                        return null;
                    }
                    if (when == BrokerCommission.When.COUNT) {
                        final int count = brokeringCommissionStatus.getTotal().getCount();
                        // Number of transactions exceeded, don't charge commission anymore
                        if (count >= maxCount) {
                            return null;
                        }
                        // This is the last transaction that generates the fee, so it's necessary to close the brokering commission status
                        if (count == (maxCount - 1) && !simulation) {
                            brokeringCommissionStatus = commissionService.closeBrokeringCommissionStatus(brokeringCommissionStatus);
                        }
                    }
                    if (when == BrokerCommission.When.DAYS) {
                        // Pays if the current day is not beyond max. day stored in the fee or default broker commission
                        final Calendar begin = brokeringCommissionStatus.getPeriod().getBegin();
                        final Calendar maxDay = new TimePeriod(maxCount, Field.DAYS).add(begin);
                        // Period expired, dont charge commission anymore
                        if (Calendar.getInstance().after(maxDay)) {
                            return null;
                        }
                    }
                }

                if (!simulation && !crossPayment && commissionContract == null) {
                    // Update total count and total amount of the brokering commission status
                    brokeringCommissionStatus.setTotal(brokeringCommissionStatus.getTotal().add(transferAmount));
                    commissionService.updateBrokeringCommissionStatus(brokeringCommissionStatus);
                }
            }
        }// END OF BROKER PART

        // Check if we found the from and to owner
        if (fromOwner == null || toOwner == null) {
            return null;
        }
        final LocalSettings localSettings = settingsService.getLocalSettings();

        // Get the accounts
        final Account fromAccount = accountService.getAccount(new AccountDTO(fromOwner, generated.getFrom()));
        final Account toAccount = accountService.getAccount(new AccountDTO(toOwner, generated.getTo()));

        // Calculate the fee amount
        BigDecimal amount = BigDecimal.ZERO;
        switch (feeChargeType) {
            case FIXED:
                amount = feeValue;
                break;
            case PERCENTAGE:
                amount = Amount.percentage(feeValue).apply(transferAmount);
                break;
            case A_RATE:
            case MIXED_A_D_RATES:
                if (aRateParam == null) {
                    feeValue = aRateService.getARatedFeePercentage(fee, fromAccount, date);
                } else {
                    feeValue = aRateService.getARatedFeePercentage(fee, aRateParam, dRateParam);
                }
                fee.setAmountForRates(Amount.percentage(feeValue));
                amount = Amount.percentage(feeValue).apply(transferAmount);
                break;
            case D_RATE:
                if (dRateParam == null) {
                    amount = dRateService.convertWithDRate(transferAmount, fromAccount, date);
                } else {
                    final Currency currency = fromAccount.getType().getCurrency();
                    final RatesDTO setOfUnits = RatesDTO.createSetOfUnitsForDRate(transferAmount, dRateParam, currency);
                    final BigDecimal result = dRateService.getDRateConversionResult(setOfUnits);
                    amount = transferAmount.subtract(result);
                }
                final MathContext mc = new MathContext(LocalSettings.BIG_DECIMAL_DIVISION_PRECISION);
                feeValue = amount.multiply(new BigDecimal(100)).divide(transferAmount, mc);
                fee.setAmountForRates(Amount.percentage(feeValue));
                break;
        }
        amount = localSettings.round(amount);

        // Get the description variables
        final String description = fee.getGeneratedTransferType().getDescription();
        final UnitsConverter unitsConverter = localSettings.getUnitsConverter(generated.getFrom().getCurrency().getPattern());
        final NumberConverter<BigDecimal> numberConverter = localSettings.getNumberConverter();
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put("amount", unitsConverter.toString(amount));
        values.put("transfer", unitsConverter.toString(transferAmount));
        values.put("original_amount", values.get("transfer")); // Aliasing to keep old transfer
        switch (feeChargeType) {
            case FIXED:
                values.put("fee", unitsConverter.toString(feeValue));
                break;
            case PERCENTAGE:
                values.put("fee", numberConverter.toString(feeValue) + "%");
                break;
            case A_RATE:
                values.put("fee", numberConverter.toString(feeValue) + "%");
                // be aware that this gets the fromAccount's rate, and not the transaction's rate (we don't have access to the transaction object in
                // this method). This is no problem as long as users cannot specify a rate different from their account's rate for a single
                // transaction. As soon as we allow this, the next line must be changed. Same counts for a few lines further.
                BigDecimal aRate = (aRateParam == null) ? aRateService.getActualRate(fromAccount, date) : aRateParam;
                aRate = localSettings.round(aRate);
                values.put("a_rate", aRate);
                break;
            case MIXED_A_D_RATES:
                values.put("fee", numberConverter.toString(feeValue) + "%");
                BigDecimal aRate1 = (aRateParam == null) ? aRateService.getActualRate(fromAccount, date) : aRateParam;
                aRate1 = localSettings.round(aRate1);
                values.put("a_rate", aRate1);
                // break was left out deliberately
            case D_RATE:
                BigDecimal dRate = (dRateParam == null) ? dRateService.getActualRate(fromAccount, date) : dRateParam;
                dRate = localSettings.round(dRate);
                values.put("d_rate", dRate);
                break;
        }
        values.put("fee_amount", values.get("fee")); // Aliasing to keep old transfer
        values.put("member", fromAccount.getOwnerName());

        // Build the transfer
        final Transfer feeTransfer = new Transfer();
        feeTransfer.setFrom(fromAccount);
        feeTransfer.setTo(toAccount);
        feeTransfer.setAmount(localSettings.round(amount));
        feeTransfer.setDescription(MessageProcessingHelper.processVariables(description, values));
        feeTransfer.setType(generated);
        feeTransfer.setTransactionFee(fee);
        if (commissionContract != null) {
            feeTransfer.setBrokerCommissionContract(commissionContract);
        }
        return feeTransfer;
    }

    public TransactionFee load(final Long id, final Relationship... fetch) {
        return transactionFeeDao.load(id, fetch);
    }

    public TransactionFeePreviewDTO preview(final AccountOwner from, final AccountOwner to, final TransferType transferType, final BigDecimal amount) {
        return preview(from, to, transferType, amount, null, null);
    }

    public TransactionFeePreviewDTO preview(AccountOwner from, final AccountOwner to, TransferType transferType, final BigDecimal amount, final BigDecimal aRate, final BigDecimal dRate) {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        if (from == null) {
            from = LoggedUser.accountOwner();
        }
        final Account fromAccount = accountService.getAccount(new AccountDTO(from, transferType.getFrom()));
        final Account toAccount = accountService.getAccount(new AccountDTO(to, transferType.getTo()));
        BigDecimal finalAmount = amount;
        final Map<TransactionFee, BigDecimal> map = new LinkedHashMap<TransactionFee, BigDecimal>();
        transferType = fetchService.fetch(transferType, TransferType.Relationships.TRANSACTION_FEES);
        final Collection<? extends TransactionFee> fees = transferType.getTransactionFees();
        final Calendar now = Calendar.getInstance();
        if (fees != null && !fees.isEmpty()) {
            // Search for enabled fees that the source pays
            for (final TransactionFee fee : fees) {
                // We just want fees the source member would pay
                if (!shouldPreviewFee(from, to, amount, fee)) {
                    continue;
                }
                // The buildTransfer() method returns a transfer if the fee should be applied or null
                final BuildTransferWithFeesDTO buildParams = new BuildTransferWithFeesDTO(now, fromAccount, toAccount, amount, fee, true);
                buildParams.setARate(aRate);
                buildParams.setDRate(dRate);
                final Transfer generatedTransfer = buildTransfer(buildParams);
                if (generatedTransfer != null) {
                    final BigDecimal feeAmount = generatedTransfer.getAmount();
                    map.put(fee, feeAmount);
                    // Check deducted amount
                    if (fee.isDeductAmount()) {
                        finalAmount = finalAmount.subtract(feeAmount);
                    }
                }
            }
        }
        final TransactionFeePreviewForRatesDTO result = new TransactionFeePreviewForRatesDTO();
        result.setFees(map);
        result.setFinalAmount(localSettings.round(finalAmount));
        result.setAmount(localSettings.round(amount));
        result.setARate(aRate);
        result.setDRate(dRate);
        return result;
    }

    public int remove(final Long... ids) {
        for (final Long id : ids) {
            final TransactionFee transactionFee = load(id);
            if (transactionFee instanceof BrokerCommission) {
                // Before removing the broker commission, check if it was already charged ...
                final BrokerCommission brokerCommission = (BrokerCommission) transactionFee;
                BrokeringCommissionStatusQuery query = new BrokeringCommissionStatusQuery();
                query.setBrokerCommission(brokerCommission);
                query.setAlreadyCharged(true);
                query.setPageForCount();
                List<BrokeringCommissionStatus> brokeringCommissionStatusList = brokeringCommissionStatusDao.search(query);
                // ... if it was already charged, it´s not possible to remove the broker commission, so throw an exception
                if (PageHelper.getTotalCount(brokeringCommissionStatusList) > 0) {
                    throw new DaoException();
                }
                // ... if it was not charged yet, remove the brokering commission status objects related to it
                query = new BrokeringCommissionStatusQuery();
                query.setBrokerCommission(brokerCommission);
                brokeringCommissionStatusList = brokeringCommissionStatusDao.search(query);
                for (final BrokeringCommissionStatus brokeringCommissionStatus : brokeringCommissionStatusList) {
                    brokeringCommissionStatusDao.delete(brokeringCommissionStatus.getId());
                }

                // Check if the commission was already customized by the broker ...
                DefaultBrokerCommissionQuery defaultBrokerCommissionQuery = new DefaultBrokerCommissionQuery();
                defaultBrokerCommissionQuery.setBrokerCommission(brokerCommission);
                defaultBrokerCommissionQuery.setSetByBroker(true);
                defaultBrokerCommissionQuery.setPageForCount();
                List<DefaultBrokerCommission> defaultBrokerCommissions = defaultBrokerCommissionDao.search(defaultBrokerCommissionQuery);
                // ... if it was already customized, it´s not possible to remove the broker commission, so throw an exception
                if (PageHelper.getTotalCount(defaultBrokerCommissions) > 0) {
                    throw new DaoException();
                }
                // ... if it was not customized yet, remove the default broker commissions related to it
                defaultBrokerCommissionQuery = new DefaultBrokerCommissionQuery();
                defaultBrokerCommissionQuery.setBrokerCommission(brokerCommission);
                defaultBrokerCommissions = defaultBrokerCommissionDao.search(defaultBrokerCommissionQuery);
                for (final DefaultBrokerCommission defaultBrokerCommission : defaultBrokerCommissions) {
                    defaultBrokerCommissionDao.delete(defaultBrokerCommission.getId());
                }
            }
        }
        return transactionFeeDao.delete(ids);
    }

    public BrokerCommission save(BrokerCommission brokerCommission) {
        preSave(brokerCommission);
        if (brokerCommission.isAllBrokerGroups()) {
            brokerCommission.setBrokerGroups(null);
        }

        validate(brokerCommission);

        if (brokerCommission.isTransient()) {
            brokerCommission = transactionFeeDao.insert(brokerCommission);
            if (brokerCommission.isFromMember()) {
                commissionService.createDefaultBrokerCommissions(brokerCommission);
            } else { // Broker commission paid by system
                commissionService.createBrokeringCommissionStatus(brokerCommission);
            }
        } else {
            final BrokerCommission savedBrokerCommission = transactionFeeDao.load(brokerCommission.getId(), BrokerCommission.Relationships.BROKER_GROUPS);
            fetchService.removeFromCache(savedBrokerCommission);
            brokerCommission = transactionFeeDao.update(brokerCommission);
            if (brokerCommission.isFromMember()) {
                commissionService.updateDefaultBrokerCommissions(brokerCommission, savedBrokerCommission);
            } else { // Broker commission paid by system
                commissionService.updateBrokeringCommissionStatus(brokerCommission, savedBrokerCommission);
            }
        }
        return brokerCommission;
    }

    public SimpleTransactionFee save(SimpleTransactionFee fee, final ARateRelation aRateRelation) {
        preSave(fee);

        // Ensure deduct amount is set for rates
        if (Arrays.asList(ChargeType.A_RATE, ChargeType.D_RATE, ChargeType.MIXED_A_D_RATES).contains(fee.getChargeType())) {
            fee.setDeductAmount(true);
        }

        validate(fee, aRateRelation);
        if (fee.getReceiver() != Subject.FIXED_MEMBER) {
            fee.setToFixedMember(null);
        }
        if (fee.isTransient()) {
            fee = transactionFeeDao.insert(fee);
        } else {
            fee = transactionFeeDao.update(fee);
        }
        return fee;
    }

    public List<TransactionFee> search(final TransactionFeeQuery query) {
        return transactionFeeDao.search(query);
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setaRateService(final ARateService aRateService) {
        this.aRateService = aRateService;
    }

    public void setBrokeringCommissionStatusDao(final BrokeringCommissionStatusDAO brokeringCommissionStatusDao) {
        this.brokeringCommissionStatusDao = brokeringCommissionStatusDao;
    }

    public void setBrokeringService(final BrokeringService brokeringService) {
        this.brokeringService = brokeringService;
    }

    public void setCommissionService(final CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    public void setDefaultBrokerCommissionDao(final DefaultBrokerCommissionDAO defaultBrokerCommissionDao) {
        this.defaultBrokerCommissionDao = defaultBrokerCommissionDao;
    }

    public void setdRateService(final DRateService dRateService) {
        this.dRateService = dRateService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setMessageResolver(final MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransactionFeeDao(final TransactionFeeDAO dao) {
        transactionFeeDao = dao;
    }

    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public void validate(final BrokerCommission brokerCommission) {
        getValidator(brokerCommission).validate(brokerCommission);
    }

    public void validate(final SimpleTransactionFee transactionFee, final ARateRelation aRateRelation) {
        getValidator(transactionFee, aRateRelation).validate(transactionFee);
    }

    /**
     * Checks whether the given fee will be shown on the preview
     */
    protected boolean shouldPreviewFee(final AccountOwner from, final AccountOwner to, final BigDecimal amount, final TransactionFee fee) {
        if (fee.getPayer() == Subject.SOURCE) {
            return true;
        } else if (fee.getPayer() == Subject.SYSTEM && (from instanceof SystemAccountOwner)) {
            return true;
        }
        return false;
    }

    /**
     * performs all initial tests for buildTransfer method
     * @return false when tests not passed.
     */
    private boolean doTests(final TransactionFee fee, final BigDecimal transferAmount, final Account from, final Account to) {
        // If fee is not enabled, does not generate the fee transfer
        if (!fee.isEnabled()) {
            return false;
        }

        // Test the amount range
        final BigDecimal initialAmount = fee.getInitialAmount();
        if (initialAmount != null && transferAmount.compareTo(initialAmount) < 0) {
            return false;
        }
        final BigDecimal finalAmount = fee.getFinalAmount();
        if (finalAmount != null && transferAmount.compareTo(finalAmount) > 0) {
            return false;
        }

        // Test from group
        final AccountOwner originalFromAccountOwner = from.getOwner();
        if (originalFromAccountOwner instanceof Member && !fee.isFromAllGroups()) {
            final Member fromMember = fetchService.fetch((Member) originalFromAccountOwner, Element.Relationships.GROUP);
            final MemberGroup fromGroup = fromMember.getMemberGroup();
            if (!fee.getFromGroups().contains(fromGroup)) {
                return false;
            }
        }
        // Test to group
        final AccountOwner originalToAccountOwner = to.getOwner();
        if (originalToAccountOwner instanceof Member && !fee.isToAllGroups()) {
            final Member toMember = fetchService.fetch((Member) originalToAccountOwner, Element.Relationships.GROUP);
            final MemberGroup toGroup = toMember.getMemberGroup();
            if (!fee.getToGroups().contains(toGroup)) {
                return false;
            }
        }
        return true;
    }

    private Validator getBasicValidator(final TransactionFee fee) {
        final Validator validator = new Validator("transactionFee");
        validator.property("name").required().maxLength(100);
        validator.property("description").maxLength(1000);
        validator.property("chargeType").required();
        final Property value = validator.property("value");
        if (fee.getChargeType() != null) {
            switch (fee.getChargeType()) {
                case PERCENTAGE:
                    value.required().positiveNonZero().lessEquals(100);
                    break;
                case FIXED:
                    value.required().positiveNonZero();
                    break;
            }
        }
        validator.property("originalTransferType").required();
        validator.property("generatedTransferType").required().anyOf(getPossibleGeneratedTransferTypes(fee));
        validator.property("payer").required();
        if (fee.getPayer() == Subject.FIXED_MEMBER) {
            validator.property("fromFixedMember").key("transactionFee.fromFixedMember.name").required();
        }
        return validator;
    }

    private AccountOwner getFromOwner(final TransactionFee fee, final AccountOwner originalFromAccountOwner, final Account to) {
        AccountOwner fromOwner = null;
        switch (fee.getPayer()) {
            case SYSTEM:
                fromOwner = SystemAccountOwner.instance();
                break;
            case SOURCE:
                fromOwner = originalFromAccountOwner;
                break;
            case DESTINATION:
                fromOwner = to.getOwner();
                break;
            case FIXED_MEMBER:
                fromOwner = fee.getFromFixedMember();
                break;
        }
        return fromOwner;
    }

    private List<TransferType> getPossibleGeneratedTransferTypes(final TransactionFee transactionFee) {
        final TransferTypeQuery ttQuery = new TransferTypeQuery();
        final TransferType original = fetchService.fetch(transactionFee.getOriginalTransferType(), TransferType.Relationships.FROM, TransferType.Relationships.TO);
        // Normally, the transfer type should be from the transaction fee from account.
        // However, some users requested that it could come out from another account of the same member.
        // So, we are just enforcing the nature here, not the specified account.
        final Subject payer = transactionFee.getPayer();
        if (payer == null) {
            return Collections.emptyList();
        }
        // Find the TT source
        switch (payer) {
            case SYSTEM:
                ttQuery.setFromNature(AccountType.Nature.SYSTEM);
                break;
            case SOURCE:
                ttQuery.setFromNature(original.getFrom().getNature());
                break;
            case DESTINATION:
                ttQuery.setFromNature(original.getTo().getNature());
                break;
        }
        // Find the TT destination
        switch (transactionFee.getNature()) {
            case SIMPLE:
                final SimpleTransactionFee simple = (SimpleTransactionFee) transactionFee;
                final Subject receiver = simple.getReceiver();
                if (receiver == null) {
                    return Collections.emptyList();
                }
                // Use the receiver subject
                switch (receiver) {
                    case SYSTEM:
                        ttQuery.setToNature(AccountType.Nature.SYSTEM);
                        break;
                    case SOURCE:
                        ttQuery.setToNature(original.getFrom().getNature());
                        break;
                    case DESTINATION:
                        ttQuery.setToNature(original.getTo().getNature());
                        break;
                }
                break;
            case BROKER:
                // Since the broker is always a member, use the nature
                ttQuery.setToNature(AccountType.Nature.MEMBER);
                break;
        }
        return transferTypeService.search(ttQuery);
    }

    private AccountOwner getToOwner(final TransactionFee fee, final AccountOwner originalFromAccountOwner, final Account to) {
        final SimpleTransactionFee simpleTransactionFee = (SimpleTransactionFee) fee;
        AccountOwner toOwner = null;
        switch (simpleTransactionFee.getReceiver()) {
            case SYSTEM:
                toOwner = SystemAccountOwner.instance();
                break;
            case SOURCE:
                toOwner = originalFromAccountOwner;
                break;
            case DESTINATION:
                toOwner = to.getOwner();
                break;
            case FIXED_MEMBER:
                toOwner = simpleTransactionFee.getToFixedMember();
                break;
        }
        return toOwner;
    }

    private Validator getValidator(final BrokerCommission commission) {
        final Validator validator = getBasicValidator(commission);
        validator.property("maxFixedValue").positiveNonZero();
        validator.property("maxPercentageValue").positiveNonZero();
        validator.property("when").required();
        validator.property("count").add(new CountValidation());
        validator.property("chargeType").anyOf(ChargeType.FIXED, ChargeType.PERCENTAGE);
        if (commission.getInitialAmount() != null && commission.getFinalAmount() != null) {
            final Property initialAmount = validator.property("initialAmount");
            final BigDecimal finalAmount = commission.getFinalAmount();
            initialAmount.comparable(finalAmount, "<=", new ValidationError("errors.greaterThan", messageResolver.message("transactionFee.finalAmount")));
        }
        return validator;
    }

    private Validator getValidator(final SimpleTransactionFee fee, final ARateRelation arateRelation) {
        final TransferType originalTransferType = fetchService.fetch(fee.getOriginalTransferType(), RelationshipHelper.nested(TransferType.Relationships.FROM, AccountType.Relationships.CURRENCY));

        Validator validator = getBasicValidator(fee);

        validator.property("receiver").required();
        if (fee.getReceiver() == Subject.FIXED_MEMBER) {
            validator.property("toFixedMember").key("transactionFee.toFixedMember.name").required();
        }
        validator.general(new PayerAndReceiverValidation());

        final Collection<ChargeType> allowedChargeTypes = EnumSet.allOf(ChargeType.class);
        final Currency currency = originalTransferType.getCurrency();
        if (!currency.isEnableARate()) {
            allowedChargeTypes.remove(ChargeType.A_RATE);
            allowedChargeTypes.remove(ChargeType.MIXED_A_D_RATES);
        }
        if (!currency.isEnableDRate()) {
            allowedChargeTypes.remove(ChargeType.D_RATE);
            allowedChargeTypes.remove(ChargeType.MIXED_A_D_RATES);
        }
        // put the fee in a dto in order to pass it to the validator for A-rate fields
        final ARatedFeeDTO dto = new ARatedFeeDTO(fee);
        validator = aRateService.applyARateFieldsValidation(validator, dto, arateRelation);

        validator.property("chargeType").anyOf(allowedChargeTypes);

        if (fee.getInitialAmount() != null && fee.getFinalAmount() != null) {
            final Property initialAmount = validator.property("initialAmount");
            final BigDecimal finalAmount = fee.getFinalAmount();
            initialAmount.comparable(finalAmount, "<=", new ValidationError("errors.greaterThan", messageResolver.message("transactionFee.finalAmount")));
        }

        return validator;
    }

    private void preSave(final TransactionFee transactionFee) {
        if (transactionFee.isFromAllGroups()) {
            transactionFee.setFromGroups(null);
        }
        if (transactionFee.isToAllGroups()) {
            transactionFee.setToGroups(null);
        }
        if (transactionFee.getPayer() != Subject.FIXED_MEMBER) {
            transactionFee.setFromFixedMember(null);
        }
    }

}