package ru.fizteh.fivt.students.asaitgalin.storable.tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.asaitgalin.storable.MultiFileTableProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MultiFileTableTest {
    private static final String DB_PATH = "/home/andrey/testdb";
    private static TableProvider provider;
    private static Table testTable;

    @Before
    public void setUp() throws Exception {
        List<Class<?>> columns = new ArrayList<>();
        columns.add(Integer.class);
        columns.add(String.class);
        provider = new MultiFileTableProvider(new File(DB_PATH));
        testTable = provider.createTable("table3", columns);
    }

    @After
    public void tearDown() throws Exception {
        provider.removeTable("table3");
    }

    @Test
    public void testGetName() throws Exception {
        Assert.assertEquals(testTable.getName(), "table3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWithNull() throws Exception {
        testTable.get(null);
    }

    @Test
    public void testGet() throws Exception {
        testTable.put("key", provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
        Storeable st = testTable.get("key");
        Assert.assertNotNull(st);
        Assert.assertEquals(st.getIntAt(0), Integer.valueOf(5));
        Assert.assertNull(testTable.get("not_existent_key"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutWithNullKey() throws Exception {
        testTable.put(null, provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutWithNullValue() throws Exception {
        testTable.put("key", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutWithSpaces() throws Exception {
        testTable.put("   ", provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
    }

    @Test
    public void testPut() throws Exception {
        Storeable st = provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>");
        Assert.assertNull(testTable.put("new_key", st));
        Assert.assertEquals(testTable.put("new_key", provider.deserialize(testTable,
                "<row><col>5</col><col>testval</col></row>")).toString(), st.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveWithNull() throws Exception {
        testTable.remove(null);
    }

    @Test
    public void testRemove() throws Exception {
        testTable.put("1", provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
        Assert.assertNull(testTable.remove("not_existing_key"));
        Assert.assertNull(testTable.remove("not_existing_key2"));
        Assert.assertEquals(testTable.remove("1").toString(), "5 value");
    }

    @Test
    public void testSize() throws Exception {
        for (int i = 0; i < 7; ++i) {
            testTable.put("sizeKey" + i, provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
        }
        Assert.assertEquals(testTable.size(), 7);
        testTable.remove("sizeKey5");
        Assert.assertEquals(testTable.size(), 6);
        testTable.rollback();
        Assert.assertEquals(testTable.size(), 0);
    }

    @Test
    public void testCommit() throws Exception {
        testTable.put("1", provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
        testTable.put("3", provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
        testTable.put("5", provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
        Assert.assertEquals(testTable.commit(), 3);
    }

    @Test
    public void testRollback() throws Exception {
        for (int i = 0; i < 7; ++i) {
            testTable.put("rollbackKey" + i, provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>"));
        }
        testTable.commit();
        testTable.put("rollbackKey4", provider.deserialize(testTable, "<row><col>5</col><col>value222</col></row>"));
        testTable.put("newKey", provider.deserialize(testTable, "<row><col>6</col><col>value</col></row>"));
        Assert.assertEquals(testTable.rollback(), 1);
        Assert.assertNull(testTable.get("newKey"));
        Assert.assertEquals(testTable.get("rollbackKey4").toString(),
                provider.deserialize(testTable, "<row><col>5</col><col>value</col></row>").toString());
    }

    @Test
    public void testGetColumnsCount() throws Exception {
        Assert.assertEquals(testTable.getColumnsCount(), 2);
    }


    @Test
    public void testGetColumnType() throws Exception {
        Assert.assertEquals(testTable.getColumnType(0).getSimpleName(), "Integer");
        Assert.assertEquals(testTable.getColumnType(1).getSimpleName(), "String");
    }

}
