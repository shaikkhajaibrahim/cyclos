/*
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Cyclos is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. �See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Cyclos; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA �02111-1307 �USA
  
 */
package nl.strohalm.cyclos.utils.logging;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee.PaymentDirection;
import nl.strohalm.cyclos.entities.accounts.pos.Pos;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.entities.services.ServiceOperation;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.LogSettings;
import nl.strohalm.cyclos.entities.settings.LogSettings.AccountFeeLevel;
import nl.strohalm.cyclos.entities.settings.LogSettings.ScheduledTaskLevel;
import nl.strohalm.cyclos.entities.settings.LogSettings.TraceLevel;
import nl.strohalm.cyclos.entities.settings.LogSettings.TransactionLevel;
import nl.strohalm.cyclos.entities.settings.events.LogSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LogSettingsEvent;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.ClassHelper;
import nl.strohalm.cyclos.utils.FileUnits;
import nl.strohalm.cyclos.utils.FormatObject;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.conversion.UnitsConverter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains methods for logging actions
 * @author luis
 */
public class LoggingHandler implements LogSettingsChangeListener {

    private static final Log LOG = LogFactory.getLog(LoggingHandler.class);

    private Logger           traceLogger;
    private Logger           transactionLogger;
    private Logger           accountFeeLogger;
    private Logger           scheduledTaskLogger;

    private LogFormatter     logFormatter;
    private SettingsService  settingsService;
    private FetchService     fetchService;

    /**
     * Returns if at least {@link AccountFeeLevel#ERRORS} level is enabled
     */
    public boolean isAccountFeeEnabled() {
        return settingsService.getLogSettings().getAccountFeeLevel() != AccountFeeLevel.OFF;
    }

    /**
     * Returns if at least {@link ScheduledTaskLevel#ERRORS} level is enabled
     */
    public boolean isSchedulingEnabled() {
        return settingsService.getLogSettings().getScheduledTaskLevel() != ScheduledTaskLevel.OFF;
    }

    /**
     * Returns if at least {@link TraceLevel#ERRORS} level is enabled
     */
    public boolean isTraceEnabled() {
        return settingsService.getLogSettings().getTraceLevel() != TraceLevel.OFF;
    }

    /**
     * Returns if at least {@link TransactionLevel#NORMAL} level is enabled
     */
    public boolean isTransactionEnabled() {
        return settingsService.getLogSettings().getTransactionLevel() != TransactionLevel.OFF;
    }

    /**
     * Log a tax error
     */
    public void logAccountFeeError(final AccountFeeLog feeLog, final Throwable error) {
        final Logger logger = getAccountFeeLogger();
        final Level level = AccountFeeLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            try {
                logger.log(level, "Error on " + feeLog.getAccountFee().getName(), error);
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getAccountFeeFile());
            }
        }
    }

    /**
     * Log an account fee transfer
     */
    public void logAccountFeeInvoice(final Invoice invoice) {
        final Logger logger = getAccountFeeLogger();
        final Level level = AccountFeeLevel.DETAILED.getLevel();
        if (logger.isLoggable(level)) {
            final UnitsConverter unitsConverter = settingsService.getLocalSettings().getUnitsConverter(invoice.getTransferType().getFrom().getCurrency().getPattern());
            final String message = "Sent invoice of %s from %s";
            final Object[] params = { unitsConverter.toString(invoice.getAmount()), invoice.getToMember().getUsername() };
            try {
                logger.log(level, String.format(message, params));
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getAccountFeeFile());
            }
        }
    }

    /**
     * Log an account fee transfer
     */
    public void logAccountFeePayment(final Transfer transfer) {
        final Logger logger = getAccountFeeLogger();
        final Level level = AccountFeeLevel.DETAILED.getLevel();
        if (logger.isLoggable(level)) {
            final AccountFeeLog feeLog = transfer.getAccountFeeLog();
            final AccountFee fee = feeLog.getAccountFee();
            final UnitsConverter unitsConverter = settingsService.getLocalSettings().getUnitsConverter(transfer.getFrom().getType().getCurrency().getPattern());
            String message;
            Object[] params;
            if (fee.getPaymentDirection() == PaymentDirection.TO_SYSTEM) {
                message = "Charged %s from %s";
                params = new Object[] { unitsConverter.toString(transfer.getAmount()), transfer.getFrom().getOwnerName() };
            } else {
                message = "Paid %s to %s";
                params = new Object[] { unitsConverter.toString(transfer.getAmount()), transfer.getTo().getOwnerName() };
            }
            try {
                logger.log(level, String.format(message, params));
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getAccountFeeFile());
            }
        }
    }

    /**
     * Log a tax status change
     */
    public void logAccountFeeStatus(final AccountFeeLog feeLog) {
        final Logger logger = getAccountFeeLogger();
        final Level level = AccountFeeLevel.STATUS.getLevel();
        if (logger.isLoggable(level)) {
            String status = null;
            switch (feeLog.getStatus()) {
                case RUNNING:
                    status = "Started";
                    break;
                case CANCELED:
                    status = "Manually canceled";
                    break;
                case NEVER_RAN:
                    status = "Never ran";
                    break;
                case PARTIALLY_FAILED:
                    status = "Partially failed";
                    break;
                case FINISHED:
                    status = "Finished";
                    break;
            }
            try {
                logger.log(level, feeLog.getAccountFee().getName() + ": " + status);
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getAccountFeeFile());
            }
        }
    }

    /**
     * Log a permission denied on action execution
     */
    public void logPermissionDenied(final User user, final Method method, final Object[] args) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            try {
                logger.log(level, "Permission denied for " + buildActionString(user, null, method, args, null, false));
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getTraceFile());
            }
        }
    }

    /**
     * Logs that a request has been rejected because the account status queue is full
     */
    public void logRequestRejectedOnSystemOverloaded(final String uri, final String remoteAddress) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            try {
                logger.log(level, "A request to " + uri + " from " + remoteAddress + " was rejected because the system is overloaded");
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getTraceFile());
            }
        }
    }

    /**
     * Log a scheduled task error
     */
    public void logScheduledTaskError(final String taskName, final Exception error) {
        final Logger logger = getScheduledTaskLogger();
        final Level level = ScheduledTaskLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            try {
                logger.log(level, "Exception on scheduled task: " + taskName, error);
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getScheduledTaskFile());
            }
        }
    }

    /**
     * Logs a scheduled task execution
     * @param taskName The task name
     * @param time The milliseconds the task took to run
     */
    public void logScheduledTaskTrace(final String taskName, final long time) {
        final Logger logger = getScheduledTaskLogger();
        final Level level = ScheduledTaskLevel.DETAILED.getLevel();
        if (logger.isLoggable(level)) {
            final MathContext mathContext = settingsService.getLocalSettings().getMathContext();
            final String formattedTime = settingsService.getLocalSettings().getNumberConverter().toString(new BigDecimal(time).divide(new BigDecimal(1000), mathContext));
            try {
                logger.log(level, String.format("Scheduled task '%s' ran on %s seconds", taskName, formattedTime));
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getScheduledTaskFile());
            }
        }
    }

    /**
     * Logs an scheduling group execution
     * @param time The milliseconds the scheduling took to run
     */
    public void logSchedulingTrace(final long time) {
        final Logger logger = getScheduledTaskLogger();
        final Level level = ScheduledTaskLevel.INFO.getLevel();
        if (logger.isLoggable(level)) {
            final MathContext mathContext = settingsService.getLocalSettings().getMathContext();
            final String formattedTime = settingsService.getLocalSettings().getNumberConverter().toString(new BigDecimal(time).divide(new BigDecimal(1000), mathContext));
            try {
                logger.log(level, String.format("Scheduled tasks ran on %s seconds", formattedTime));
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getScheduledTaskFile());
            }
        }
    }

    /**
     * Log a successful transfer
     */
    public void logTransfer(Transfer transfer) {
        final Logger logger = getTransactionLogger();
        final Level detailed = TransactionLevel.DETAILED.getLevel();
        final Level normal = TransactionLevel.NORMAL.getLevel();
        final boolean detailedLoggable = logger.isLoggable(detailed);
        final boolean normalLoggable = logger.isLoggable(normal);
        final boolean willLog = detailedLoggable || normalLoggable;
        // Generate log if, at least, normal level is enabled
        if (willLog) {
            transfer = fetchService.fetch(transfer, RelationshipHelper.nested(Transfer.Relationships.FROM, Account.Relationships.TYPE, AccountType.Relationships.CURRENCY), Transfer.Relationships.TO);
            Level level;
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final UnitsConverter unitsConverter = localSettings.getUnitsConverter(transfer.getFrom().getType().getCurrency().getPattern());
            String message;
            Object[] args;
            // Get the specific level arguments
            if (detailedLoggable) {
                final TransferType type = transfer.getType();
                level = detailed;
                message = "id: %s, date: %s, type: %s (%s), amount: %s, from: %s, to: %s, by: %s, tx#: %s, description: %s";
                final Element by = transfer.getBy();
                args = new Object[] { transfer.getId(), localSettings.getDateTimeConverter().toString(transfer.getDate()), type.getId(), type.getName(), unitsConverter.toString(transfer.getAmount()), transfer.getFrom().getOwnerName(), transfer.getTo().getOwnerName(), by == null ? "<null>" : by.getUsername(), StringUtils.defaultIfEmpty(transfer.getTransactionNumber(), "<null>"), StringUtils.replace(transfer.getDescription(), "\n", "\\n") };
            } else {
                level = normal;
                message = "id: %s, amount: %s, from: %s, to: %s";
                args = new Object[] { transfer.getId(), unitsConverter.toString(transfer.getAmount()), transfer.getFrom().getOwnerName(), transfer.getTo().getOwnerName() };
            }
            try {
                logger.log(level, String.format(message, args));
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getTransactionFile());
            }
        }
    }

    public synchronized void onLogSettingsUpdate(final LogSettingsEvent event) {
        // Invalidate the loggers, forcing them to be recreated on the next time
        close(traceLogger);
        traceLogger = null;
        close(transactionLogger);
        transactionLogger = null;
        close(accountFeeLogger);
        accountFeeLogger = null;
        close(scheduledTaskLogger);
        scheduledTaskLogger = null;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setLogFormatter(final LogFormatter logFormatter) {
        this.logFormatter = logFormatter;
    }

    public void setSettingsService(final SettingsService service) {
        settingsService = service;
        service.addListener(this);
    }

    /**
     * Logs an action trace
     */
    public void trace(final String remoteAddress, final User user, final Permission permission, final Method method, final Object[] args, final Object retVal) {
        final Logger logger = getTraceLogger();
        final Level detailed = TraceLevel.DETAILED.getLevel();
        final Level normal = TraceLevel.SIMPLE.getLevel();
        final boolean detailedLoggable = logger.isLoggable(detailed);
        final boolean normalLoggable = logger.isLoggable(normal);
        final Level logLevel = detailedLoggable ? detailed : normalLoggable ? normal : null;
        if (logLevel != null) {
            final String prefix = StringUtils.isEmpty(remoteAddress) ? "" : remoteAddress + " - ";
            final String message = buildActionString(user, permission, method, args, retVal, true);
            try {
                logger.log(logLevel, prefix + message);
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getTraceFile());
            }
        }
    }

    /**
     * Logs an action error
     */
    public void traceError(final User user, final Permission permission, final Method method, final Object[] args, final Throwable t) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            try {
                logger.log(level, "Error on " + buildActionString(user, permission, method, args, null, false), t);
            } catch (final Exception e) {
                System.out.println("Error generating error log on " + settingsService.getLogSettings().getTraceFile());
            }
        }
    }

    /**
     * Logs an user login
     */
    public void traceLogin(final String remoteAddress, final User user, final String sessionId) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.SIMPLE.getLevel();
        if (logger.isLoggable(level)) {
            try {
                logger.log(level, remoteAddress + " - Login for " + user + " under session id " + sessionId);
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getTraceFile());
            }
        }
    }

    /**
     * Logs an user logout
     */
    public void traceLogout(final String remoteAddress, final User user, final String sessionId) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.SIMPLE.getLevel();
        if (logger.isLoggable(level)) {
            try {
                logger.log(level, remoteAddress + " - Logout for " + user + " under session id " + sessionId);
            } catch (final Exception e) {
                System.out.println("Error generating log on " + settingsService.getLogSettings().getTraceFile());
            }
        }
    }

    public void traceWebService(final String message) {
        final Logger logger = getTraceLogger();
        final Level logLevel = getLogLevel(logger);
        if (logLevel != null) {
            log(logger, logLevel, settingsService.getLogSettings().getTraceFile(), "Unknown IP", message);
        }
    }

    public void traceWebService(final String remoteAddress, final Pos pos, final String methodName, final Map<String, Object> parameters) {
        traceWebService(remoteAddress, pos, methodName, parameters, null);
    }

    public void traceWebService(final String remoteAddress, final Pos pos, final String methodName, final String message) {
        traceWebService(remoteAddress, pos, methodName, null, message);
    }

    public void traceWebService(final String remoteAddress, final ServiceClient serviceClient, final String methodName, final Map<String, Object> parameters) {
        traceWebService(remoteAddress, serviceClient, methodName, parameters, null);
    }

    public void traceWebService(final String remoteAddress, final ServiceClient serviceClient, final String methodName, final String message) {
        traceWebService(remoteAddress, serviceClient, methodName, null, message);
    }

    public void traceWebServiceError(final String error) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            error(logger, level, settingsService.getLogSettings().getTraceFile(), "Unknown IP", error, null);
        }
    }

    public void traceWebServiceError(final String remoteAddress, final Pos pos, final String methodName, final Map<String, Object> parameters, final Throwable th) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            error(logger, level, settingsService.getLogSettings().getTraceFile(), remoteAddress, buildWebServiceActionString(pos, methodName, parameters), th);
        }
    }

    public void traceWebServiceError(final String remoteAddress, final ServiceClient serviceClient, final String methodName, final Map<String, Object> parameters, final Throwable th) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            error(logger, level, settingsService.getLogSettings().getTraceFile(), remoteAddress, buildWebServiceActionString(serviceClient, methodName, parameters), th);
        }
    }

    public void traceWebServiceError(final Throwable th) {
        final Logger logger = getTraceLogger();
        final Level level = TraceLevel.ERRORS.getLevel();
        if (logger.isLoggable(level)) {
            error(logger, level, settingsService.getLogSettings().getTraceFile(), "Unknown IP", "Unknown method", th);
        }
    }

    private LoggingHandler append(final StringBuilder builder, final Pos pos) {
        builder.append("PosId: ");
        if (pos != null) {
            builder.append(pos.getPosId());
            if (pos.getMemberPos() != null) {
                builder.append(", PosName: ");
                builder.append(pos.getMemberPos().getPosName());
                builder.append(", Status: ");
                builder.append(pos.getMemberPos().getStatus());
                builder.append(", Restricted Member: ");
                builder.append(pos.getMemberPos().getMember().getUsername());
            } else {
                builder.append(", MemberPos: null");
            }
        } else {
            builder.append(" null ");
        }

        return this;
    }

    private LoggingHandler append(final StringBuilder builder, final ServiceClient serviceClient) {
        builder.append("Web Service Client: ");
        if (serviceClient != null) {
            builder.append(serviceClient.getName()).append(", Host Name: ").append(serviceClient.getHostname());
            builder.append(", Channel: ");
            builder.append(serviceClient.getChannel() == null ? " null " : serviceClient.getChannel().getInternalName());
            builder.append(", Permissions [");
            final Iterator<ServiceOperation> it = serviceClient.getPermissions().iterator();
            while (it.hasNext()) {
                builder.append(it.next().name()).append(", ");
            }
            if (!serviceClient.getPermissions().isEmpty()) {
                builder.delete(builder.length() - 2, builder.length()); // removes the last ', '
            }
            builder.append("], Restricted User: ");
            builder.append(serviceClient.getMember() == null ? " null " : serviceClient.getMember().getUsername());
        } else {
            builder.append(" null ");
        }

        return this;
    }

    private StringBuilder append(final StringBuilder builder, final String methodName, final Map<String, Object> parameters) {
        builder.append(", Operation: ").append(methodName + "(");

        if (parameters != null) {
            for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
                builder.append(entry.getKey());
                builder.append("=");
                builder.append(entry.getValue() == null ? " null" : entry.getValue());
            }
        }
        builder.append(")");
        return builder;
    }

    /**
     * Builds an action string for the method execution. Will be something like userName.methodName if not detailed, or userName.methodName([param1,
     * param2, ...]) if detailed
     */
    private String buildActionString(final User user, final Permission permission, final Method method, final Object[] args, final Object retVal, final boolean useReturn) {
        final StringBuilder sb = new StringBuilder();
        final String className = StringUtils.replace(ClassHelper.getClassName(method.getDeclaringClass()), "Impl", "");
        final String methodName = method.getName();
        sb.append(user.getUsername()).append(" - ").append(className).append('.').append(methodName);
        if (permission != null) {
            sb.append('[').append(permission.module()).append('.').append(permission.operation()).append(']');
        }
        final boolean detailed = getTraceLogger().isLoggable(TraceLevel.DETAILED.getLevel());
        if (detailed) {
            // Don't log the parameters of some methods
            final boolean logParameters = logParameters(methodName);
            if (logParameters && useReturn) {
                sb.append(", Returning ").append(FormatObject.formatArgument(retVal)).append(", Arguments ");
            }
            // Append the arguments
            sb.append('(');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (logParameters) {
                    final Object argument = args[i];
                    if (!fetchService.isInitialized(argument)) {
                        sb.append(FormatObject.formatArgument(argument));
                    } else {
                        sb.append(FormatObject.formatVO(argument));
                    }
                } else {
                    sb.append("***");
                }
            }
            sb.append(')');
        }
        return sb.toString();
    }

    private String buildWebServiceActionString(final Pos pos, final String methodName, final Map<String, Object> parameters) {
        final StringBuilder builder = new StringBuilder("");
        return append(builder, pos).append(builder, methodName, parameters).toString();
    }

    private String buildWebServiceActionString(final ServiceClient serviceClient, final String methodName, final Map<String, Object> parameters) {
        final StringBuilder builder = new StringBuilder("");
        return append(builder, serviceClient).append(builder, methodName, parameters).toString();
    }

    /**
     * Closes all handlers for the given logger
     * @param logger
     */
    private void close(final Logger logger) {
        if (logger == null) {
            return;
        }
        for (final Handler handler : logger.getHandlers()) {
            try {
                handler.close();
            } catch (final Exception e) {
                LOG.warn("Error while closing log handler - Ignoring", e);
            }
        }
    }

    private void error(final Logger logger, final Level logLevel, final String logFile, final String remoteAddress, final String message, final Throwable th) {
        if (logLevel != null) {
            try {
                final String prefix = StringUtils.isEmpty(remoteAddress) ? "" : remoteAddress + " - ";
                logger.log(logLevel, prefix + message, th);
            } catch (final Exception e) {
                System.out.printf("Error generating error log on %1$s: %2$s%n", logFile, e.getMessage());
            }
        }
    }

    private Logger getAccountFeeLogger() {
        if (accountFeeLogger == null) {
            final LogSettings logSettings = settingsService.getLogSettings();
            accountFeeLogger = init(logSettings.getAccountFeeLevel().getLevel(), logSettings.getAccountFeeFile());
        }
        return accountFeeLogger;
    }

    private Level getLogLevel(final Logger logger) {
        final Level detailed = TraceLevel.DETAILED.getLevel();
        final Level normal = TraceLevel.SIMPLE.getLevel();
        final boolean detailedLoggable = logger.isLoggable(detailed);
        final boolean normalLoggable = logger.isLoggable(normal);

        return detailedLoggable ? detailed : normalLoggable ? normal : null;
    }

    private Logger getScheduledTaskLogger() {
        if (scheduledTaskLogger == null) {
            final LogSettings logSettings = settingsService.getLogSettings();
            scheduledTaskLogger = init(logSettings.getScheduledTaskLevel().getLevel(), logSettings.getScheduledTaskFile());
        }
        return scheduledTaskLogger;
    }

    private Logger getTraceLogger() {
        if (traceLogger == null) {
            final LogSettings logSettings = settingsService.getLogSettings();
            traceLogger = init(logSettings.getTraceLevel().getLevel(), logSettings.getTraceFile());
        }
        return traceLogger;
    }

    private Logger getTransactionLogger() {
        if (transactionLogger == null) {
            final LogSettings logSettings = settingsService.getLogSettings();
            transactionLogger = init(logSettings.getTransactionLevel().getLevel(), logSettings.getTransactionFile());
        }
        return transactionLogger;
    }

    /**
     * Creates a new logger
     */
    private Logger init(final Level level, final String file) {
        final LogSettings logSettings = settingsService.getLogSettings();
        final Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(level);
        logger.setUseParentHandlers(false);
        try {
            final FileUnits units = logSettings.getMaxLengthPerFileUnits();
            final FileHandler fileHandler = new FileHandler(file, units.calculate(logSettings.getMaxLengthPerFile()), logSettings.getMaxFilesPerLog(), true);
            fileHandler.setFormatter(logFormatter);
            fileHandler.setEncoding(settingsService.getLocalSettings().getCharset());
            logger.addHandler(fileHandler);
        } catch (final Exception e) {
            final ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(logFormatter);
            try {
                consoleHandler.setEncoding(settingsService.getLocalSettings().getCharset());
            } catch (final Exception e1) {
                // Just ignore
            }
            logger.addHandler(consoleHandler);
            logger.log(Level.WARNING, "Unable to create logger for file " + file);
        }
        return logger;
    }

    private void log(final Logger logger, final Level logLevel, final String logFile, final String remoteAddress, final String message) {
        if (logLevel != null) {
            try {
                final String prefix = StringUtils.isEmpty(remoteAddress) ? "" : remoteAddress + " - ";
                logger.log(logLevel, prefix + message);
            } catch (final Exception e) {
                System.out.printf("Error generating log on %1$s: %2$s%n", logFile, e.getMessage());
            }
        }
    }

    /**
     * Check if parameters will be logged. Methods like changePassword won't be.
     */
    private boolean logParameters(final String name) {
        if (name.startsWith("change") && name.endsWith("Password")) {
            return false;
        }
        return true;
    }

    private void traceWebService(final String remoteAddress, final Pos pos, final String methodName, final Map<String, Object> parameters, String message) {
        final Logger logger = getTraceLogger();
        final Level logLevel = getLogLevel(logger);
        if (logLevel != null) {
            final String msg = buildWebServiceActionString(pos, methodName, parameters);
            message = message == null ? msg : msg + ": " + message;
            log(logger, logLevel, settingsService.getLogSettings().getTraceFile(), remoteAddress, msg);
        }
    }

    private void traceWebService(final String remoteAddress, final ServiceClient serviceClient, final String methodName, final Map<String, Object> parameters, String message) {
        final Logger logger = getTraceLogger();
        final Level logLevel = getLogLevel(logger);
        if (logLevel != null) {
            final String msg = buildWebServiceActionString(serviceClient, methodName, parameters);
            message = message == null ? msg : msg + ": " + message;
            log(logger, logLevel, settingsService.getLogSettings().getTraceFile(), remoteAddress, message);
        }
    }
}