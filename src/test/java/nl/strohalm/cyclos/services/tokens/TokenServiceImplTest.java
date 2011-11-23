/*
 *
 *    This file is part of Cyclos.
 *
 *    Cyclos is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    Cyclos is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Cyclos; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *
 */

package nl.strohalm.cyclos.services.tokens;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TokenServiceImplTest {

    @Test
    public void shouldGenerateUserName() {
        String tokenId = new TokenServiceImpl().generateTokenID();
        assertTrue(Pattern.matches("\\d{12}", tokenId));
    }


    @Test
    public void shouldPadTransactionId() {
        String trId = "1234";
        assertEquals("0001234", new TokenServiceImpl().padTransactionId(trId));
    }

    @Test
    public void shouldNotPadTransactionId() {
        String trId = "1234567";
        assertEquals("1234567", new TokenServiceImpl().padTransactionId(trId));
    }

}
