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
package mp.platform.cyclone.webservices.model;

import java.math.BigDecimal;
import java.util.*;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents a transfer from the point of view of an account (no absolute from / to, but the amount is negative for debits and positive for credits)
 * 
 * @author luis
 */
@XmlType(name = "transfer")
public class AccountHistoryTransferVO extends WebServicesEntityVO {
    private static final long   serialVersionUID = 1739114447726519996L;
    private TransferTypeVO      transferType;
    private Calendar            date;
    private String              formattedDate;
    private Calendar            processDate;
    private String              formattedProcessDate;
    private BigDecimal          amount;
    private String              formattedAmount;
    // this field is set only for a non-restricted member
    private MemberVO            fromMember;
    // this field is the related (other side) member for a restricted member
    private MemberVO            member;
    // this field is set only for a non-restricted member
    private String              fromSystemAccountName;
    // this field is the related (other side) system account for a restricted member
    private String              systemAccountName;

    private String              transactionNumber;

    private String              traceNumber;

    private String              description;

    @XmlTransient
    private Map<String, String> fieldsMap;
    private List<FieldValueVO>  fields;
    
    private List<TransferFeeVO> transferFees = new ArrayList<TransferFeeVO>();

    public BigDecimal getAmount() {
        return amount;
    }

    public Calendar getDate() {
        return date;
    }

    public List<FieldValueVO> getFields() {
        return fields;
    }

    public Map<String, String> getFieldsMap() {
        if (fieldsMap == null) {
            if (fields != null) {
                fieldsMap = new HashMap<String, String>();
                for (final FieldValueVO vo : fields) {
                    fieldsMap.put(vo.getField(), vo.getValue());
                }
            } else {
                fieldsMap = Collections.emptyMap();
            }
        }
        return fieldsMap;
    }

    public String getFormattedAmount() {
        return formattedAmount;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public String getFormattedProcessDate() {
        return formattedProcessDate;
    }

    public MemberVO getFromMember() {
        return fromMember;
    }

    public String getFromSystemAccountName() {
        return fromSystemAccountName;
    }

    public MemberVO getMember() {
        return member;
    }

    public Calendar getProcessDate() {
        return processDate;
    }

    public String getSystemAccountName() {
        return systemAccountName;
    }

    public String getTraceNumber() {
        return traceNumber;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public TransferTypeVO getTransferType() {
        return transferType;
    }

    public String getDescription() {
        return description;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setFields(final List<FieldValueVO> fields) {
        this.fields = fields;
    }

    public void setFormattedAmount(final String formattedAmount) {
        this.formattedAmount = formattedAmount;
    }

    public void setFormattedDate(final String formattedDate) {
        this.formattedDate = formattedDate;
    }

    public void setFormattedProcessDate(final String formattedProcessDate) {
        this.formattedProcessDate = formattedProcessDate;
    }

    public void setFromMember(final MemberVO fromMember) {
        this.fromMember = fromMember;
    }

    public void setFromSystemAccountName(final String fromSystemAccountName) {
        this.fromSystemAccountName = fromSystemAccountName;
    }

    public void setMember(final MemberVO member) {
        this.member = member;
    }

    public void setProcessDate(final Calendar processDate) {
        this.processDate = processDate;
    }

    public void setSystemAccountName(final String systemAccountName) {
        this.systemAccountName = systemAccountName;
    }

    public void setTraceNumber(final String traceNumber) {
        this.traceNumber = traceNumber;
    }

    public void setTransactionNumber(final String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public void setTransferType(final TransferTypeVO transferType) {
        this.transferType = transferType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TransferFeeVO> getTransferFees() {
        return transferFees;
    }

    public void setTransferFees(List<TransferFeeVO> transferFees) {
        this.transferFees = transferFees;
    }

    @Override
    public String toString() {
        if (fromMember != null || fromSystemAccountName != null) {
            return "AccountHistoryTransferVO [amount=" + amount + ", fields=" + fields + ", formattedAmount=" + formattedAmount + ", formattedDate=" + formattedDate + ", formattedProcessDate=" + formattedProcessDate + ", fromMember=" + fromMember + ", toMember=" + member + ", fromSystemAccountName=" + fromSystemAccountName + ", toSystemAccountName=" + systemAccountName + ", traceNumber=" + traceNumber + ", transactionNumber=" + transactionNumber + ", transferType=" + transferType + ", description=" + description + "]";
        } else {
            return "AccountHistoryTransferVO [amount=" + amount + ", fields=" + fields + ", formattedAmount=" + formattedAmount + ", formattedDate=" + formattedDate + ", formattedProcessDate=" + formattedProcessDate + ", member=" + member + ", systemAccountName=" + systemAccountName + ", traceNumber=" + traceNumber + ", transactionNumber=" + transactionNumber + ", transferType=" + transferType + ", description=" + description + "]";
        }
    }

}
