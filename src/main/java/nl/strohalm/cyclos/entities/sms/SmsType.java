package nl.strohalm.cyclos.entities.sms;

import nl.strohalm.cyclos.utils.StringValuedEnum;

/**
 * Possible reasons for an sms to be sent by external channels
 * 
 * @author luis
 */
public enum SmsType implements StringValuedEnum {

    /** A sms originated by the Controller when processing a request payment */
    REQUEST_PAYMENT("RP"),

    /** A sms originated by the Controller when processing a request payment but there is an error to notify */
    REQUEST_PAYMENT_ERROR("RPE"),

    /** A sms originated by the Controller when processing a payment */
    PAYMENT("P"),

    /** A Sms originated by the Controller when processing a payment but there is an error to notify */
    PAYMENT_ERROR("PE"),

    /** A Sms originated by the Controller when processing an account details */
    ACCOUNT_DETAILS("AD"),

    /** A Sms originated by the Controller when processing an account details but there is an error to notify */
    ACCOUNT_DETAILS_ERROR("ADE"),

    /** A Sms originated by the Controller when processing a help command */
    HELP("H"),

    /** A Sms originated by the Controller when processing a help command but there is an error to notify */
    HELP_ERROR("HE"),

    /** A Sms originated by the Controller when processing an info text */
    INFO_TEXT("IT"),

    /** A Sms originated by the Controller when processing an info text but there is an error to notify */
    INFO_TEXT_ERROR("ITE"),

    /** A sms originated by the Controller when requesting a command confirmation */
    OPERATION_CONFIRMATION("CC"),

    /** A general notification */
    GENERAL("G");

    private final String value;

    private SmsType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}