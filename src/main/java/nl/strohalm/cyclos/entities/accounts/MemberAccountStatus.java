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
package nl.strohalm.cyclos.entities.accounts;

import static nl.strohalm.cyclos.utils.BigDecimalHelper.nvl;

import java.math.BigDecimal;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;

/**
 * AccountStatus for member accounts
 * @author luis
 */
public class MemberAccountStatus extends AccountStatus {

    public static enum Relationships implements Relationship {
        ACCOUNT_FEE_LOG("accountFeeLog");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long serialVersionUID  = -8956407946976328529L;
    private BigDecimal        volumeAccountFees = BigDecimal.ZERO;
    private AccountFeeLog     accountFeeLog;

    public MemberAccountStatus() {
    }

    public MemberAccountStatus(final MemberAccount account) {
        super(account);
    }

    public AccountFeeLog getAccountFeeLog() {
        return accountFeeLog;
    }

    public MemberAccount getMemberAccount() {
        return (MemberAccount) getAccount();
    }

    @Override
    public BigDecimal getReservedAmount() {
        return super.getReservedAmount().add(nvl(volumeAccountFees));
    }

    public BigDecimal getVolumeAccountFees() {
        return volumeAccountFees;
    }

    @Override
    public MemberAccountStatus newBasedOnThis() {
        final MemberAccountStatus status = (MemberAccountStatus) super.newBasedOnThis();
        status.volumeAccountFees = volumeAccountFees;
        return status;
    }

    public void setAccountFeeLog(final AccountFeeLog accountFeeLog) {
        this.accountFeeLog = accountFeeLog;
    }

    public void setVolumeAccountFees(final BigDecimal volumeAccountFees) {
        if (volumeAccountFees == null || volumeAccountFees.compareTo(BigDecimal.ZERO) < 0) {
            this.volumeAccountFees = BigDecimal.ZERO;
        } else {
            this.volumeAccountFees = volumeAccountFees;
        }
    }

}
