package com.landawn.abacus.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.condition.Criteria;
import com.landawn.abacus.util.SQLBuilder.PSC;
import com.landawn.abacus.util.SQLBuilder.SP;
import com.landawn.abacus.util.entity.Account;
import com.landawn.abacus.util.entity.AccountContact;

class SQLBuilderTest {

    @Test
    public void test_distinct() {

        Criteria criteria = CF.criteria().distinctBy("firstName, lastName").where(CF.eq("id"));
        String sql = PSC.select(Account.class).preselect(criteria.preselect()).from(Account.class).append(criteria).sql();
        N.println(sql);

        String sql2 = PSC.selectFrom(Account.class).preselect(criteria.preselect()).append(criteria).sql();
        N.println(sql2);

        assertEquals(sql, sql2);
    }

    @Test
    public void test_selectFrom() {

        String sql = PSC.selectFrom(AccountContact.class, true).where(CF.eq("id")).sql();
        N.println(sql);

        sql = PSC.selectFrom(Account.class).where(CF.eq("id")).sql();
        N.println(sql);

        sql = PSC.selectFrom(Account.class, true).where(CF.eq("id")).sql();
        N.println(sql);

        sql = PSC.selectFrom(Account.class, "acc", true).where(CF.eq("id")).sql();
        N.println(sql);
    }

    @Test
    public void test_count() {

        String sql = PSC.count(Account.class).where(CF.eq("id")).sql();
        N.println(sql);
    }

    @Test
    public void test_expr() {

        String sql = PSC.count(Account.class).where(CF.eq("id")).append(CF.orderBy("firstName", "last_name")).sql();
        N.println(sql);

        sql = PSC.count(Account.class)
                .where(CF.eq("id").and(CF.expr("lengTh(firstName) > 0")).and(CF.expr("lengTh (last_Name) > 0")))
                .append(CF.orderBy("firstName", "last_name"))
                .sql();
        N.println(sql);
    }

    @Test
    public void test_multi_select() {
        SP ps = PSC.select(Account.class, "acc", null, AccountContact.class, "ac", "contact").from(Account.class).pair();

        N.println(ps.sql);
        ps = PSC.selectFrom(Account.class, "acc", null, AccountContact.class, "ac", "contact").pair();

        N.println(ps.sql);

        List<Selection> selections = Selection.multiSelectionBuilder().add(Account.class, "acc", null).add(AccountContact.class, "ac", "contact").build();

        SP ps2 = PSC.selectFrom(selections).pair();
        N.println(ps2.sql);

        assertEquals(ps, ps2);

        selections = Selection.multiSelectionBuilder()
                .add(Account.class, "acc", null, N.exclude(ClassUtil.getPropNameList(Account.class), "contact"))
                .add(AccountContact.class, "ac", "contact")
                .build();

        SP ps3 = PSC.selectFrom(selections).pair();
        N.println(ps3.sql);

        // assertEquals(ps, ps3);

        SP ps4 = PSC.selectFrom(Account.class, "acc", true).pair();
        N.println(ps4.sql);
    }

    @Test
    public void test_multi_select_02() {
        List<Selection> selections = Selection.multiSelectionBuilder()
                .add(Account.class, "acc", null, N.asList("firstName", "devices", "lastName"))
                .add(AccountContact.class, "ac", "contact")
                .build();

        SP ps3 = PSC.selectFrom(selections).pair();
        N.println(ps3.sql);

        String sql = "SELECT acc.first_name AS \"firstName\", device.id AS \"devices.id\", device.account_id AS \"devices.accountId\", device.name AS \"devices.name\", device.udid AS \"devices.udid\", device.platform AS \"devices.platform\", device.model AS \"devices.model\", device.manufacturer AS \"devices.manufacturer\", device.produce_time AS \"devices.produceTime\", device.category AS \"devices.category\", device.description AS \"devices.description\", device.status AS \"devices.status\", device.last_update_time AS \"devices.lastUpdateTime\", device.create_time AS \"devices.createTime\", acc.last_name AS \"lastName\", ac.id AS \"contact.id\", ac.account_id AS \"contact.accountId\", ac.mobile AS \"contact.mobile\", ac.telephone AS \"contact.telephone\", ac.email AS \"contact.email\", ac.address AS \"contact.address\", ac.address2 AS \"contact.address2\", ac.city AS \"contact.city\", ac.state AS \"contact.state\", ac.country AS \"contact.country\", ac.zip_code AS \"contact.zipCode\", ac.category AS \"contact.category\", ac.description AS \"contact.description\", ac.status AS \"contact.status\", ac.last_update_time AS \"contact.lastUpdateTime\", ac.create_time AS \"contact.createTime\" FROM account acc, device, account_contact ac";

        assertEquals(sql, ps3.sql);
    }

}
