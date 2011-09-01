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
/**
 * 
 */
package nl.strohalm.cyclos.services.accounts.pos;

import nl.strohalm.cyclos.dao.accounts.pos.MemberPosDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.pos.MemberPos;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.accounts.pos.exceptions.InvalidPosPinException;
import nl.strohalm.cyclos.services.accounts.pos.exceptions.PosPinBlockedException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.InvalidTriesTracer;
import nl.strohalm.cyclos.utils.TimePeriod;

import com.mysql.jdbc.StringUtils;

/**
 * 
 * @author rodrigo
 */
public class MemberPosServiceImpl implements MemberPosService {

    private FetchService       fetchService;
    private MemberPosDAO       memberPosDao;
    private PosService         posService;
    private InvalidTriesTracer pinTriesTracer = new InvalidTriesTracer();

    public MemberPos blockMemberPos(final MemberPos memberPos) {
        memberPos.setStatus(MemberPos.Status.BLOCKED);
        memberPosDao.update(memberPos);

        posService.generateLog(memberPos.getPos());

        return memberPos;
    }

    public MemberPos changePin(final MemberPos memberPos, final String pin) {
        boolean generateLog = false;
        if (memberPos.getPosPin() == null || StringUtils.isEmptyOrWhitespaceOnly(memberPos.getPosPin())) {
            memberPos.setStatus(MemberPos.Status.ACTIVE);
            generateLog = true;
        }
        memberPos.setPosPin(pin);
        memberPosDao.update(memberPos);
        if (generateLog) {
            posService.generateLog(memberPos.getPos());
        }
        return memberPos;
    }

    public void checkPin(final MemberPos memberPos, final String pin) throws InvalidPosPinException, PosPinBlockedException {
        // Check the pin
        final String posId = memberPos.getPos().getPosId();

        // Check if not already blocked
        if (pinTriesTracer.isBlocked(posId)) {
            throw new PosPinBlockedException(posId);
        } else {
            final String posPin = memberPos.getPosPin();
            if (posPin == null || !posPin.equalsIgnoreCase((pin))) {
                final Member member = fetchService.fetch(memberPos.getMember(), Element.Relationships.GROUP);
                final MemberGroupSettings memberSettings = member.getMemberGroup().getMemberSettings();
                final int maxTries = memberSettings.getMaxPinWrongTries();
                final TimePeriod blockTimeAfterMaxTries = memberSettings.getPinBlockTimeAfterMaxTries();
                final InvalidTriesTracer.TryStatus status = pinTriesTracer.trace(posId, maxTries, blockTimeAfterMaxTries);
                switch (status) {
                    case MAX_TRIES_REACHED:
                        // Block the POS.
                        memberPos.setStatus(MemberPos.Status.PIN_BLOCKED);
                        memberPosDao.update(memberPos);
                        posService.generateLog(memberPos.getPos());
                        throw new PosPinBlockedException(posId).noRollBack();
                    case NO_MORE_TRIES:
                        // MessageAspect will send a personal message to the member
                        throw new PosPinBlockedException(posId);
                    case TRIES_ALLOWED:
                        throw new InvalidPosPinException();
                }
            } else {
                pinTriesTracer.unblock(posId);
            }
        }
    }

    public MemberPosDAO getMemberPosDao() {
        return memberPosDao;
    }

    public MemberPos load(final Long id, final Relationship... fetch) {
        return memberPosDao.load(id, fetch);
    }

    public void save(final MemberPos memberPos) {
        memberPosDao.update(memberPos, true);

    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setMemberPosDao(final MemberPosDAO memberPosDao) {
        this.memberPosDao = memberPosDao;
    }

    public void setPosService(final PosService posService) {
        this.posService = posService;
    }

    public MemberPos unblockMemberPos(final MemberPos memberPos) {
        memberPos.setStatus(MemberPos.Status.ACTIVE);
        memberPosDao.update(memberPos);

        posService.generateLog(memberPos.getPos());
        return memberPos;
    }

    public MemberPos unblockPosPin(final MemberPos memberPos) {
        pinTriesTracer.unblock(memberPos.getPos().getPosId());

        // Unblock the POS
        unblockMemberPos(memberPos);

        return memberPos;
    }
}
