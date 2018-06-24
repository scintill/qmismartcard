/*
 * This file is part of qmismartcard.
 *
 * qmismartcard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * qmismartcard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with qmismartcard.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.scintill.qmi.smartcard;

import net.scintill.qmi.QmiException;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import java.util.ArrayList;
import java.util.List;

public class QmiSmartcardTerminals extends CardTerminals {

    static final QmiSmartcardTerminals sInstance = new QmiSmartcardTerminals();

    @Override
    public List<CardTerminal> list(State state) throws CardException {
        List<CardTerminal> l = new ArrayList<>(1);
        if (state == State.CARD_PRESENT || state == State.ALL) {
            // TODO handle multi-slot?
            l.add(new QmiSmartcardTerminal(1));
        }
        return l;
    }

    @Override
    public boolean waitForChange(long l) throws CardException {
        throw new CardException("not implemented");
    }

    static class QmiSmartcardTerminal extends CardTerminal {

        private int slot;
        private Card card;

        public QmiSmartcardTerminal(int slot) {
            this.slot = slot;
        }

        @Override
        public String getName() {
            return "QMI smartcard terminal "+slot;
        }

        @Override
        public Card connect(String protocol) throws CardException {
            if (card == null) {
                try {
                    card = new QmiSmartcardCard(slot, QmiSmartcardProvider.sQmiClient);
                } catch (QmiException e) {
                    throw new CardException("QMI card exception", e);
                }
            }

            return card;
        }

        @Override
        public boolean isCardPresent() throws CardException {
            return true;
        }

        @Override
        public boolean waitForCardPresent(long l) throws CardException {
            throw new RuntimeException("not implemented");
        }

        @Override
        public boolean waitForCardAbsent(long l) throws CardException {
            throw new RuntimeException("not implemented");
        }
    }

}
