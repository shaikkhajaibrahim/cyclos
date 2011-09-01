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
package nl.strohalm.cyclos.webservices.pos;

/**
 * Parameters for retrieving an account history
 * 
 * @author luis
 */
public class GetAccountHistoryParameters extends PosPinParameters {
    private Long    accountTypeId;
    private Integer currentPage = 0;

    public Long getAccountTypeId() {
        return accountTypeId;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setAccountTypeId(final Long accountTypeId) {
        this.accountTypeId = accountTypeId;
    }

    public void setCurrentPage(final Integer currentPage) {
        this.currentPage = currentPage;
    }

    @Override
    public String toString() {
        return "GetAccountHistoryParameters [accountTypeId=" + accountTypeId + ", currentPage=" + currentPage + ", " + super.toString() + "]";
    }
}
