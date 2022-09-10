package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.SQLBuilder.PSC;
import com.landawn.abacus.util.SQLBuilder.SP;
import com.landawn.abacus.util.entity.Account;
import com.landawn.abacus.util.entity.AccountContact;

class SQLBuilderTest {

    @Test
    public void test_multi_select() {
        SP ps = PSC.select(Account.class, "acc", null, AccountContact.class, "ac", "contact").from(Account.class).pair();

        N.println(ps.sql);
        ps = PSC.selectFrom(Account.class, "acc", null, AccountContact.class, "ac", "contact").pair();

        N.println(ps.sql);
    }

}
